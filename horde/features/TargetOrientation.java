package sim.app.horde.features;

import sim.app.horde.*;
import sim.app.horde.targets.*;
import sim.app.horde.behaviors.*;
/**
 *
 * @author vittorio
 */
public class TargetOrientation extends ToroidalFeature
{
	private static final long serialVersionUID = 1;
	public TargetOrientation(Target t)
	{
		this();
		targets[0] = t;
	}

	public TargetOrientation()
	{
		super("OrientationOf");
		targets = new Target[1];
		targets[0] = new Me();  //  default
		targetNames = new String[] { "X" };
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		System.err.println("--- TargetDistance.getValue() : orientation of "
		                   + targets[0].toString());
		return targets[0].getOrientation(agent, parent, horde);
	}
}
