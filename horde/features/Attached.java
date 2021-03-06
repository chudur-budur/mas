package sim.app.horde.features;
import sim.app.horde.*;
import sim.app.horde.behaviors.*;

/** A simple binary categorical feature which returns 1 or 0 based on whether the
    macro is "done". **/

public class Attached extends CategoricalFeature implements NonDefaultFeature
{
	private static final long serialVersionUID = 1;

	public Attached()
	{
		super("Attached", new String[] {"NotAttached", "Attached"});
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		return (((SimAgent)agent).getManipulated() == null) ? 0.0 : 1.0;
	}
}