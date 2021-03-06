package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.targets.*;
import sim.util.*;

public class Attach extends Behavior
{
	private static final long serialVersionUID = 1L;

	public static final double MINIMUM_ATTACH_DISTANCE_SQUARED = 4.0;

	public boolean shouldAddDefaultExample()
	{
		return false;
	}

	public Attach()
	{
		name = "Attach";
		targets = new Target[1];
		targets[0] = new Me();
		targetNames = new String[1];
		targetNames[0] = "To";
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		// it has to be close enough
		Targetable targetable = targets[0].getTargetable(agent, parent, horde);
		Double2D targetLoc = targetable.getTargetLocation(agent, horde);
		SimAgent simagent = (SimAgent)agent;
		if (simagent.getLocation().distanceSq(targetLoc) < MINIMUM_ATTACH_DISTANCE_SQUARED)
		{
			simagent.setManipulated(targetable);
		}
	}
}
