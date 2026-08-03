package branchandprice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import general.Cluster;
import general.Instance;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class PricingModel extends Pricing
{
	// Input data
	private Master _master;
	private Instance _instance;
	private int p; // Points
	private int d; // Dimension
	private int _class;

	// Solver
    private IloCplex cplex;
    
    // Objective function
    private IloObjective obj;

    // Branching constraints
    private Map<BranchingDecision, IloConstraint> branchingConstraints; //Constraints added to enforce branching decisions

    // Variables
	private IloNumVar[] z;
	private IloNumVar[] r;
	private IloNumVar[] l;
	private IloNumVar[][] wl;
	private IloNumVar[][] wr;
	
	// Parameters
	private static double _reducedCostThreshold = -0.0001; // Threshold for considering the objective as negative
	private static double _variableThreshold = 0.05; // Threshold for considering a variable as null
	private static boolean _stopWhenNegative = false;

	// Statistics
	private double _solvingTime = 0;
	private int _generatedColumns = 0;
	
	// Creates a new solver instance for a particular pricing problem
    public PricingModel(Master master, int classID)
    {
    	_master = master;
        _instance = master.getInstance();
        _class = classID;

        p = _instance.getPoints();
		d = _instance.getDimension();
        
        this.buildModel();
    }

    // Build the MIP model
    private void buildModel()
    {
        try
        {
        	createSolver();
    		createVariables();
    	    createOrderingConstraints();
    	    createBindingLRWConstraints();
    	    createBindingWZConstraints();
    	    createNonemptyConstraints();
    		createObjective();

            branchingConstraints = new HashMap<BranchingDecision, IloConstraint>();
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    private void createSolver() throws IloException
    {
        cplex = new IloCplex();
        cplex.setParam(IloCplex.Param.Advance, 0);
        cplex.setParam(IloCplex.Param.Threads, 1);
        cplex.setOut(null);

//        cplex.setParam(IloCplex.IntParam.AdvInd, 0);
//        cplex.setParam(IloCplex.IntParam.Threads, 1);
        
    	// The parameters cplex.uppercutoff = 0 and MIP integer solution limit = 1
    	// make Cplex stop as soon as it finds a solution with objective function < 0,
    	// but may degrade the performance since heuristics are not employed from nodes with
    	// relaxation objective >= 0, which are pruned
    	
        if( _stopWhenNegative == true )
        {
        	cplex.setParam(IloCplex.Param.MIP.Tolerances.UpperCutoff, _reducedCostThreshold);
        	cplex.setParam(IloCplex.Param.MIP.Limits.Solutions, 1);
        }
    }
    
	private void createVariables() throws IloException
	{
		z = new IloNumVar[p];
		r = new IloNumVar[d];
		l = new IloNumVar[d];
		wl = new IloNumVar[p][d];
		wr = new IloNumVar[p][d];
		
		for(int i=0; i<p; ++i)
	    	z[i] = cplex.boolVar("z" + i);

		for(int i=0; i<p; ++i)
		for(int t=0; t<d; ++t)
		{
		  	wl[i][t] = cplex.boolVar("wl" + i + "_" + t);
		  	wr[i][t] = cplex.boolVar("wr" + i + "_" + t);
		}

		for(int t=0; t<d; ++t)
	    	r[t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + t);

		for(int t=0; t<d; ++t)
	    	l[t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + t);
	}

	private void createOrderingConstraints() throws IloException
	{
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, l[t]);
			lhs = cplex.sum(lhs, cplex.prod(-1, r[t]));
			
			cplex.addLe(lhs, 0);
		}
	}
	
	private void createBindingLRWConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
		for(int t=0; t<d; ++t)
		{
			double M = _instance.max(t) - _instance.min(t);
			
			IloNumExpr lhs1 = cplex.linearIntExpr();
			lhs1 = cplex.sum(lhs1, l[t]);
			lhs1 = cplex.sum(lhs1, cplex.prod(-M, wl[i][t]));
			cplex.addLe(lhs1, _instance.getPoint(i).get(t));

			IloNumExpr lhs2 = cplex.linearIntExpr();
			lhs2 = cplex.sum(lhs2, r[t]);
			lhs2 = cplex.sum(lhs2, cplex.prod(M, wr[i][t]));
			cplex.addGe(lhs2, _instance.getPoint(i).get(t));

			IloNumExpr lhs3 = cplex.linearIntExpr();
			lhs3 = cplex.sum(lhs3, l[t]);
			lhs3 = cplex.sum(lhs3, cplex.prod(-M, wl[i][t]));
			cplex.addGe(lhs3, _instance.getPoint(i).get(t) - M);

			IloNumExpr lhs4 = cplex.linearIntExpr();
			lhs4 = cplex.sum(lhs4, r[t]);
			lhs4 = cplex.sum(lhs4, cplex.prod(M, wr[i][t]));
			cplex.addLe(lhs4, _instance.getPoint(i).get(t) + M);
		}
	}
	
	private void createBindingWZConstraints() throws IloException
	{
	    for(int i=0; i<p; ++i)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, z[i]);
			
			for(int t=0; t<d; ++t)
			{
				lhs = cplex.sum(lhs, wl[i][t]);
				lhs = cplex.sum(lhs, wr[i][t]);
			}
			
			cplex.addGe(lhs, 1);
		}

	    for(int i=0; i<p; ++i)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, z[i]);
			lhs = cplex.sum(lhs, wl[i][t]);
			lhs = cplex.sum(lhs, wr[i][t]);
			cplex.addLe(lhs, 1);
		}
	}

	private void createNonemptyConstraints() throws IloException
	{
		IloNumExpr lhs = cplex.linearIntExpr();

		for(int i=0; i<p; ++i) if( _class == _instance.getPoint(i).getClassID() )
			lhs = cplex.sum(lhs, z[i]);
			
		cplex.addGe(lhs, 1);
	}

	private void createObjective() throws IloException
	{
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int i=0; i<p; ++i) if( _class != _instance.getPoint(i).getClassID() )
			fobj = cplex.sum(fobj, z[i]);
		
		obj = cplex.addMinimize(fobj);
	}
	
	// Main method for solving the pricing problem
    public List<Cluster> generateColumns(double timeLimit)
    {
        List<Cluster> newPatterns = new ArrayList<>();
        try
        {
            cplex.setParam(IloCplex.Param.TimeLimit, timeLimit); //set time limit in seconds
//            cplex.exportModel("/home/javier/Escritorio/pricing" + _class + ".lp");

       		// Solve the problem and check the solution status
            double start = System.currentTimeMillis();
            boolean solved = cplex.solve();
            
            _solvingTime += (System.currentTimeMillis() - start) / 1000.0;

       		if( cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim ) // Aborted due to time limit
       			return newPatterns;

            if( _stopWhenNegative == false )
            {
           		if( cplex.getStatus() == IloCplex.Status.Infeasible ) // Pricing problem infeasible
           			throw new RuntimeException("Pricing problem infeasible");
           		
            	if( solved == false || cplex.getStatus() != IloCplex.Status.Optimal )
           			throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus() + ", obj: " + cplex.getObjValue());
            }
            else
            {
           		if( cplex.getStatus() != IloCplex.Status.Infeasible && cplex.getObjValue() > -_reducedCostThreshold )
           			throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus() + ", obj: " + cplex.getObjValue());
            }
            
