package heuristic;

import java.util.ArrayList;

import general.Instance;
import general.Point;
import general.Solution;

public class ClosestCentroidReconstruct implements Heuristic.ReconstructClustersStrategy
{
	private Instance _instance;
	
	public ClosestCentroidReconstruct(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution reconstructClusters(ArrayList<Point> centroids)
	{
		Solution ret = Solution.withEmptyClusters(_instance, centroids.size());
		
		for(Point point: _instance.asList())
			ret.getCluster(closestCentroid(point, centroids)).add(point);
		
		return ret;
	}
	
	private int closestCentroid(Point point, ArrayList<Point> centroids)
	{
		int bestIndex = -1;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for(int i=0; i<centroids.size(); ++i) if( centroids.get(i) != null && centroids.get(i).getClassID() == point.getClassID() )
		{
			double distance = point.distance(centroids.get(i));
			if( distance < bestDistance )
			{
				bestIndex = i;
				bestDistance = distance;
			}
		}
		
		return bestIndex;
	}

}
