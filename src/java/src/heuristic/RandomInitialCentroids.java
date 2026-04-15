package heuristic;

import java.util.ArrayList;
import java.util.Random;

import general.Instance;
import general.Point;

public class RandomInitialCentroids implements Heuristic.InitialCentroidsStrategy
{
	private Instance _instance;
	private Random _random;
	private static int _seed = 0;
	
	public RandomInitialCentroids(Instance instance)
	{
		_instance = instance;
		_random = new Random(_seed);
	}

	public ArrayList<Point> initialCentroids()
	{
		ArrayList<Point> ret = new ArrayList<Point>();
		
		for(int i=0; i<_instance.getClasses(); ++i)
		for(int j=0; j<_instance.getClusters(i); ++j)
			ret.add(_instance.random(_random, i));
		
		return ret;
	}
	
	public static void setSeed(int seed)
	{
		_seed = seed;
	}
}
