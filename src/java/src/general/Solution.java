package general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Solution
{
	private Instance _instance;
	private ArrayList<Cluster> _clusters;
	private Map<Point,Integer> _relabels;

	public Solution(Instance instance)
	{
		_instance = instance;
		_clusters = new ArrayList<>();
		_relabels = new HashMap<Point,Integer>();
	}

	public Solution(Instance instance, ArrayList<Cluster> clusters)
	{
		_instance = instance;
		_clusters = clusters;
		_relabels = new HashMap<Point,Integer>();
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
		return _clusters.stream().mapToInt(c -> c.misclassified(this).size()).sum();
	}

	public void print()
	{
		for(Cluster cluster: _clusters)
			System.out.println(cluster.toRectanglesString());
	}
	
	public Instance getInstance()
	{
		return _instance;
	}
	
	public void relabel(Point point, int classID)
	{
		_relabels.put(point, classID);
	}

	public int getLabel(Point point)
	{
		return _relabels.containsKey(point) ? _relabels.get(point) : point.getClassID();
	}

	public boolean relabeled(Point point)
	{
		return getLabel(point) != point.getClassID();
	}
}
