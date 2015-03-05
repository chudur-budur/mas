package sim.app.horde.behaviors;
import sim.app.horde.*;

public class Rotate extends Behavior
{

	private static final long serialVersionUID = 1;
	public double speed;

	public Rotate()
	{
		this(2);
	} // good default

	public Rotate(double speed)
	{
		this.speed = speed;
		name = "Rotate[" + speed + "]";
		setKeyStroke(KS_RIGHT);
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);

		SimAgent simagent = (SimAgent) agent;
		simagent.setOrientation(simagent.getOrientation() + speed / 360.0 * Math.PI * 2);
	}
}
