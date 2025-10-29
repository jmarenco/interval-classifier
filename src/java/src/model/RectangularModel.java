package model;

import general.Cluster;
import general.Instance;
import general.Solution;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

public class RectangularModel
{
	private Instance _instance;
	private Solution _solution;
	private IloCplex cplex;
	
	private int p;
	private int n;
	private int d;
	private int c;
	private int[] classOf;
	
	private int _maxTime = 3600;
	private boolean _verbose = true;
	private boolean _summary = true;

	private IloNumVar[][] z;
	private IloNumVar[][] r;
	private IloNumVar[][] l;
	private IloNumVar[][][] wl;
	private IloNumVar[][][] wr;

	public RectangularModel(Instance instance)
	{
		_instance = instance;
		
		p = _instance.getPoints();
		n = _instance.getTotalClusters();
		d = _instance.getDimension();
		c = _instance.getClasses();
		
		classOf = new int[n];
		
		for(int i=0, k=0; i<c; ++i)
		for(int j=0; j<_instance.getClusters(i); ++j)
			classOf[k++] = i;
	}

	public Solution run()
	{
		Solution ret = null;
		
		try
		{
			ret = solve();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public Solution solve() throws IloException
	{
		createSolver();
		createVariables();
	    createOrderingConstraints();
	    createBindingLRWConstraints();
	    createBindingWZConstraints();
	    createClusteringConstraints();
		createObjective();
		
		solveModel();
    	obtainSolution();
	    closeSolver();
	    
	    return _solution;
	}

	private void createSolver() throws IloException
	{
		if (cplex != null)
			cplex.end();
		
		cplex = new IloCplex();
		
		if( _verbose == false )
		{
			cplex.setOut(null);
			cplex.setWarning(null);
		}
	}

	private void createVariables() throws IloException
	{
		z = new IloNumVar[p][n];
		r = new IloNumVar[n][d];
		l = new IloNumVar[n][d];
		wl = new IloNumVar[p][n][d];
		wr = new IloNumVar[p][n][d];
		
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
	    	z[i][j] = cplex.boolVar("z" + i + "_" + j);

		for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
		  	wl[i][j][t] = cplex.boolVar("wl" + i + "_" + j + "_" + t);
		  	wr[i][j][t] = cplex.boolVar("wr" + i + "_" + j + "_" + t);
		}

		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	r[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + j + "_" + t);

	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	l[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + j + "_" + t);
	}

	private void createOrderingConstraints() throws IloException
	{
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, l[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(-1, r[j][t]));
			
			cplex.addLe(lhs, 0);
		}
	}
	
	private void createBindingLRWConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			double M = _instance.max(t) - _instance.min(t);
			
			IloNumExpr lhs1 = cplex.linearIntExpr();
			lhs1 = cplex.sum(lhs1, l[j][t]);
			lhs1 = cplex.sum(lhs1, cplex.prod(-M, wl[i][j][t]));
			cplex.addLe(lhs1, _instance.getPoint(i).get(t));

			IloNumExpr lhs2 = cplex.linearIntExpr();
			lhs2 = cplex.sum(lhs2, r[j][t]);
			lhs2 = cplex.sum(lhs2, cplex.prod(M, wr[i][j][t]));
			cplex.addGe(lhs2, _instance.getPoint(i).get(t));

			IloNumExpr lhs3 = cplex.linearIntExpr();
			lhs3 = cplex.sum(lhs3, l[j][t]);
			lhs3 = cplex.sum(lhs3, cplex.prod(-M, wl[i][j][t]));
			cplex.addGe(lhs3, _instance.getPoint(i).get(t) - M);

			IloNumExpr lhs4 = cplex.linearIntExpr();
			lhs4 = cplex.sum(lhs4, r[j][t]);
			lhs4 = cplex.sum(lhs4, cplex.prod(M, wr[i][j][t]));
			cplex.addLe(lhs4, _instance.getPoint(i).get(t) + M);
		}
	}
	
	private void createBindingWZConstraints() throws IloException
	{
	    for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, z[i][j]);
			
			for(int t=0; t<d; ++t)
			{
				lhs = cplex.sum(lhs, wl[i][j][t]);
				lhs = cplex.sum(lhs, wr[i][j][t]);
			}
			
			cplex.addGe(lhs, 1);
		}

	    for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, z[i][j]);
			lhs = cplex.sum(lhs, wl[i][j][t]);
			lhs = cplex.sum(lhs, wr[i][j][t]);
			cplex.addLe(lhs, 1);
		}
	}

	private void createClusteringConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			
		    for(int j=0; j<n; ++j) if( classOf[j] == _instance.getPoint(i).getClassID() )
		    	lhs = cplex.sum(lhs, z[i][j]);
		    
		    cplex.addEq(lhs, 1, "clus" + i);
	    }
	}
	
	private void createObjective() throws IloException
	{
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j) if( classOf[j] != _instance.getPoint(i).getClassID() )
			fobj = cplex.sum(fobj, z[i][j]);
		
		cplex.addMinimize(fobj);
	}

	private void solveModel() throws IloException
	{
		long start = System.currentTimeMillis();
		
 		cplex.setParam(IntParam.TimeLimit, _maxTime);
		cplex.solve();
		
		if( _summary == false )
		{
			System.out.println("Status: " + cplex.getStatus());
			System.out.println("Objective: " + String.format("%6.4f", cplex.getObjValue()));
			System.out.println("Time: " + String.format("%6.2f", (System.currentTimeMillis() - start) / 1000.0));
			System.out.println("Nodes: " + cplex.getNnodes());
			System.out.println("Gap: " + ((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) : "  ****"));
			System.out.println("Cuts: " + cplex.getNcuts(IloCplex.CutType.User));
		}
		else
		{
			System.out.print(_instance.getName() + " | Std | ");
			System.out.print(cplex.getStatus() + " | ");
			System.out.print("Obj: " + String.format("%6.4f", cplex.getObjValue()) + " | ");
			System.out.print(String.format("%6.2f", (System.currentTimeMillis() - start) / 1000.0) + " sec. | ");
			System.out.print(cplex.getNnodes() + " nodes | ");
			System.out.print(((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) + " % | " : "  **** | "));
			System.out.print(cplex.getNcuts(IloCplex.CutType.User) + " cuts | ");
			System.out.println();
		}
	}

	private void obtainSolution() throws IloException, UnknownObjectException
	{
		_solution = new Solution();
		
    	if( cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible )
		{
	    	for(int j=0; j<n; ++j)
	    	{
	    		Cluster cluster = new Cluster();
	    		
				for(int i=0; i<p; ++i) if( cplex.getValue(z[i][j]) > 0.9 && classOf[j] == _instance.getPoint(i).getClassID() )
					cluster.add(_instance.getPoint(i));
				
				_solution.add(cluster);
		    }
		}
    }
	
	public void closeSolver()
	{
		cplex.end();
	}
	
	public void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public void showSummary(boolean summary)
	{
		_summary = summary;
	}
	
	public void setMaxTime(int value)
	{
		_maxTime = value;
	}
}
