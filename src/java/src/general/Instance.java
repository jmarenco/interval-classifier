package general;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;
import java.util.HashMap;

public class Instance
{
	private String _name;
	private int _classes;
	private ArrayList<Point> _points;
	private Map<Integer, Integer> _clusters;
	
	public Instance(String name, int classes)
	{
		_name = name;
		_classes = classes;
		_points = new ArrayList<Point>();
		_clusters = new HashMap<Integer, Integer>();
		
		for(int i=0; i<_classes; ++i)
			_clusters.put(i, 1);
	}
	
	public Instance(String name, int classes, int clustersInEachClass)
	{
		_name = name;
		_classes = classes;
		_points = new ArrayList<Point>();
		_clusters = new HashMap<Integer, Integer>();
		
		for(int i=0; i<_classes; ++i)
			_clusters.put(i, clustersInEachClass);
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setClusters(int classID, int clusters)
	{
		if( classID < 0 || classID >= _classes )
			throw new RuntimeException("Invalid class ID: " + classID);
		
		_clusters.put(classID, clusters);
	}
	
	public void add(Point p)
	{
		if( _points.size() > 0 && _points.get(0).getDimension() != p.getDimension() )
			throw new RuntimeException("Input points have different dimensions!");
		
		_points.add(p);
	}
	
	public int getPoints()
	{
		return _points.size();
	}
	
	public Point getPoint(int i)
	{
		if (i < 0 || i >= getPoints())
			throw new RuntimeException("Out of range point index: " + i);
		
		return _points.get(i);
	}
	
	public int getClusters(int classID)
	{
		return _clusters.get(classID);
	}
	
	public int getDimension()
	{
		return _points.size() == 0 ? 0 : _points.get(0).getDimension();
	}
	
	public double min(int coordinate)
	{
		return _points.stream().mapToDouble(p -> p.get(coordinate)).min().orElse(0);
	}
	
	public double max(int coordinate)
	{
		return _points.stream().mapToDouble(p -> p.get(coordinate)).max().orElse(0);
	}
	
	public double globalDiameter()
	{
		Point min = new Point(-1, -1, this.getDimension());
		Point max = new Point(-2, -1, this.getDimension());
		
		for(int t=0; t<this.getDimension(); ++t)
		{
			min.set(t, this.min(t));
			max.set(t, this.max(t));
		}
		
		return min.distance(max);
	}
	
	public void scale(double factor)
	{
		for(Point point: _points)
			point.scale(factor);
	}

	public void print()
	{
		for(int i=0; i<_points.size(); ++i)
			System.out.println("x[" + i + "] = " + _points.get(i));
		
		for(int i=0; i<_classes; ++i)
			System.out.println("clusters[" + i + "] = " + _clusters.get(i));
	}
	
	public Stream<Point> stream()
	{
		return _points.stream();
	}
}
