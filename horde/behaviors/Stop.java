package sim.app.horde.behaviors;

import sim.app.horde.*;

/**
 *
 * @author josephzelibor
 */
public class Stop extends Behavior
{
	private static final long serialVersionUID = 1;

	public Stop ()
	{
		name = "Stop";
		setKeyStroke(KS_DOWN);
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);

		SimAgent simagent = (SimAgent) agent;
		//stay at this location
		simagent.setLocation(simagent.getLocation());
		//stopping resets distance traveled
		simagent.resetDistanceTraveled();
	}
}
