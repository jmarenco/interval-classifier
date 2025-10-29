package frontend;

import general.Instance;
import general.RandomInstance;
import general.Solution;
import heuristic.Heuristic;
import model.RectangularModel;

public class EntryPoint
{
	public static void main(String[] args)
	{
//		Instance instance = RandomInstance.generate(2, 50, 3, 0.5, 5);
		Instance instance = RandomInstance.generate(2, 10, 2, 0.5, 5);

		Heuristic heuristic = new Heuristic(instance);
		Solution solution1 = heuristic.run();

		RectangularModel model = new RectangularModel(instance);
		Solution solution2 = model.run();
		
		new Viewer(instance, solution1, "Heuristic solution");
		new Viewer(instance, solution2, "Model solution");
	}
}
