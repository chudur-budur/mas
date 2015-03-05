package sim.app.horde.scenarios.forage.features;

import sim.app.horde.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.*;
import sim.app.horde.scenarios.forage.*;
import sim.app.horde.targets.Target;
import sim.util.Bag;
import sim.util.Double2D;

public class CanSeeBox extends Feature
{

	private static final long serialVersionUID = 1L;

	public CanSeeBox()
	{
		super("Can See Box");
		targets = new Target[0];
		targetNames = new String[0];
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		ForageHorde simhorde = (ForageHorde) horde;
		Forager simagent = (Forager) agent;
		Double2D loc = simagent.getLocation();
		Bag boxes = simhorde.boxesField.getAllObjects(); // we'll just do a scan
		double bestDistance = Double.MAX_VALUE; // loc.distanceSq(best.getTargetLocation(simagent,
		// simhorde));

		for (int i = 0; i < boxes.numObjs; i++)
		{
			Box o = (Box) (boxes.objs[i]);
			double d = loc.distanceSq(o.getTargetLocation(simagent, simhorde));
			if (d < bestDistance)
				bestDistance = d;
		}

		if (bestDistance <= Forager.RANGE * Forager.RANGE)
		{
			simagent.setStatus(Forager.SEE_BOX_STATUS);
			return 1;
		}
		simagent.clearStatus(Forager.SEE_BOX_STATUS);

		return 0;
	}
}
