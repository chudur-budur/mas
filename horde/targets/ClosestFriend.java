package sim.app.horde.targets;
import sim.app.horde.*;
import sim.util.*;
import java.util.ArrayList;
import sim.app.horde.behaviors.*;
public class ClosestFriend extends Target
{
	private static final long serialVersionUID = 1;

	public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
	{
		SimHorde simhorde = (SimHorde) horde;
		SimAgent simagent = (SimAgent)agent;
		Double2D loc = simagent.getLocation();
		ArrayList<Agent> agentList = simhorde.agentList;

		Targetable best = (Targetable)agentList.get(0);
		double bestDistance = Double.POSITIVE_INFINITY; //hack w/e
		for(int i = 0; i < agentList.size(); i++)
		{
			Targetable o = (Targetable)(agentList.get(i));
			double d = loc.distanceSq(o.getTargetLocation(simagent, simhorde));
			if (d < bestDistance && o!=(Targetable)simagent && o.getTargetStatus(simagent,simhorde)==simagent.getStatus())
			{
				best = o;
				bestDistance = d;
			}
		}
		return best;
	}

	public String toString()
	{
		return "ClosestFriend";
	}
}
