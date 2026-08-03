package frontend;

import branchandprice.Solver;
import general.Instance;
import general.RandomInstance;
import general.Solution;
import heuristic.Heuristic;
import heuristic.Heuristics;
import model.RectangularModel;

public class EntryPoint
{
	private static String _version = "0.09";
	private static ArgMap _argmap;
	
	public static void main(String[] args)
	{
		_argmap = new ArgMap(args);
		
		if( _argmap.containsArg("-help") )
		{
			showParameters();
			return;
		}
		
		int dim = _argmap.intArg("-d", 2);
		int points = _argmap.intArg("-n", 50);
		int clusters = _argmap.intArg("-c", 3);
		double disp = _argmap.doubleArg("-disp", 0.5);
		int seed = _argmap.intArg("-seed", 5);
		
		Instance instance = RandomInstance.generate(dim, points, clusters, disp, seed);
		Solution solution = null;

		Heuristic.setRounds(_argmap.intArg("-rounds", 10));
		Heuristic.setMaxTime(_argmap.intArg("-maxtime", 3600));
		RectangularModel.setMaxTime(_argmap.intArg("-maxtime", 3600));
		RectangularModel.setVerbose(_argmap.containsArg("-verbose"));
		Solver.setMaxTime(_argmap.intArg("-maxtime", 3600));
		Solver.setVerbose(_argmap.containsArg("-verbose"));
		Solver.setBrancher(_argmap.stringArg("-branch", "rf").equals("rf") ? Solver.Brancher.RyanFoster : Solver.Brancher.Side);
		
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
		System.out.println("-seed [i]    Random seed");
		System.out.println("-rounds [i]  Heuristic rounds");
		System.out.println("-maxtime [i] Time limit in seconds");
		System.out.println("-branch [s]  Branching strategy [rf|side]");
		System.out.println("-verbose     Show log");
		System.out.println("-show        Show solutions");
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
