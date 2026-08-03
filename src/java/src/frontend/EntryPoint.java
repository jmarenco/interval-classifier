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
	private static String _version = "0.08";
	
	public static void main(String[] args)
	{
		ArgMap argmap = new ArgMap(args);
		
		if( argmap.containsArg("-help") )
		{
			showParameters();
			return;
		}
		
		int dim = argmap.intArg("-d", 2);
		int points = argmap.intArg("-n", 50);
		int clusters = argmap.intArg("-c", 3);
		double disp = argmap.doubleArg("-disp", 0.5);
		int seed = argmap.intArg("-seed", 5);
		
		Instance instance = RandomInstance.generate(dim, points, clusters, disp, seed);
		Solution solution = null;

		Heuristic.setRounds(argmap.intArg("-rounds", 10));
		Heuristic.setMaxTime(argmap.intArg("-maxtime", 3600));
		RectangularModel.setMaxTime(argmap.intArg("-maxtime", 3600));
		
		if( argmap.stringArg("-m", "xxx").equals("model") )
		{
			RectangularModel model = new RectangularModel(instance);
			solution = model.run();
		}

		if( argmap.stringArg("-m", "xxx").equals("kmeans") )
		{
			Heuristic heuristic = Heuristics.basic(instance);
			solution = heuristic.run();
		}

		if( argmap.stringArg("-m", "xxx").equals("bap") )
		{
			Solver solver = new Solver(instance);
			solution = solver.solve();
		}
		
		if( argmap.containsArg("-show"))
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
		System.out.println("-show        Show solutions");
		
	}
}
