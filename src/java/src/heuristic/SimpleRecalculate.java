package heuristic;

import java.util.ArrayList;

import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;

public class SimpleRecalculate implements Heuristic.RecalculateCentroidsStrategy
{
	private Instance _instance;
	
	private static enum Factor { Exponential, Linear };
	private static Factor _factor = Factor.Exponential;
	private static double _denominator = 10;
	private static double _maxRelativeDistance = 1;
	
	public SimpleRecalculate(Instance instance)
	{
		_instance = instance;
	}

	public boolean recalculateCentroids(Solution solution, ArrayList<Point> centroids)
	{
		boolean ret = false;
		for(int i=0; i<centroids.size(); ++i) if( centroids.get(i) != null || solution.getCluster(i).size() > 0 )
		{
			Cluster cluster = solution.getCluster(i);
			Point newCentroid = cluster.centroid();
			double radius = cluster.distanceToBorder(newCentroid);
			
			for(Point foreign: cluster.misclassified(_instance)) if( cluster.relativeDistanceToBorder(foreign) <= _maxRelativeDistance )
			{
				double dist = cluster.distanceToBorder(foreign);
				double factor = 0;
				
				if( _factor == Factor.Exponential )
					factor = Math.exp(-dist * dist / radius / radius) / _denominator;
				else if( _factor == Factor.Linear )
					factor = Math.abs(dist / _denominator);
				
				newCentroid.escapeFrom(foreign, factor);
			}
			
			if( centroids.get(i) == null || newCentroid == null || centroids.get(i).distance(newCentroid) > 0.001 )
			{
				centroids.set(i, newCentroid);
				ret = true;
			}
		}
		
		return ret;
	}
	
	public static void setFactor(String factor)
	{
		if( factor.toLowerCase().trim().equals("exp") )
			_factor = Factor.Exponential;
		else if( factor.toLowerCase().trim().equals("linear") )
			_factor = Factor.Linear;
		else
			throw new RuntimeException("Unkown factor: " + factor);
	}
	
	public static void setDenominator(double value)
	{
		_denominator = value;
	}
	
	public static void setMaxRelativeDistance(double value)
	{
		_maxRelativeDistance = value;
	}
}
