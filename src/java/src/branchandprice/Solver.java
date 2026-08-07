package branchandprice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import frontend.EntryPoint;

import java.util.HashMap;

import general.Cluster;
import general.Instance;
import general.Solution;
import heuristic.Heuristics;

public class Solver
{
	private Instance _instance;
	private Master _master;
	private Pricing _pricing;
	private Pricing _exactPricing;
	private Pricing _rootPricing;
	private Branching _branching;
	
	private long _start;
	private double _ub;
	private double _pricingTime;
	private int _pricedColumns;
	private int _totalIterations;
	
	private ArrayList<Node> _nodes;
	private ArrayList<Node> _openNodes;
	private ArrayList<Cluster> _incumbent;
	private ArrayList<Solution> _solutions;
	private Map<Node, Double> _dualBound;
	
	public static enum Pricer { Model, Heuristic, None };
	public static enum Brancher { Side, RyanFoster };
	
	private static long _timeLimit = 3600;
	private static double _dualStabilizer = 0;
	private static boolean _onlyStabilizeRoot = true;
    private static boolean _initialSingletons = false;
    private static boolean _initialkmeans = false;
	private static boolean _verbose = true;
	private static boolean _pricingLog = false;
	private static boolean _summary = true;
	private static Pricer _pricer = Pricer.Model;
	private static Pricer _rootPricer = Pricer.None;
	private static Brancher _brancher = Brancher.RyanFoster;

