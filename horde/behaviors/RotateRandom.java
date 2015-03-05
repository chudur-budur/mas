package sim.app.horde.behaviors;
import sim.app.horde.*;

public class RotateRandom extends Behavior
{
	private static final long serialVersionUID = 1;

	public RotateRandom()
	{
		name = "RotateRandom";
		setKeyStroke('?');
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);
		SimAgent simagent = (SimAgent) agent;
		simagent.setOrientation(horde.random.nextDouble() * Math.PI * 2);
	}
}
