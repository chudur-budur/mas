package sim.app.horde.targets;
import sim.app.horde.*;
import sim.util.*;
import sim.app.horde.behaviors.*;

public class ClosestStated extends Target
{
	private static final long serialVersionUID = 1;

	public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
	{
		SimHorde simhorde = (SimHorde) horde;
		SimAgent simagent = (SimAgent)agent;
		Double2D loc = simagent.getLocation();
		Bag stuff = new Bag(simhorde.obstacles.getAllObjects());  // we'll just do a scan
		stuff.addAll(simhorde.markers.getAllObjects());  // scan the markers too
		stuff.addAll(simhorde.agents.getAllObjects()); //throw in agents now

		Targetable best = (Targetable)agent;
		double bestDistance = Double.POSITIVE_INFINITY; //hack w/e
		for(int i = 0; i < stuff.numObjs; i++)
		{
			Targetable o = (Targetable)(stuff.objs[i]);
			if(o.getTargetStatus(simagent,simhorde)!=0)
			{
				double d = loc.distanceSq(o.getTargetLocation(simagent, simhorde));
				if (d < bestDistance && d!=0)
				{
					best = o;
					bestDistance = d;
				}
			}
		}
		return best;
	}

	public String toString()
	{
		return "Closest Stated Object";
	}
}
