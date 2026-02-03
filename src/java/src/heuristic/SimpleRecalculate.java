package heuristic;

import java.util.ArrayList;

import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;

public class SimpleRecalculate implements Heuristic.RecalculateCentroidsStrategy
{
	private Instance _instance;
	
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
			
			for(Point foreign: cluster.misclassified(_instance))
			{
				double dist = cluster.distanceToBorder(foreign);
				double factor = Math.exp(-dist * dist / radius / radius) / 10;
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

}
