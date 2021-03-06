package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.targets.*;

public class IncrementStatus extends SetStatus
{
	private static final long serialVersionUID = 1;

	public IncrementStatus()
	{
		this(1);
	}

	public IncrementStatus(int val)
	{
		super(val);
		name = "IncrementStatus";
		setKeyStroke('8');
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		((SimAgent) agent).setStatus(val);
	}
}
