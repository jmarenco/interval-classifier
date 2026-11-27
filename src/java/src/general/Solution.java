package general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Solution
{
	private Instance _instance;
	private Map<Cluster,Double> _clusters;
	private Map<Point,Integer> _relabel;

	public Solution(Instance instance) 
	{
		_instance = instance;
		_clusters = new HashMap<Cluster,Double>();
		_relabel = new HashMap<Point,Integer>();
	}

	public Solution(Instance instance, ArrayList<Cluster> clusters)
	{
		_instance = instance;
		_clusters = new HashMap<Cluster,Double>();
		_relabel = new HashMap<Point,Integer>();
		
		for(Cluster c: clusters)
			_clusters.put(c, 1.0);
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

	public Set<Cluster> getClusters()
	{
		return _clusters.keySet();
	}

	public void add(Cluster cluster) 
	{
		_clusters.put(cluster, 1.0);
	}

	public void add(Cluster cluster, double value) 
	{
		_clusters.put(cluster, value);
	}
	
	public double getValue(Cluster cluster)
	{
		return _clusters.get(cluster);
	}
	
	public void remove(Cluster cluster)
	{
		if( _clusters.containsKey(cluster) == false )
			throw new RuntimeException("Solution does not contain the cluster! " + cluster);
		
		_clusters.remove(cluster);
	}
	
	public void relabel(Point point, int classID)
	{
		if( classID < 0 || classID >= _instance.getClasses() )
			throw new RuntimeException("Invalid class ID: " + classID);
		
		_relabel.put(point, classID);
	}
	
	public int getLabel(Point point)
	{
		return _relabel.containsKey(point) ? _relabel.get(point) : point.getClassID();
	}
	
	public int size()
	{
		return _clusters.size();
	}
	
	public boolean isOutlier(Point point)
	{
		for(Cluster cluster: _clusters.keySet())
		{
			if( cluster.getPoints().stream().anyMatch(p -> point.equalsExceptClass(p)) )
				return false;
		}
		
		return true;
	}
	
	public double totalSpan()
	{
		return _clusters.keySet().stream().mapToDouble(c -> c.span()).sum();
	}

	public double totalDiagonal() 
	{
		return _clusters.keySet().stream().mapToDouble(c -> c.diagonal()).sum();
	}
	
	public double averageDiagonal()
	{
		return _clusters.keySet().stream().mapToDouble(c -> c.diagonal()).average().orElse(0);
	}

	public int misclassified()
	{
		return _clusters.keySet().stream().mapToInt(c -> c.misclassified(this).size()).sum();
	}

	public void print()
	{
		for(Cluster cluster: _clusters.keySet())
			System.out.println(cluster.toRectanglesString());
	}

	public Instance getInstance()
	{
		return _instance;
	}

	public boolean relabeled(Point point)
	{
		return _relabel.containsKey(point);
	}
}
