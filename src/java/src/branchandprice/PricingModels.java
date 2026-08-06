package branchandprice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import general.Cluster;
import general.Instance;

public class PricingModels extends Pricing
{
	private Master _master;
	private Instance _instance;
	private ArrayList<PricingModel> _models;
	private double _masterBound;
	
	// Creates a new solver instance for a particular pricing problem
    public PricingModels(Master master)
    {
    	_master = master;
        _instance = master.getInstance();
    	_models = new ArrayList<PricingModel>();
    	
    	for(int j=0; j<_instance.getClasses(); ++j)
    		_models.add(new PricingModel(master, j));
    }
	
	// Main method for solving the pricing problem
    public List<Cluster> generateColumns(double timeLimit)
    {
        List<Cluster> ret = new ArrayList<>();
        
        for(PricingModel model: _models)
    		ret.addAll(model.generateColumns(timeLimit));
        
        while( ret.size() > this.getMaxColsPerPricing() )
        	ret.removeLast();
        
    	 _masterBound = _master.getObjValue() + IntStream.range(0, _instance.getClasses()).mapToDouble(c -> _models.get(c).getObjValue() * _instance.getClusters(c)).sum();

    	return ret;
    }

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public void updateObjective()
    {
        for(PricingModel model: _models)
    		model.updateObjective();
    }

    // Close the pricing problem
    public void close()
    {
        for(PricingModel model: _models)
    		model.close();
    }

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public void performBranching(BranchingDecision sc)
    {
        for(PricingModel model: _models)
        	model.performBranching(sc);
    }
    
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public void reverseBranching(BranchingDecision sc)
    {
        for(PricingModel model: _models)
        	model.reverseBranching(sc);
    }
    
    public double getSolvingTime()
    {
    	return _models.stream().mapToDouble(m -> m.getSolvingTime()).sum();
    }
    
    public int getGeneratedColumns()
    {
    	return _models.stream().mapToInt(m -> m.getGeneratedColumns()).sum();
    }

	public double getMasterBound()
	{
		return _masterBound;
	}
}