	public Solver(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution solve()
	{
		// Initializes components
		_master = new MasterWithRebuild(_instance);
		_nodes = new ArrayList<Node>();
		_openNodes = new ArrayList<Node>();
		_dualBound = new HashMap<Node, Double>();
		_solutions = new ArrayList<Solution>();
		_start = System.currentTimeMillis();
		_totalIterations = 0;
		
		if( _brancher == Brancher.Side )
			_branching = new BranchingOnSide(_instance);
		else
			_branching = new BranchingRyanFoster(_instance);
		
		if( _pricer == Pricer.Model )
		{
			_pricing = new PricingModels(_master);
			_exactPricing = _pricing;
		}
		else if( _pricer == Pricer.Heuristic )
		{
			_pricing = new PricingHeuristicWithBranching(_master);
			_exactPricing = new PricingModels(_master);
		}
		
		if( _rootPricer == Pricer.Heuristic )
			_rootPricing = new PricingHeuristic(_master);

		// Initializes variables
		_ub = 1000;
		_incumbent = null;
		_master.setInitialColumns(initialColumns()); // Should be called after initializing _ub and _incumbent
		_master.addFeasibleColumns();
		_master.setDualStabilizer(_dualStabilizer);
		
		// Creates root node
		Node root = new Node(0);
		Node last = null;
		
		_nodes.add(root);
		_openNodes.add(root);
		
		// Main loop
		while( _openNodes.size() > 0 && getDualBound() + 0.001 < _ub && elapsedTime() < _timeLimit )
		{
			Node current = nextNode();
//			System.out.println("Solving node " + current.getId() + ", " + current.getBranchingDecision() + (current.getParent() != null ? " - Parent: Node " + current.getParent().getId() : ""));

//			Node aux = current;
//			while( aux != null )
//			{
//				System.out.println(aux.getBranchingDecision());
//				aux = aux.getParent();
//			}
			
			updateSubproblems(last, current);
			
			boolean incumbentUpdated = false;
			boolean newColumns = true;
			int addedColumns = 0;
			int iterations = 0;
			
			while( newColumns == true )
			{
//				_master.buildModel();
				_master.solve(remainingTime());
				newColumns = false;

//				System.out.println("debug counter = " + dbg);
//				System.out.println(" - Master Obj: " + _master.getObjValue());
				
				if( _master.isOptimal() == true )
				{
					List<Cluster> added = null;

					if( _nodes.size() == 1 && _rootPricing != null )
					{
						_rootPricing.updateObjective();
						added = _rootPricing.generateColumns(remainingTime());
					}
					
					if( (added == null || added.size() == 0) && _rootPricing != _pricing )
					{
						_pricing.updateObjective();
						added = _pricing.generateColumns(remainingTime());
					}

					if( (added == null || added.size() == 0) && _exactPricing != _pricing) // Activate exact pricing
					{
						_exactPricing.updateObjective();
						added = _exactPricing.generateColumns(remainingTime());
					}
					
					for(Cluster cluster: added)
						_master.addColumn(cluster);
	
					newColumns = added.size() > 0;
					addedColumns += added.size();
					iterations += 1;
					_totalIterations += 1;
					
					if( newColumns == false && _dualStabilizer > 0 && _master.getDualStabilizer() > 0 )
					{
						_master.setDualStabilizer(0);
						newColumns = true; // Let's try again if no columns were added with dual stabilization
					}
					
					if( _pricingLog == true )
						showPricing(iterations);
				}
			}
			
			if( _master.isIntegerSolution() == true )
			{
//				System.out.println("Integer solution!");

				if( _master.getObjValue() < _ub )
				{
					_incumbent = new ArrayList<Cluster>(_master.getSolution().keySet());
					_ub = _master.getObjValue();
					_solutions.add(new Solution(_incumbent));

					incumbentUpdated = true;
//					System.out.println("Incumbent updated! Obj = " + _master.getObjValue());
				}
			}
			else if( _master.isFeasible() == true && _master.getObjValue() < _ub )
			{
//				System.out.println("Fractional solution - Branching ...");
				for(BranchingDecision bd: _branching.getBranches(_master.getSolution()))
				{
					Node node = new Node(_nodes.size(), current, bd);

					_nodes.add(node);
					_openNodes.add(node);
					_dualBound.put(node, _master.getObjValue());
					
//					System.out.println(" - Branch created: " + bd);
				}
			}
//			else
//				System.out.println("Node fathomed!");
			
			if( remainingTime() > 0 )
				_openNodes.remove(current); // Otherwise, we are exiting due to the time limit, and the current node remains open
			
			last = current;
//			_master.restartDualStabilization();
			
			if( _onlyStabilizeRoot == false )
				_master.setDualStabilizer(_dualStabilizer);
			
			if( _verbose == true )
				showStatistics(current, iterations, addedColumns, incumbentUpdated);
		}

		if( _summary == true )
			showSummary();
		
		return new Solution(_incumbent);
	}
	
	// Initial columns
	private ArrayList<Cluster> initialColumns()
	{
		ArrayList<Cluster> ret = new ArrayList<Cluster>();
		
        if( _initialSingletons == true )
        {
        	for(int i=0; i<_instance.getPoints(); ++i)
    			ret.add(Cluster.singleton(_instance.getPoint(i)));
        }
        
        if( _initialkmeans == true )
        {
			Solution heuristic = Heuristics.basic(_instance).run();
			for(Cluster cluster: heuristic.getClusters())
				ret.add(cluster);
			
			_ub = heuristic.misclassified(_instance);
			_incumbent = heuristic.getClusters();
        }
        
        return ret;
	}
	
	// Node selection rule
	private Node nextNode()
	{
		return _openNodes.get(_openNodes.size() - 1);
	}
	
	// Updates branching rules in master and pricer
	private void updateSubproblems(Node last, Node current)
	{
		if( last == null )
			return;
		
		ArrayList<Node> fromLast = last.pathToRoot();
		ArrayList<Node> fromCurrent = current.pathToRoot();

//		System.out.println("From last (" + last.getId() + "):");
//		for(Node n: fromLast)
//			System.out.println(n.getId());
//		System.out.println("From current (" + current.getId() + "):");
//		for(Node n: fromCurrent)
//			System.out.println(n.getId());

		int i = 0;
		while( fromCurrent.contains(fromLast.get(i)) == false )
		{
			_master.reverseBranching(fromLast.get(i).getBranchingDecision());
			_pricing.reverseBranching(fromLast.get(i).getBranchingDecision());
			if (_pricing != _exactPricing)
				_exactPricing.reverseBranching(fromLast.get(i).getBranchingDecision());
			
			++i;
		}
		
//		System.out.println("Int: " + fromLast.get(i).getId());

		int j = fromCurrent.indexOf(fromLast.get(i)) - 1;
		while( j >= 0 )
		{
			_master.performBranching(fromCurrent.get(j).getBranchingDecision());
			_pricing.performBranching(fromCurrent.get(j).getBranchingDecision());
			if (_pricing != _exactPricing)
				_exactPricing.performBranching(fromCurrent.get(j).getBranchingDecision());

			--j;
		}
	}
	
	public double getDualBound()
	{
		return _openNodes.stream().mapToDouble(n -> _dualBound.containsKey(n) ? _dualBound.get(n) : 0.0).min().orElse(_ub);
	}
	
	public double elapsedTime()
	{
		return (System.currentTimeMillis() - _start) / 1000.0;
	}
	
	public long remainingTime()
	{
		return _timeLimit - (System.currentTimeMillis() - _start) / 1000;
	}
	
	private void showPricing(int iteration)
	{
		System.out.print("  It: " + String.format("%3d", iteration) + " | ");
		System.out.print(String.format("%5d", _pricing.getGeneratedColumns()) + " cols | ");
		System.out.print(String.format("%3d", _pricing.getGeneratedColumns() - _pricedColumns) + " new | ");
		System.out.print(String.format("%7.2f", _pricing.getSolvingTime()) + " total sec | ");
		System.out.print(String.format("%7.2f", _pricing.getSolvingTime() - _pricingTime) + " sec | ");
		System.out.print("Obj: " + String.format("%11.4f", _master.getObjValue()) + " | ");
		System.out.print("MLB: " + String.format("%11.4f", _pricing.getMasterBound()));
		System.out.print(_master.getDualStabilizer() > 0 ? " | DS: " + _master.getDualStabilizer() : "");
		System.out.println();
		
		_pricingTime = _pricing.getSolvingTime(); // These statistics are cumulative
		_pricedColumns = _pricing.getGeneratedColumns();
	}

	private void showStatistics(Node current, int iterations, int addedColumns, boolean incumbentUpdated)
	{
		double dualBound = getDualBound();
		double gap = _ub > 0 ? 100 * (_ub - dualBound) / _ub : 100;
		
		System.out.print("LB: " + String.format("%8.4f", dualBound));
		System.out.print(incumbentUpdated ? "*| " : " | ");
		System.out.print("UB: " + String.format("%9.4f", _ub));
		System.out.print(" (" + String.format("%5.2f", gap) + "%) | ");
		System.out.print("Nodes: " + _nodes.size() + " | ");
		System.out.print("Open: " + _openNodes.size() + " | ");
		System.out.print(String.format("%7.2f", elapsedTime()) + " sec | ");
		System.out.print("MLB: " + String.format("%7.4f", _pricing.getMasterBound()) + " | ");
		System.out.print("Cols: " + _master.getColumns().size());
		
		if( current != null )
		{
			System.out.print(" (" + addedColumns + " new, " + iterations + " its) | ");
			System.out.print("Cur: " + current.getId() + ", H: " + current.getHeight() + " - ");
			System.out.print(current.getBranchingDecision());
		}
		
		System.out.println();
	}
	
	private void showSummary()
	{
		double pricingTime = _pricing.getSolvingTime() + (_rootPricing != null && _rootPricing != _pricing? _rootPricing.getSolvingTime() : 0);
		
		if (_pricing != _exactPricing)
			pricingTime += _exactPricing.getSolvingTime();
		
		System.out.print("v" + EntryPoint.getVersion() + " | ");
		System.out.print(_instance.getName() + " | B&P | ");
		System.out.print(_openNodes.size() == 0 || relativeGap() == 0 ? "Optimal | " : "Feasible | ");
		System.out.print("Obj: " + String.format("%6.4f", _ub) + " | ");
		System.out.print(String.format("%6.2f", elapsedTime()) + " sec. | ");
		System.out.print(_nodes.size() + " nodes | ");
		System.out.print(String.format("%6.2f", relativeGap()) + " % | ");
		System.out.print(" | ");
		System.out.print(_master.getColumns().size() + " cols, " + _totalIterations + " its | ");
		System.out.print("M: " + String.format("%6.2f", _master.getSolvingTime()) + " sec. | ");
		System.out.print("P: " + String.format("%6.2f", pricingTime) + " sec. | ");
		System.out.print(_rootPricer == Pricer.Heuristic ? "HC: " + _rootPricing.getGeneratedColumns() + " | ": " | ");
		System.out.print(EntryPoint.getArgs());
		System.out.println();
	}	
	
	private double relativeGap()
	{
		double dualBound = getDualBound();
		
		if( _ub == 0 && dualBound == 0 )
			return 0;
		
		return _ub > 0 ? 100 * (_ub - dualBound) / _ub : 100;
	}

	public ArrayList<Cluster> getSolution()
	{
		return _incumbent;
	}
	
	public ArrayList<Solution> getFoundSolutions()
	{
		return _solutions;
	}
	
	public Master getMaster()
	{
		return _master;
	}
	
	public static void setMaxTime(long timeLimit)
	{
		_timeLimit = timeLimit;
	}

	public static void setDualStabilizer(double alpha)
	{
		_dualStabilizer = alpha;
	}
	
	public static void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public static void setPricingLog(boolean verbose)
	{
		_pricingLog = verbose;
	}
    
    public static void setInitialSingletons(boolean value)
    {
    	_initialSingletons = value;
    }
    
    public static void setInitialkMeans(boolean value)
    {
    	_initialkmeans = value;
    }
	
	public static void showSummary(boolean summary)
	{
		_summary = summary;
	}
	
	public static void onlyStabilizeRoot(boolean value)
	{
		_onlyStabilizeRoot = value;
	}
	
	public static void setPricer(Pricer pricer)
	{
		_pricer = pricer;
	}
	
	public static void setRootPricer(boolean heuristic)
	{
		_rootPricer = heuristic ? Pricer.Heuristic : Pricer.None;
	}
	
	public static void setBrancher(Brancher brancher)
	{
		_brancher = brancher;
	}

	public static Brancher getBrancher()
	{
		return _brancher;
	}
}
