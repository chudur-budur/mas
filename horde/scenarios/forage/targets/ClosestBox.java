package sim.app.horde.scenarios.forage.targets;

import sim.app.horde.scenarios.forage.*;
import sim.app.horde.*;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.objects.Body;
import sim.app.horde.targets.Target;
import sim.util.Bag;
import sim.util.Double2D;

public class ClosestBox extends Target
{
	private static final long serialVersionUID = 1;

	public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
	{
		ForageHorde simhorde = (ForageHorde) horde;
		SimAgent simagent = (SimAgent) agent;
		Double2D loc = simagent.getLocation();
		Bag boxes = simhorde.boxesField.getAllObjects(); // we'll just do a scan
		Body best = (Body) (boxes.objs[0]);
		double bestDistance = loc.distanceSq(best.getTargetLocation(simagent, simhorde));
		for (int i = 0; i < boxes.numObjs; i++)
		{
			Body o = (Body) (boxes.objs[i]);
			double d = loc.distanceSq( o.getTargetLocation(simagent, simhorde));
			if (d < bestDistance)
			{
				best = o;
				bestDistance = d;
			}
		}

		//if (bestDistance <= Forager.RANGE)
		return best;
		//return null;
	}

	public String toString()
	{
		return "Closest Box";
	}

}
