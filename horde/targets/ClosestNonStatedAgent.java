package sim.app.horde.targets;

import sim.app.horde.*;
import sim.util.Bag;
import sim.util.Double2D;
import sim.app.horde.behaviors.*;

/**
 *
 * @author josephzelibor
 */
public class ClosestNonStatedAgent extends Target
{
	private static final long serialVersionUID = 1;

	public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
	{
		SimHorde simhorde = (SimHorde) horde;
		SimAgent simagent = (SimAgent)agent;
		Double2D myLoc = simagent.getLocation();

		//empty bag to hold non stated agents
		Bag nonstatedAgents = new Bag();

		//fill the bag
		for (int i = 0; i < simhorde.agents.getAllObjects().numObjs; i++)
		{
			SimAgent a = (SimAgent)(simhorde.agents.getAllObjects().objs[i]);

			//agent is non stated and non self
			if(a.getStatus()==0 && a != simagent)
			{
				nonstatedAgents.add(a);
			}
		}

		//bag now contains nonself, nonstated agents, lets find the closest
		//are there any agents in the bag?  If not return self
		if(nonstatedAgents.isEmpty())
			return simagent;

		//known: at least 1 agent is in the bag.
		//defaults:
		SimAgent closest = (SimAgent)nonstatedAgents.objs[0];
		double bestDistance = myLoc.distanceSq(closest.getTargetLocation(simagent, simhorde));

		//scan all nonstated agents
		for (int i = 0; i < nonstatedAgents.numObjs; i++)
		{
			SimAgent a = (SimAgent) (nonstatedAgents.objs[i]);

			double d = myLoc.distanceSq(a.getTargetLocation(simagent, simhorde));

			if (d < bestDistance)
			{
				closest = a;
				bestDistance = d;
			}

		}

		return closest;
	}

	public String toString()
	{
		return "Closest Non-Stated Agent";
	}
}
