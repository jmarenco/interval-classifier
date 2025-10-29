package general;

import java.util.Random;

public class RandomInstance
{
	public static Instance generate(int dimension, int points, int clusters, double dispersion)
	{
		return generate(dimension, points, clusters, dispersion, 0);
	}
	
	public static Instance generate(int dimension, int points, int clusters, double dispersion, int seed)
	{
		Random random = new Random(seed);
		String name = "R(" + dimension + "," + points + "," + clusters + "," + dispersion + "," + seed + ")";
		Instance instance = new Instance(name, 2, clusters);
		
		Point[] centroids = new Point[2*clusters];
		
		for(int i=0; i<2*clusters; ++i)
			centroids[i] = randomPoint(random, 0, i / clusters, dimension, 2);
		
		for(int i=0; i<points; ++i)
		{
			int j = random.nextInt(2*clusters);
			
			Point point = randomPoint(random, i+1, j / clusters, dimension, dispersion);
			point.sum(centroids[j]);
			instance.add(point);
		}
		
		Point point = randomPoint(random, points, 1, dimension, dispersion);
		point.sum(centroids[0]);
		instance.add(point);

		return instance;
	}
	
	private static Point randomPoint(Random random, int id, int classID, int dimension, double range)
	{
		Point ret = new Point(id, classID, dimension);
		
		for(int i=0; i<dimension; ++i)
			ret.set(i, range * random.nextDouble() - range / 2);
		
		return ret;
	}
}
