package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.targets.*;

public class SetCounter extends Behavior
{
	private static final long serialVersionUID = 1L;
	int val;

	public boolean shouldAddDefaultExample()
	{
		return false;
	}

	public SetCounter()
	{
		this(1);
	}

	public SetCounter(int val)
	{
		this.val = val;
		name = "SetCounter";
		setKeyStroke('2');
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		parent.setCounter(Macro.COUNTER_BASIC, 1);
	}
}
