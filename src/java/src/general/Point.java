package general;

import java.util.Arrays;
import java.util.Objects;

public class Point
{
	private int _id;
	private int _class;
	private double[] _values;
	
	public Point(int id, int classID, int dimension)
	{
		_id = id;
		_class = classID;
		_values = new double[dimension];
	}
	
	public static Point fromVector(int id, int classID, double... values)
	{
		Point ret = new Point(id, classID, values.length);
		
		for(int i=0; i<values.length; ++i)
			ret.set(i, values[i]);
		
		return ret;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getClassID()
	{
		return _class;
	}
	
	public int getDimension()
	{
		return _values.length;
	}
	
	public void set(int i, double value)
	{
		if( i < 0 || i >= _values.length )
			throw new RuntimeException("Out of range Point coordinate: " + i);
			
		_values[i] = value;
	}
	
	public double get(int i)
	{
		if( i < 0 || i >= _values.length )
			throw new RuntimeException("Out of range Point coordinate: " + i);
		
		return _values[i];
	}
	
	public void sum(Point other)
	{
		if( this.getDimension() != other.getDimension() )
			throw new RuntimeException("Summing points of different dimension!");
		
		for(int i=0; i<getDimension(); ++i)
			_values[i] += other.get(i);
	}
	
	public void subtract(Point other)
	{
		if( this.getDimension() != other.getDimension() )
			throw new RuntimeException("Summing points of different dimension!");
		
		for(int i=0; i<getDimension(); ++i)
			_values[i] -= other.get(i);
	}

	public void divide(double factor)
	{
		if( factor == 0 )
			throw new RuntimeException("Dividing a point by zero!");
		
		for(int i=0; i<getDimension(); ++i)
			_values[i] /= factor;
	}
	
	public void escapeFrom(Point other, double distance)
	{
		Point difference = this.clone();
		difference.subtract(other);
		difference.scale(distance);
		this.sum(difference);
	}
	
	public double distance(Point other)
	{
		if( this.getDimension() != other.getDimension() )
			throw new RuntimeException("Taking distance between points of different dimensions!");

		double sum = 0;
		for(int i=0; i<getDimension(); ++i)
			sum += (this.get(i) - other.get(i)) * (this.get(i) - other.get(i));
		
		return Math.sqrt(sum);
	}
	
	public Point clone()
	{
		return Point.fromVector(_id, _class, _values);
	}
	
	public double length()
	{
		double sum = 0;
		
		for(int i=0; i<_values.length; ++i)
			sum += _values[i] * _values[i];
		
		return Math.sqrt(sum);
	}
	
	public void scale(double factor)
	{
		for(int i=0; i<getDimension(); ++i)
			this.set(i, factor * this.get(i));
	}
	
	public void normalize()
	{
		double len = this.length();
		if( len >= 0.0001 )
			scale(1 / len);
	}
	
	public void integrize()
	{
		for(int i=0; i<getDimension(); ++i)
			this.set(i, (int)this.get(i));
	}
	
    public double dot(Point other)
    {
    	if( this.getDimension() != other.getDimension() )
    		throw new RuntimeException("Dot product of different dimensions!");

        double ret = 0;
        for(int i=0; i<this.getDimension(); ++i)
        	ret += this.get(i) * other.get(i);
        
        return ret;
    }

    public double angle(Point other)
    {
		double dotProduct = this.dot(other);
        double magnitudeThis = this.length();
        double magnitudeOther = other.length();

        if( magnitudeThis <= 0.001 || magnitudeOther <= 0.001 )
        	return 0;

        double cosTheta = dotProduct / (magnitudeThis * magnitudeOther);
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));

        return Math.acos(cosTheta);
    }

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(_values);
		result = prime * result + Objects.hash(_class, _id);
		return result;
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
		Point other = (Point) obj;
		return _class == other._class && _id == other._id && Arrays.equals(_values, other._values);
	}

	@Override
	public String toString()
	{
		return Arrays.toString(_values) + " - c" + _class;
	}
}
