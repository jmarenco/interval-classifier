package general;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FractionalSolution
{
	private Instance _instance;
	private Map<Cluster,Double> _clusters;
	private Map<Point,Double> _relabeled;
	private Map<Point,Double> _outlier;

	public FractionalSolution(Instance instance) 
	{
		_instance = instance;
		_clusters = new HashMap<Cluster,Double>();
		_relabeled = new HashMap<Point,Double>();
		_outlier = new HashMap<Point,Double>();
	}

	public Set<Cluster> getClusters()
	{
		return _clusters.keySet();
	}

	public void add(Cluster cluster, double value) 
	{
		_clusters.put(cluster, value);
	}
	
	public double getValue(Cluster cluster)
	{
		return _clusters.get(cluster);
	}
	
	public void setRelabeling(Point point, double value)
	{
		_relabeled.put(point, value);
	}
	
	public void setOutlier(Point point, double value)
	{
		_outlier.put(point, value);
	}
	
	public double getRelabeling(Point point)
	{
		return _relabeled.get(point);
	}
	
	public double getOutlier(Point point)
	{
		return _outlier.get(point);
	}
	
	public int size()
	{
		return _clusters.size();
	}

	public Instance getInstance()
	{
		return _instance;
	}
}
