package sim.app.horde.features;
import sim.app.horde.*;
import sim.app.horde.behaviors.*;

/** Returns true if the agent has become stuck on an obstacle (he tried to move forward and was unable to do so. */

public abstract class CollectiveFeature extends CategoricalFeature
{
	private static final long serialVersionUID = 1;
	public static final int SOMEONE_SEES_ATTACK = 0;
	public static final int SOMEONE_IS_DONE = 1;
	public static final int EVERYONE_IS_DONE = 2;
	public static final int SOMEONE_SEES_PROF_IN_N_SECS = 3;

	public static final String[] atts = new String[] { "SomeoneSeesAttack", "SomeoneIsDone", "EveryoneIsDone", "SomeoneSeesProfInNSecs" };

	int feature;

	protected CollectiveFeature(int featureNumber)  // assumes boolean
	{
		this(atts[featureNumber], new String[] { "false", "true" }, featureNumber);
	}

	protected CollectiveFeature(String attribute, String[] values, int featureNumber)
	{
		super(attribute, values);
		feature = featureNumber;
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		double[] features = ((SimHorde)horde).features;
		synchronized(features)
		{
			return (int)(features[feature]);
		}
	}
}