package heuristic;

import general.Instance;

public class Heuristics
{
	public static Heuristic basic(Instance instance)
	{
		Heuristic ret = new Heuristic(instance);
		ret.set(new RandomInitialCentroids(instance));
		ret.set(new ClosestCentroidReconstruct(instance));
		ret.set(new SimpleRecalculate(instance));
		ret.set(new VoidAdjust());
		
		return ret;
	}

}
