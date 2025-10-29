package frontend;

import general.Instance;
import general.RandomInstance;
import general.Solution;
import heuristic.Heuristic;

public class EntryPoint
{
	public static void main(String[] args)
	{
		Instance instance = RandomInstance.generate(2, 50, 3, 0.4, 11);
		Heuristic heuristic = new Heuristic(instance);
		Solution solution = heuristic.run();
		
		new Viewer(instance, solution);
	}
}
