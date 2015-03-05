package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.targets.*;

public class ResetCounter extends SetCounter
{
	private static final long serialVersionUID = 1L;

	public ResetCounter()
	{
		super(0);
		name = "ResetCounter";
		setKeyStroke('9');
	}
}
