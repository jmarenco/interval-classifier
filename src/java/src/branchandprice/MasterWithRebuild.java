package branchandprice;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import general.Instance;
import general.Solution;
import heuristic.Heuristics;
import general.Cluster;

public class MasterWithRebuild implements Master
{
	private Instance _instance;
	private IloCplex _cplex;
    private IloObjective _obj; // Objective function
    private IloRange[] _binding; // Constraints
    private IloRange[] _numberOfClusters;
    private IloRange[] _allConstraints;
    private ArrayList<Column> _columns;
    private Map<IloNumVar, Column> _variables;
    private ArrayList<BranchingDecision> _branchings;

    private double _solvingTime = 0;
    private double _objValue;
    private double[] _previousDuals = null;
    private double[] _duals = null;
    
    private static boolean _initialSingletons = false;
    private static boolean _initialkmeans = false;
    private double _dualStabilizer = 0;

    public MasterWithRebuild(Instance instance)
    {
    	_instance = instance;
    	_columns = new ArrayList<Column>();
    	_branchings = new ArrayList<BranchingDecision>();
    	
    	this.buildModel();
    }

    // Builds the master model
    public void buildModel()
    {
//    	System.out.println("Rebuilding master");

    	try
        {
    		long start = System.currentTimeMillis();
        	_variables = new HashMap<IloNumVar, Column>();

        	// Create Cplex
            _cplex = new IloCplex();
            _cplex.setOut(null);

            // Define objective
            _obj = _cplex.addMinimize();

            // Define binding constraints
            _binding = new IloRange[_instance.getPoints()];
            for(int i=0; i<_instance.getPoints(); i++)
                _binding[i] = _cplex.addGe(_cplex.linearIntExpr(), 1, "bind" + i);
            
            // Define constraints on the number of clusters for each class
            _numberOfClusters = new IloRange[_instance.getClasses()];
            
            for(int j=0; j<_instance.getClasses(); ++j)
            	_numberOfClusters[j] = _cplex.addGe(_cplex.linearIntExpr(), -_instance.getClusters(j), "clus" + j);
            
            // Collects all constraints into a single array
            _allConstraints = new IloRange[_instance.getPoints() + _instance.getClasses()];

            for(int i=0; i<_instance.getPoints(); i++)
            	_allConstraints[i] = _binding[i];
            
            for(int j=0; j<_instance.getClasses(); ++j)
            	_allConstraints[_instance.getPoints() + j] = _numberOfClusters[j];
            
            // Adds all variables from existing columns. Must be eliminated is master is not rebuilt at each node
            int i = 0;
            for(Column column: _columns)
            {
            	if( _branchings.stream().allMatch(b -> b.isCompatible(column)) )
            		addColumnToModel(column, i++);
            }
            
            _solvingTime += (System.currentTimeMillis() - start) / 1000.0;
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Solve the master problem
    public boolean solve(long timeLimit)
    {
        try
        {
            // Set time limit
        	long start = System.currentTimeMillis();
            double timeRemaining = Math.max(1, (timeLimit-System.currentTimeMillis()) / 1000.0);
            
            _cplex.setParam(IloCplex.Param.TimeLimit, timeRemaining); // set time limit in seconds
//            _cplex.exportModel("/home/javier/Escritorio/master.lp");

            boolean solved = _cplex.solve();

            _solvingTime += (System.currentTimeMillis() - start) / 1000.0;
            _objValue = _cplex.getObjValue();
            _duals = null;

            // Solve the model
            if( solved == false || _cplex.getStatus() != IloCplex.Status.Optimal )
            {
                if( _cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim ) //Aborted due to time limit
                    return false;
                else
                    throw new RuntimeException("Master problem solve failed! Status: " + _cplex.getStatus());
            }
            else
            {
//                System.out.println("Master solved - obj: " + _cplex.getObjValue());

//                for(IloNumVar var: _variables.keySet()) if( _cplex.getValue(var) > 0 )
//            		System.out.println("  " + var.getName() + " = " + _cplex.getValue(var) + " " + _variables.get(var));
//                
//                for(IloNumVar var: _y)
//            		System.out.println("  " + var.getName() + " = " + _cplex.getValue(var));
                	
//            	for(IloRange range: _allConstraints)
//            		System.out.println(range + " = " + _cplex.getDual(range));

//              java.util.Iterator it = _cplex.getModel().iterator();
//              while( it.hasNext() )
//              {
//            	  Object obj = it.next();
//            	  System.out.println(obj.getClass().getName());
//            	  if( obj instanceof IloRange)
//            	  System.out.println(obj + " " + _cplex.getDual((IloRange)obj));
//              }

//            	for(IloNumVar var: _variables.keySet())
//            		System.out.println(var.getName() + " RC = " + _cplex.getReducedCost(var));
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return true;
    }

    // Extracts information from the master problem required by the pricing problems
    public double[] getDuals()
    {
    	if( _duals != null )
    		return _duals;
    	
        try
        {
            _duals = _cplex.getDuals(_allConstraints);
            
            if( _dualStabilizer > 0 && _previousDuals != null )
            {
            	for(int i=0; i<_duals.length && i<_previousDuals.length; ++i)
            		_duals[i] = (1 - _dualStabilizer) * _duals[i] + _dualStabilizer * _previousDuals[i];
            }

            _previousDuals = _duals; // This or the original duals?
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return _duals;
    }
    
    // Adds columns ensuring that a feasible solution exists
    public void addFeasibleColumns()
    {
		try
        {
			for(int j=0; j<_instance.getClasses(); ++j)
				addColumn(Column.artificial(_instance, j));
			
	        if( _initialSingletons == true )
	        {
	        	for(int i=0; i<_instance.getPoints(); ++i)
	    			addColumn(Column.singleton(_instance.getPoint(i)));
	        }
	        
	        if( _initialkmeans == true )
	        {
				Solution heuristic = Heuristics.basic(_instance).run();
				for(Cluster cluster: heuristic.getClusters())
					addColumn(Column.regular(cluster, _instance));
	        }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Adds a new column to the master problem
    public void addColumn(Cluster cluster)
    {
		if( _columns.stream().anyMatch(c -> !c.isArtificial() && c.getCluster().equals(cluster)) && _dualStabilizer == 0 )
			throw new RuntimeException("Duplicated column added to master! " + cluster);

		try
        {
			Column column = Column.regular(cluster, _instance);
        	addColumnToModel(column, _columns.size());
            _columns.add(column);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    private void addColumn(Column column) throws IloException
    {
    	addColumnToModel(column, _columns.size());
        _columns.add(column);
    }
    
    private void addColumnToModel(Column column, int index) throws IloException
    {
		// Register column with objective
//    	System.out.println("Registering column - cost: " + column.getCost() + " - " + column.getCluster());
        IloColumn iloColumn = _cplex.column(_obj, column.getCost());

        // Register column with the constraints
        for(int i=0; i<_instance.getPoints(); ++i) if( column.contains(_instance.getPoint(i)) )
            iloColumn = iloColumn.and(_cplex.column(_binding[i], 1));
        
        iloColumn = iloColumn.and(_cplex.column(_numberOfClusters[column.getCluster().getClassID()], -1));
        
        // Create the variable and store it
        IloNumVar var = _cplex.numVar(iloColumn, 0, Double.MAX_VALUE, "x" + index);
        _cplex.add(var);
        
        _variables.put(var, column);
    }

    // Gets the solution from the master problem. Returns all non-zero valued columns from the master problem
    public Map<Cluster, Double> getSolution()
    {
        Map<Cluster, Double> ret = new HashMap<Cluster, Double>();
        try
        {
        	for(IloNumVar var: _variables.keySet())
        	{
        		double value = _cplex.getValue(var);
        		if( value > 0.01 )
        			ret.put(_variables.get(var).getCluster(), value);
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
//        System.out.println("Returning solution with " + ret.size() + " clusters");
        return ret;
    }
    
    public boolean isIntegerSolution()
    {
    	if( this.isFeasible() == false )
    		return false;
    	
        try
        {
        	for(IloNumVar var: _variables.keySet())
        	{
        		if( _variables.get(var).isArtificial() )
        			continue;
        		
        		double value = _cplex.getValue(var);
        		if( Math.abs(value - (int)(value + 0.5)) > 0.01 )
        			return false;
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }

        return true;
    }
    
    public double getObjValue()
    {
    	return _objValue;
    }
    
    public boolean isOptimal()
    {
       	try
       	{
       		return _cplex.getStatus() == IloCplex.Status.Optimal;
       	}
       	catch(Exception e)
       	{
       		e.printStackTrace();
       	}
       	
       	return false;
    }
    
    public boolean isFeasible()
    {
    	try
    	{
    		if( _cplex.getStatus() != IloCplex.Status.Optimal)
    			return false;
    		
        	for(IloNumVar var: _variables.keySet())
        	{
        		if( _variables.get(var).isArtificial() && _cplex.getValue(var) > 0.01 )
        			return false;
            }
        	
        	return true;
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	return false;
    }

    // Closes the master problem
    public void close()
    {
        _cplex.end();
    }

    // Listen to branching decisions
    public void performBranching(BranchingDecision bd)
    {
//    	System.out.println("Master: Perform branching " + bd);
    	_branchings.add(bd);
    	
    	// TODO: Update the model instead of rebuilding it! This changes the construction in this.buildModel()
        this.close();
        this.buildModel();
    }

    // Undo branching decisions during backtracking in the Branch-and-Price tree
    public void reverseBranching(BranchingDecision bd)
    {
//    	System.out.println("Master: Reverse branching " + bd);
    	_branchings.remove(bd);
    }
    
    public Instance getInstance()
    {
    	return _instance;
    }
    
    public ArrayList<Column> getColumns()
    {
    	return _columns;
    }
    
    public double getSolvingTime()
    {
    	return _solvingTime;
    }
    
    public static void setInitialSingletons(boolean value)
    {
    	_initialSingletons = value;
    }
    
    public static void setInitialkMeans(boolean value)
    {
    	_initialkmeans = value;
    }
    
    public void setDualStabilizer(double value)
    {
    	if( value < 0 || value >= 1 )
    		throw new RuntimeException("Invalid factor for dual stabilization: " + value);

    	_dualStabilizer = value;
    }
    
    public double getDualStabilizer()
    {
    	return _dualStabilizer;
    }
}