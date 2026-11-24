package general;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;

import heuristic.Heuristic;
import heuristic.Heuristics;

public class InstanceReader
{
	public static Instance constructFromFile(String fileName, int clustersInEachClass)
	{
		Instance temporal = readSingleClassFromColumnFile(fileName, 2 * clustersInEachClass);
		Heuristic.setShowSummary(false);
		Solution kmeans = Heuristics.basic(temporal).run();
		Heuristic.setShowSummary(true);
		
		Instance ret = new Instance(fileName, 2, clustersInEachClass);

		for(int i=0; i<kmeans.getClusters().size(); ++i)
		for(Point existing: kmeans.getCluster(i).getPoints())
		{
			Point point = existing.clone();
			point.setClassID(i%2);
			ret.add(point);
		}

		return ret;
    }

	private static Instance readSingleClassFromColumnFile(String fileName, int clusters)
	{
		Instance temporal = new Instance(fileName, 1, clusters);
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) 
		{
            String line;
            double maxValue = 0;
            while ((line = br.readLine()) != null) 
            {
            	String[] values = line.split("\\s+");

            	int dimension = 0;
                for (String value : values) if( value.trim().length() > 0 )
                	++dimension;
                
                Point p = new Point(temporal.getPoints(), 0, dimension);
                
                int j = 0;
                for (String value : values) if( value.trim().length() > 0 )
                {
                    value = value.trim(); // Remove leading and trailing whitespace
                    if (isNumeric(value))
                    {
                    	p.set(j++, Double.parseDouble(value));
                    	maxValue = Math.max(maxValue, Math.abs(Double.parseDouble(value)));
                    }
                }
                
                temporal.add(p);
            }

            if( maxValue > 0 )
    			temporal.scale(1 / maxValue);
        } 
		catch (IOException e) 
		{
            e.printStackTrace();
            return null;
        }

		return temporal;
    }

	public static Instance constructFromFileWithGroundTruth(String fileName)
	{
		Instance ret = new Instance(fileName, 2, 0);
		Set<Integer> class0labels = new HashSet<Integer>();
		Set<Integer> class1labels = new HashSet<Integer>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) 
		{
            String line;
            double maxValue = 0;
            while ((line = br.readLine()) != null) 
            {
            	String[] values = line.split("\\s+");

            	int dimension = -1;
                for (String value : values) if( value.trim().length() > 0 )
                	++dimension;
                
                Point p = new Point(ret.getPoints(), 0, dimension);
                
                int j = 0;
                for (String value : values) if( value.trim().length() > 0 )
                {
                    value = value.trim(); // Remove leading and trailing whitespace
                    if( isNumeric(value) )
                    {
                    	if( j < dimension )
                    	{
                    		p.set(j++, Double.parseDouble(value));
                    		maxValue = Math.max(maxValue, Math.abs(Double.parseDouble(value)));
                    	}
                    	else
                    	{
                    		int groundTruth = Integer.parseInt(value);
                    		int pointClass = groundTruth % 2;
                    		p.setClassID(pointClass);
                    		
                    		if( pointClass == 0 )
                    			class0labels.add(groundTruth);
                    		else
                    			class1labels.add(groundTruth);
                    	}
                    }
                }
                
                ret.add(p);
            }
            
            if( maxValue > 0 )
    			ret.scale(1 / maxValue);
            
            ret.setClusters(0, class0labels.size());
            ret.setClusters(1, class1labels.size());
        } 
		catch (IOException e) 
		{
            e.printStackTrace();
            return null;
        }

		return ret;
    }

    public static boolean isNumeric(String str)
    {
        if (str == null || str.isEmpty())
        {
            return false;
        }
        try
        {
            Double.parseDouble(str);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
