package sim.app.horde.features;

import sim.app.horde.*;
import sim.app.horde.targets.*;
import sim.app.horde.behaviors.*;
import sim.util.*;

/** Describes the cosine simiarility between two vectors.
    This value ranges from -1 to 1, where -1 is highly nosimilar
    (pointing in opposite directions) and 1 is pointing in the same direction.
    If one vector is zero, then the value is -1.  If two
    vectors are zero, then the value is 1.
*/

public class DirectionSimilarity extends Feature
{
	private static final long serialVersionUID = 1;

	public DirectionSimilarity()
	{
		super("DirectionSimilarity");
		targets = new Target[3];
		targets[0] = new Me();  //  default
		targets[1] = new Me();
		targets[2] = new Me();
		targetNames = new String[] { "0", "1", "2" };
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		SimAgent simagent = (SimAgent) agent;
		Double2D loc0 = targets[0].getLocation(agent, parent, horde);
		Double2D loc1 = targets[1].getLocation(agent, parent, horde).subtract(loc0);
		Double2D loc2 = targets[2].getLocation(agent, parent, horde).subtract(loc0);

		if (loc1.x == 0 && loc1.y == 0)  // zero distance, can't compute angle
		{
			if (loc2.x == 0 && loc2.y == 0)
				return 1.0;  // very similar
			else return -1.0;
		}
		else if (loc2.x == 0 && loc2.y == 0)  // zero distance, can't compute angle
		{
			return -1.0;
		}
		else
		{
			return loc1.dot(loc2) / (loc1.length() * loc2.length());  // that's what Wikipedia says :-)
		}
	}
}