//            System.out.println("Pricing problem solved - Obj = " + cplex.getObjValue());
//            
//            for(IloNumVar var: z) if( cplex.getValue(var) != 0 )
//            	System.out.println("  " + var.getName() + " = " + cplex.getValue(var));
//            
//            for(int i=0; i<_instance.getPoints(); ++i)
//            for(int t=0; t<_instance.getDimension(); ++t)
//            {
//            	if( cplex.getValue(f[i][t]) != 0 )
//            		System.out.println("  " + f[i][t].getName() + " = " + cplex.getValue(f[i][t]));
//
//            	if( cplex.getValue(l[i][t]) != 0 )
//            		System.out.println("  " + l[i][t].getName() + " = " + cplex.getValue(l[i][t]));
//            }

            // Generate new column if it has negative reduced cost
            if( cplex.getStatus() != IloCplex.Status.Infeasible )
            { 
            	int nsols = cplex.getSolnPoolNsolns();
            	int found = 0;
            	for (int j = 0; j < nsols; j++)
            	{
//            		System.out.println(" => Pricing obj: " + cplex.getObjValue(j) + " - Dual of class constr: " + _master.getDuals()[p + _class]);
					if (cplex.getObjValue(j) <= _reducedCostThreshold)
					{ 
		                Cluster cluster = new Cluster(_class);
		                double[] values = cplex.getValues(z, j);
		
		                for(int i=0; i<_instance.getPoints(); ++i)
		                {
		                  	if( Math.abs(values[i] - 1) < _variableThreshold && _instance.getPoint(i).getClassID() == _class )
		                   		cluster.add(_instance.getPoint(i));
		                }
		                    	
		                newPatterns.add(cluster);
//		                System.out.println(" -> " + cluster);
		                
		                found++;
		                if (found >= getMaxColsPerPricing())
		                	break;
					}
				}
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        _generatedColumns += newPatterns.size();
        return newPatterns;
    }

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public void updateObjective()
    {
        try
        {
            double[] dualCosts = _master.getDuals();
    		IloNumExpr fobj = cplex.linearNumExpr();

    		for(int i=0; i<p; ++i) if( _class != _instance.getPoint(i).getClassID() )
    			fobj = cplex.sum(fobj, z[i]);
    		
    		for(int i=0; i<p; ++i) if( _class == _instance.getPoint(i).getClassID() )
    			fobj = cplex.sum(fobj, cplex.prod(-dualCosts[i], z[i]));
    		
    		fobj = cplex.sum(fobj, dualCosts[p + _class]);
            obj.setExpr(fobj);
            
//            System.out.println("Pricing obj set: " + obj);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Close the pricing problem
    public void close()
    {
        cplex.end();
    }

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public void performBranching(BranchingDecision sc)
    {
    	if( sc instanceof BranchOnSide )
    		performBranchingOnSide((BranchOnSide)sc);
    	
    	if (sc instanceof BranchRyanFoster )
    		performBranchingRyanFoster((BranchRyanFoster)sc);
    }
    
    private void performBranchingOnSide(BranchOnSide sc)
    {
        try
        {
//        	System.out.println("Pricing: Perform branching " + sc);
        	
           	IloNumExpr lhs = cplex.linearIntExpr();
            IloConstraint branchingConstraint = null;

         	if( sc.appliesToMaxSide() )
           		lhs = cplex.sum(lhs, r[sc.getDimension()]);
           	else
           		lhs = cplex.sum(lhs, l[sc.getDimension()]);
            	
           	if( sc.isLowerBound() )
           	{
           		lhs = cplex.sum(lhs, cplex.prod(-sc.getThreshold() + _instance.min(sc.getDimension()), z[sc.getPoint()]));
           		branchingConstraint = cplex.addGe(lhs, _instance.min(sc.getDimension()));
           	}
           	else
           	{
           		lhs = cplex.sum(lhs, cplex.prod(-sc.getThreshold() + _instance.max(sc.getDimension()), z[sc.getPoint()]));
           		branchingConstraint = cplex.addLe(lhs, _instance.max(sc.getDimension()));
           	}
                
            branchingConstraints.put(sc, branchingConstraint);

//            System.out.println(">>> Branching constraint added: ");
//            System.out.println("    " + sc);
//            System.out.println("    " + branchingConstraint);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    private void performBranchingRyanFoster(BranchRyanFoster sc)
    {
        try
        {
//        	System.out.println("Pricing: Perform branching " + sc);
        	
    		IloNumExpr lhs = cplex.linearIntExpr();
    		IloConstraint branchingConstraint = null;
    		
        	if( sc.areTogether() == true )
        	{
        		lhs = cplex.sum(lhs, z[sc.getFirstIndex()]);
        		lhs = cplex.sum(lhs, cplex.prod(-1, z[sc.getSecondIndex()]));

           		branchingConstraint = cplex.addEq(lhs, 0);
        	}
        	else
        	{
        		lhs = cplex.sum(lhs, z[sc.getFirstIndex()]);
        		lhs = cplex.sum(lhs, z[sc.getSecondIndex()]);

           		branchingConstraint = cplex.addLe(lhs, 1);
        	}
        	
            branchingConstraints.put(sc, branchingConstraint);

//            System.out.println(">>> Branching constraint added: ");
//            System.out.println("    " + sc);
//            System.out.println("    " + branchingConstraint);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public void reverseBranching(BranchingDecision sc)
    {
        try
        {
//        	System.out.println("Pricing: Reverse branching " + sc);
            cplex.remove(branchingConstraints.get(sc));

//            System.out.println(">>> Branching decision reversed: ");
//            System.out.println("    " + sc);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    public static void stopWhenNegative(boolean value)
    {
    	_stopWhenNegative = value;
    }
    
    public static boolean stopWhenNegative()
    {
    	return _stopWhenNegative;
    }
    
    public double getSolvingTime()
    {
    	return _solvingTime;
    }
    
    public int getGeneratedColumns()
    {
    	return _generatedColumns;
    }
}
