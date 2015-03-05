package sim.app.horde.features;
import sim.app.horde.behaviors.*;
import sim.app.horde.*;

/**
 *
 * @author josephzelibor
 */
import sim.app.horde.Agent;
import sim.app.horde.Horde;

public class DistanceTraveled extends Feature
{
	private static final long serialVersionUID = 1;

	public DistanceTraveled ()
	{
		super("DistanceTraveled");
	}

	public double getValue (Agent agent, Macro parent, Horde horde)
	{
		SimAgent simagent = (SimAgent) agent;
		return simagent.getDistanceTraveled();
	}
}
