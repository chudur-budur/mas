package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.targets.*;

public class IncrementCounter extends SetCounter
{
	private static final long serialVersionUID = 1L;

	public IncrementCounter()
	{
		this(1);
	}

	public IncrementCounter(int val)
	{
		super(val);
		name = "IncrementCounter";
		setKeyStroke('3');
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		parent.incrementCounter(Macro.COUNTER_BASIC, 1);
	}
}
