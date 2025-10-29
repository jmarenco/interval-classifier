package general;

import java.util.ArrayList;

public class Solution
{
	private ArrayList<Cluster> _clusters;

	public Solution() 
	{
		_clusters = new ArrayList<>();
	}

	public Solution(ArrayList<Cluster> clusters)
	{
		_clusters = clusters;
	}

	public static Solution withAllPoints(Instance instance, int classID) 
	{
		Solution ret = new Solution();
		ret.add(Cluster.withAllPoints(instance, classID));
		return ret;
	}
	
	public static Solution withAllSingletons(Instance instance)
	{
		Solution ret = new Solution();
		
		for(int i=0; i<instance.getPoints(); ++i)
			ret.add(Cluster.singleton(instance.getPoint(i)));
		
		return ret;
	}

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}

	public void add(Cluster cluster) 
	{
		_clusters.add(cluster);
	}
	
	public void remove(Cluster cluster)
	{
		if( _clusters.contains(cluster) == false )
			throw new RuntimeException("Solution does not contain the cluster! " + cluster);
		
		_clusters.remove(cluster);
	}
	
	public int size()
	{
		return _clusters.size();
	}

	public Cluster get(int i) 
	{
		return _clusters.get(i);
	}

	public void addTo(Point point, int j) 
	{
		_clusters.get(j).add(point);
	}
	
	public boolean isFeasible()
	{
		return _clusters != null;
	}

	public double objective() 
	{
		return _clusters.stream().mapToDouble(c -> c.diagonal()).sum();
	}
	
	public double averageDiagonal()
	{
		return _clusters.stream().mapToDouble(c -> c.diagonal()).average().orElse(0);
	}
}
