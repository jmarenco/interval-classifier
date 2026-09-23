package general;

import java.io.IOException;
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
