package heuristic;

import java.util.ArrayList;
import java.util.Random;

import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;

public class Heuristic
{
	private Instance _instance;
	private Solution _solution;
	private ArrayList<Point> _centroids;

	private long _start;
	private int _iterations;
	
	public Heuristic(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution run()
	{
		_solution = null;
		_centroids = initialCentroids();
		_start = System.currentTimeMillis();
		_iterations = 1;
		
		reconstructClusters();
		while( recalculateCentroids() == true )
		{
			reconstructClusters();
			_iterations++;
		}
		
		showSummary();
		return _solution;
	}
	
	private ArrayList<Point> initialCentroids()
	{
		Random random = new Random(0);
		ArrayList<Point> ret = new ArrayList<Point>();
		
		for(int i=0; i<_instance.getClasses(); ++i)
		for(int j=0; j<_instance.getClusters(i); ++j)
			ret.add(_instance.random(random, i));
		
		return ret;
	}
	
	private void reconstructClusters()
	{
		_solution = Solution.withEmptyClusters(_instance, _centroids.size());
		
		for(Point point: _instance.asList())
			_solution.getCluster(closestCentroid(point)).add(point);
	}
	
	private int closestCentroid(Point point)
	{
		int bestIndex = -1;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for(int i=0; i<_centroids.size(); ++i) if( _centroids.get(i) != null && _centroids.get(i).getClassID() == point.getClassID() )
		{
			double distance = point.distance(_centroids.get(i));
			if( distance < bestDistance )
			{
				bestIndex = i;
				bestDistance = distance;
			}
		}
		
		return bestIndex;
	}
	
	private boolean recalculateCentroids()
	{
		boolean ret = false;
		for(int i=0; i<_centroids.size(); ++i) if( _centroids.get(i) != null || _solution.getCluster(i).size() > 0 )
		{
			Cluster cluster = _solution.getCluster(i);
			Point newCentroid = cluster.centroid();
			double radius = cluster.distanceToBorder(newCentroid);
			
			for(Point foreign: cluster.misclassified(_instance))
			{
				double dist = cluster.distanceToBorder(foreign);
				double factor = Math.exp(-dist * dist / radius / radius) / 10;
				newCentroid.escapeFrom(foreign, factor);
			}
			
			if( _centroids.get(i) == null || newCentroid == null || _centroids.get(i).distance(newCentroid) > 0.001 )
			{
				_centroids.set(i, newCentroid);
				ret = true;
			}
		}
		
		return ret;
	}
	
	private void showSummary()
	{
		System.out.print(_instance.getName() + " | Heur | Feasible | ");
		System.out.print("Obj: " + _solution.misclassified(_instance) + " | ");
		System.out.print(String.format("%6.2f", (System.currentTimeMillis() - _start) / 1000.0) + " sec. | ");
		System.out.print(_iterations + " its | | | ");
		System.out.println();
	}
	
	public ArrayList<Point> getCentroids()
	{
		return _centroids;
	}
}
