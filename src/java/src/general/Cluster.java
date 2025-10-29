package general;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Cluster
{
	private Set<Point> _points;
	private double[] _min;
	private double[] _max;
	private int _class = -1;
	
	public Cluster()
	{
		_points = new HashSet<Point>();
	}

	public static Cluster fromArray(Instance instance, int... indexes)
	{
		Cluster ret = new Cluster();
		
		for(Integer i: indexes)
			ret.add(instance.getPoint(i));
		
		return ret;
	}
	
	public static Cluster fromSet(Instance instance, Set<Integer> indexes)
	{
		Cluster ret = new Cluster();
		
		for(Integer i: indexes)
			ret.add(instance.getPoint(i));
		
		return ret;
	}
	
	public static Cluster withAllPoints(Instance instance, int classID)
	{
		Cluster ret = new Cluster();
		
		for(Point point: instance.stream(classID).collect(Collectors.toList()))
			ret.add(point);
		
		return ret;
	}
	
	public static Cluster singleton(Point point)
	{
		Cluster ret = new Cluster();
		ret.add(point);
		return ret;
	}

	public void add(Point point)
	{
		if( _points.size() == 0 )
		{
			_min = new double[point.getDimension()];
			_max = new double[point.getDimension()];
			_class = point.getClassID();
			
			for(int t=0; t<point.getDimension(); ++t)
				_min[t] = _max[t] = point.get(t); 
		}
		else if( this.getClassID() != point.getClassID() )
		{
			throw new RuntimeException("Point of class " + point.getClassID() + " added to class " + this.getClassID() + " cluster!");
		}
		else
		{
			for(int t=0; t<point.getDimension(); ++t)
			{
				_min[t] = Math.min(_min[t], point.get(t));
				_max[t] = Math.max(_max[t], point.get(t));
			}
		}

		_points.add(point);
	}
	
	public void remove(Point point)
	{
		_points.remove(point);
		
		if( _points.size() == 0 )
		{
			_min = null;
			_max = null;
			_class = -1;
		}
	}
	
	public Set<Point> asSet()
	{
		return _points;
	}
	
	public ArrayList<Point> asArrayList()
	{
		ArrayList<Point> ret = new ArrayList<Point>(_points.size());
		
		for(Point point: _points)
			ret.add(point);
		
		return ret;
	}
	
	public int size()
	{
		return _points.size();
	}
	
	public boolean contains(Point point)
	{
		return _points.contains(point);
	}
	
	public void setMin(int coordinate, double value)
	{
		_min[coordinate] = value;
	}
	
	public void setMax(int coordinate, double value)
	{
		_max[coordinate] = value;
	}

	public double getMin(int coordinate)
	{
		return _min[coordinate];
	}
	
	public double getMax(int coordinate)
	{
		return _max[coordinate];
	}
	
	public double max(int dimension)
	{
		return _points.stream().mapToDouble(p -> p.get(dimension)).max().orElse(0);
	}
	
	public double min(int dimension)
	{
		return _points.stream().mapToDouble(p -> p.get(dimension)).min().orElse(0);
	}
	
	public Point centroid()
	{
		Point ret = null;
		
		for(Point point: _points)
		{
			if (ret == null)
				ret = point.clone();
			else
				ret.sum(point);
		}
		
		if (_points.size() > 0)
			ret.divide(_points.size());
		
		return ret;
	}
	
	public double totalDistanceToCentroid()
	{
		if( _points.size() == 0 )
			return 0;
		
		Point c = this.centroid();
		return _points.stream().mapToDouble(p -> p.distance(c)).sum();
	}

	public double span()
	{
		if( _points.size() == 0 )
			return 0;
		
		int dimension = _points.iterator().next().getDimension();

		double ret = 0;
		for(int t=0; t<dimension; ++t)
			ret += span(t);
		
		return ret;
	}
	
	public double span(int dimension)
	{
		return max(dimension) - min(dimension);
	}
	
	public double diagonal()
	{
		if( _points.size() == 0 )
			return 0;
		
		return lowerCorner().distance(upperCorner());
	}
	
	public Point diagonalDirection()
	{
		if( _points.size() == 0 )
			return null;
		
		Point ret = upperCorner();
		ret.subtract(lowerCorner());
		ret.normalize();
		
		return ret;
	}
	
	public Point lowerCorner()
	{
		Point ret = null;
		for(Point p: _points)
		{
			if( ret == null )
				ret = p.clone();
			
			for(int t=0; t<p.getDimension(); ++t)
				ret.set(t, Math.min(ret.get(t), p.get(t)));
		}
		
		return ret;
	}
	
	public Point upperCorner()
	{
		Point ret = null;
		for(Point p: _points)
		{
			if( ret == null )
				ret = p.clone();
			
			for(int t=0; t<p.getDimension(); ++t)
				ret.set(t, Math.max(ret.get(t), p.get(t)));
		}
		
		return ret;
	}
	
	public Set<Point> getPoints()
	{
		return _points;
	}
	
	public int getClassID()
	{
		return _class;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(_points);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cluster other = (Cluster) obj;
		return Objects.equals(_points, other._points);
	}

	@Override public String toString()
	{
		String ret = "";
		for(Point point: _points)
			ret += (ret.length() > 0 ? ", " : "") + point.getId();
		
		return "{" + ret + "}";
	}

	public boolean covers(Point p)
	{
		boolean covered = true;
		for (int t = 0; t < p.getDimension(); ++t)
		{
			if (this.min(t) > p.get(t) || this.max(t) < p.get(t)) // Out of the box!
			{
				covered = false;
				break;
			}
		}
		
		return covered;
	}

	public Cluster union(Cluster that)
	{
		Cluster ret = new Cluster();
		
		for(Point point: this.getPoints())
			ret.add(point);

		for(Point point: that.getPoints())
			ret.add(point);
		
		return ret;
	}
	
	public List<Point> misclassified(Instance instance)
	{
		return instance.stream().filter(p -> p.getClassID() != this.getClassID() && this.covers(p)).collect(Collectors.toList());
	}
	
	public double distanceToBorder(Point point)
	{
		if( point == null )
			return 0;
		
		double ret = Double.POSITIVE_INFINITY;
		for(int t=0; t<point.getDimension(); ++t)
		{
			ret = Math.min(ret, Math.abs(point.get(t) - _min[t]));
			ret = Math.min(ret, Math.abs(point.get(t) - _max[t]));
		}
		
		return ret;
	}
}
