package heuristic;

import java.util.ArrayList;
import java.util.Random;

import general.Instance;
import general.Point;

public class RandomInitialCentroids implements Heuristic.InitialCentroidsStrategy
{
	private Instance _instance;
	
	public RandomInitialCentroids(Instance instance)
	{
		_instance = instance;
	}

	public ArrayList<Point> initialCentroids()
	{
		Random random = new Random(0);
		ArrayList<Point> ret = new ArrayList<Point>();
		
		for(int i=0; i<_instance.getClasses(); ++i)
		for(int j=0; j<_instance.getClusters(i); ++j)
			ret.add(_instance.random(random, i));
		
		return ret;
	}

}
