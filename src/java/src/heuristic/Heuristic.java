package heuristic;

import java.util.ArrayList;

import frontend.EntryPoint;
import general.Instance;
import general.Point;
import general.Solution;

public class Heuristic
{
	public interface InitialCentroidsStrategy
	{
		ArrayList<Point> initialCentroids();
	}

	public interface ReconstructClustersStrategy
	{
		Solution reconstructClusters(ArrayList<Point> centroids);
	}
	
	public interface RecalculateCentroidsStrategy
	{
		boolean recalculateCentroids(Solution solution, ArrayList<Point> centroids);
	}
	
	public interface AdjustSolutionStrategy
	{
		Solution adjustSolution(Solution solution);
	}

	private Instance _instance;
	private Solution _solution;
	private ArrayList<Point> _centroids;
	
	private InitialCentroidsStrategy _initialCentroids;
	private ReconstructClustersStrategy _reconstructClusters;
	private RecalculateCentroidsStrategy _recalculateCentroids;
	private AdjustSolutionStrategy _adjustSolution;

	private long _start;
	private int _iterations;
	private int _rounds;

	private static int _maxIterations = 100;
	private static int _maxRounds = 100;
	private static int _maxTime = 3600;
	
	public Heuristic(Instance instance)
	{
		_instance = instance;
	}
	
	public void set(InitialCentroidsStrategy strategy)
	{
		_initialCentroids = strategy;
	}
	
	public void set(ReconstructClustersStrategy strategy)
	{
		_reconstructClusters = strategy;
	}
	
	public void set(RecalculateCentroidsStrategy strategy)
	{
		_recalculateCentroids = strategy;
	}
	
	public void set(AdjustSolutionStrategy strategy)
	{
		_adjustSolution = strategy;
	}
	
	public Solution run()
	{
		checkStrategies();

		_start = System.currentTimeMillis();
		_iterations = 0;
		_rounds = 1;
		_solution = singleRun();
		
		for(int i=1; i<_maxRounds && (System.currentTimeMillis() - _start) / 1000 <= _maxTime; ++i)
		{
			Solution actual = singleRun();
			if( actual.misclassified(_instance) < _solution.misclassified(_instance) )
				_solution = actual;
			
			_rounds++;
		}
		
		showSummary();
		return _solution;
	}
	
	public Solution singleRun()
	{
		int it = 0;
		_centroids = _initialCentroids.initialCentroids();
		
		Solution solution = _reconstructClusters.reconstructClusters(_centroids);
		while( _recalculateCentroids.recalculateCentroids(solution, _centroids) == true && it < _maxIterations )
		{
			solution = _reconstructClusters.reconstructClusters(_centroids);
			solution = _adjustSolution.adjustSolution(solution);

			_iterations++;
			it++;
		}
		
		return solution;
	}
	
	private void checkStrategies()
	{
		if( _initialCentroids == null )
			throw new RuntimeException("No InitialCentroidsStrategy specified!");

		if( _reconstructClusters == null )
			throw new RuntimeException("No ReconstructClustersStrategy specified!");

		if( _recalculateCentroids == null )
			throw new RuntimeException("No RecalculateCentroidsStrategy specified!");

		if( _adjustSolution == null )
			throw new RuntimeException("No AdjustSolutionStrategy specified!");
	}

	private void showSummary()
	{
		System.out.print("v" + EntryPoint.getVersion() + " | ");
		System.out.print(_instance.getName() + " | Heur | Feasible | ");
		System.out.print("Obj: " + _solution.misclassified(_instance) + " | ");
		System.out.print(String.format("%6.2f", (System.currentTimeMillis() - _start) / 1000.0) + " sec. | ");
		System.out.print(_rounds > 0 ? String.format("%.2f", _iterations / (double)_rounds) + " its | | " : " | | ");
		System.out.print(_rounds + " rounds | | | | |");
		System.out.print(EntryPoint.getArgs());
		System.out.println();
	}
	
	public ArrayList<Point> getCentroids()
	{
		return _centroids;
	}
	
	public static void setRounds(int rounds)
	{
		_maxRounds = rounds;
	}
	
	public static void setMaxTime(int timeLimit)
	{
		_maxTime = timeLimit;
	}
}
