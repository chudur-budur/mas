package sim.app.horde.features;

import sim.app.horde.*;
import sim.app.horde.targets.*;
import sim.app.horde.behaviors.*;

/**
 *
 * @author vittorio
 */
public class TargetDistance extends Feature
{
	private static final long serialVersionUID = 1;
	public TargetDistance(Target t)
	{
		this();
		targets[0] = t;
	}

	public TargetDistance()
	{
		super("DistanceTo");
		targets = new Target[1];
		targets[0] = new Me();  //  default
		targetNames = new String[] { "X" };
	}

	// this is actually distance squared.  We can change later on if we need to,
	// but the classifier probably should care not

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		SimAgent simagent = (SimAgent) agent;
		System.err.println("--- TargetDistance.getValue() : distance from " + targets[0].toString());
		return simagent.getLocation().distance(targets[0].getLocation(agent, parent, horde));
	}
}
