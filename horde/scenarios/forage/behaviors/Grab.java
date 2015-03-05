package sim.app.horde.scenarios.forage.behaviors;
import sim.app.horde.scenarios.forage.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.*;
import sim.util.*;
import sim.app.horde.objects.*;

public class Grab extends Behavior
{
	public static final int SLOP = 14;
	private static final long serialVersionUID = 1;
	public Grab()
	{
		name = "Grab";    // good default
		setKeyStroke('g');
	}

	public boolean shouldAddDefaultExample()
	{
		return false;
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);

		SimAgent simagent = (SimAgent) agent;

		if (simagent.manipulated != null)  // we've already got something
			return;

		Double2D loc = simagent.getLocation();
		Targetable thing = (Targetable)this.getTarget(simagent,horde);
		if (loc.distance(thing.getTargetLocation(simagent,horde)) > SLOP) return; //too far
		simagent.manipulated = (Body) thing;
		simagent.manipulated.incrementAttachment();

		int status = simagent.manipulated.getTargetStatus(simagent, horde);
		status++;
		simagent.manipulated.setTargetStatus(simagent, horde, status);

		simagent.biggestBox = ((Box)simagent.manipulated).getMinimumAttachments();

		simagent.setStatus(Forager.ATTACHED_STATUS);
	}

	public Object getTarget(Agent agent, Horde horde)
	{
		SimAgent simagent = (SimAgent) agent;
		ForageHorde simhorde = (ForageHorde) horde;

		Double2D loc = simagent.getLocation();
		Bag stuff = new Bag(simhorde.boxesField.getAllObjects());  // we'll just do a scan
		// stuff.addAll(simhorde.markers.getAllObjects());  // scan the markers too

		Targetable best = (Targetable)stuff.objs[0];
		double bestDistance = loc.distanceSq(best.getTargetLocation(simagent,simhorde));
		for(int i = 0; i < stuff.numObjs; i++)
		{
			Targetable o = (Targetable)(stuff.objs[i]);
			double d = loc.distanceSq(o.getTargetLocation(simagent, simhorde));
			if (d < bestDistance)
			{
				best = o;
				bestDistance = d;
			}
		}
		return best;
	}
}
