package sim.app.horde.targets;

import sim.app.horde.*;
import sim.util.Bag;
import sim.util.Double2D;
import sim.app.horde.behaviors.*;

/**
 *
 * @author josephzelibor
 */
public class ClosestStatedAgent extends Target
{
	private static final long serialVersionUID = 1;

	public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
	{
		SimHorde simhorde = (SimHorde) horde;
		SimAgent simagent = (SimAgent)agent;
		Double2D myLoc = simagent.getLocation();

		//bag to hold stated agents
		Bag statedAgents = new Bag();

		//fill the bag with stated agents
		for (int i = 0; i < simhorde.agents.getAllObjects().numObjs; i++)
		{
			SimAgent a = (SimAgent)simhorde.agents.getAllObjects().objs[i];

			//agent is stated and non self
			if((a.getStatus()!=0) && (a != simagent))
			{
				statedAgents.add(a);
			}
		}

		//bag now contains nonself, stated agents, lets find the closest
		//are there any agents in the bag, if not return self
		if(statedAgents.isEmpty())
			return simagent;

		//known: at least 1 agent is in the bag
		//defaults
		SimAgent closest = (SimAgent)statedAgents.objs[0];
		double bestDistance = myLoc.distanceSq(closest.getTargetLocation(simagent, simhorde));

		//scan all stated agents
		for (int i = 0; i < statedAgents.numObjs; i++)
		{
			SimAgent a = (SimAgent) (statedAgents.objs[i]);

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
		return "Closest Stated Agent";
	}
}
