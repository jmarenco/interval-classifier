package frontend;

import branchandprice.PricingModel;
import branchandprice.Solver;
import general.Instance;
import general.RandomInstance;
import general.Solution;
import heuristic.Heuristic;
import heuristic.Heuristics;
import heuristic.RandomInitialCentroids;
import heuristic.SimpleRecalculate;
import model.RectangularModel;

public class EntryPoint
{
	private static String _version = "0.13";
	private static ArgMap _argmap;
	
	public static void main(String[] args)
	{
		_argmap = new ArgMap(args);
		
		if( _argmap.containsArg("-help") )
		{
			showParameters();
			return;
		}
		
		processParameters();

		Instance instance = getInstance();
		Solution solution = null;

		if( _argmap.stringArg("-m", "xxx").equals("model") )
		{
			RectangularModel model = new RectangularModel(instance);
			solution = model.run();
		}

		if( _argmap.stringArg("-m", "xxx").equals("kmeans") )
		{
			Heuristic heuristic = Heuristics.basic(instance);
			solution = heuristic.run();
		}

		if( _argmap.stringArg("-m", "xxx").equals("bap") )
		{
			Solver solver = new Solver(instance);
			solution = solver.solve();
		}
		
		if( _argmap.containsArg("-show"))
		{
			new Viewer(instance, "Instance");
			new Viewer(instance, solution, "Solution");
		}
	}
	
	private static void showParameters()
	{
		System.out.println("Interval Classifier v" + _version);
		System.out.println();
		System.out.println("-m [s]       Method [model|kmeans|bap]");
		System.out.println("-d [i]       Dimension");
		System.out.println("-n [i]       Number of points");
		System.out.println("-c [i]       Number of clusters in each class");
		System.out.println("-disp [f]    Dispersion in each cluster");
		System.out.println("-seed [i]    Random seed for construction of instance");
		System.out.println("-maxtime [i] Time limit in seconds");
		System.out.println("-branch [s]  Branching strategy [rf|side]");
		System.out.println("-price [s]   Pricing strategy [zw|zwb]");
		System.out.println("-is          Initial singleton columns");
		System.out.println("-ik          Initial columns from k-means heuristic");
		System.out.println("-ds(r) [f]   Dual stabilization (only in root) with factor f");
		System.out.println("-rounds [i]  Heuristic rounds");
		System.out.println("-hs [i]      Random seed for heuristic");
		System.out.println("-hf [s]      Factor for heuristic [exp|linear]");
		System.out.println("-hden [f]    Denominator for heuristic");
		System.out.println("-mrd [f]     Maximum relative distance for heuristic");
		System.out.println("-verbose     Show log");
		System.out.println("-show        Show solutions");
	}
	
	private static void processParameters()
	{
		Heuristic.setRounds(_argmap.intArg("-rounds", 10));
		Heuristic.setMaxTime(_argmap.intArg("-maxtime", 3600));
    	Heuristic.setShowSummary(_argmap.stringArg("-m", "xxx").equals("kmeans"));
    	
    	SimpleRecalculate.setFactor(_argmap.stringArg("-hf", "exp"));
    	SimpleRecalculate.setDenominator(_argmap.doubleArg("-hden", 10));
    	SimpleRecalculate.setMaxRelativeDistance(_argmap.doubleArg("-mrd", 1));
    	RandomInitialCentroids.setSeed(_argmap.intArg("-hs", 0));
    	
		RectangularModel.setMaxTime(_argmap.intArg("-maxtime", 3600));
		RectangularModel.setVerbose(_argmap.containsArg("-verbose"));
		
		Solver.setMaxTime(_argmap.intArg("-maxtime", 3600));
		Solver.setInitialSingletons(_argmap.containsArg("-is"));
		Solver.setInitialkMeans(_argmap.containsArg("-ik"));
		Solver.setDualStabilizer(Math.max(_argmap.doubleArg("-ds", 0), _argmap.doubleArg("-dsr", 0)));
		Solver.onlyStabilizeRoot(_argmap.containsArg("-dsr"));
		Solver.setVerbose(_argmap.containsArg("-verbose"));
		Solver.setPricingLog(_argmap.containsArg("-pricinglog"));
		Solver.setBrancher(_argmap.stringArg("-branch", "rf").equals("rf") ? Solver.Brancher.RyanFoster : Solver.Brancher.Side);
		
		if( Solver.getBrancher() == Solver.Brancher.Side )
			PricingModel.setBorderConstraints(true);
		
		if( _argmap.stringArg("-price", "zw").equals("zwb") )
			PricingModel.setBorderConstraints(true);
	}

	private static Instance getInstance()
	{
		int dim = _argmap.intArg("-d", 2);
		int points = _argmap.intArg("-n", 50);
		int clusters = _argmap.intArg("-c", 3);
		double disp = _argmap.doubleArg("-disp", 0.5);
		int seed = _argmap.intArg("-seed", 5);
		
		Instance instance = RandomInstance.generate(dim, points, clusters, disp, seed);
		return instance;
	}
	
	public static String getVersion()
	{
		return _version;
	}
	
	public static ArgMap getArgs()
	{
		return _argmap;
	}
}
