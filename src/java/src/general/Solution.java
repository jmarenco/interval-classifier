package general;

import java.util.ArrayList;

public class Solution
{
	private Instance _instance;
	private ArrayList<Cluster> _clusters;

	public Solution(Instance instance) 
	{
		_instance = instance;
		_clusters = new ArrayList<>();
	}

	public Solution(Instance instance, ArrayList<Cluster> clusters)
	{
		_instance = instance;
		_clusters = clusters;
	}

	public static Solution withAllPoints(Instance instance, int classID) 
	{
		Solution ret = new Solution(instance);
		ret.add(Cluster.withAllPoints(instance, classID));
		return ret;
	}
	
	public static Solution withAllSingletons(Instance instance)
	{
		Solution ret = new Solution(instance);
		
		for(int i=0; i<instance.getPoints(); ++i)
			ret.add(Cluster.singleton(instance.getPoint(i)));
		
		return ret;
	}
	
	public static Solution withEmptyClusters(Instance instance, int clusters)
	{
		Solution ret = new Solution(instance);
		
		for(int i=0; i<clusters; ++i)
			ret.add(new Cluster());
		
		return ret;
	}

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
	
	public Cluster getCluster(int index)
	{
		return _clusters.get(index); 
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
	
	public boolean isOutlier(Point point)
	{
		for(Cluster cluster: _clusters)
		{
			if( cluster.getPoints().stream().anyMatch(p -> point.equalsExceptClass(p)) )
				return false;
		}
		
		return true;
	}
	
	public double totalSpan()
	{
		return _clusters.stream().mapToDouble(c -> c.span()).sum();
	}

	public double totalDiagonal() 
	{
		return _clusters.stream().mapToDouble(c -> c.diagonal()).sum();
	}
	
	public double averageDiagonal()
	{
		return _clusters.stream().mapToDouble(c -> c.diagonal()).average().orElse(0);
	}

	public int misclassified(Instance instance)
	{
		return _clusters.stream().mapToInt(c -> c.misclassified(instance).size()).sum();
	}

	public void print()
	{
		for(Cluster cluster: _clusters)
			System.out.println(cluster.toRectanglesString());
	}
}
