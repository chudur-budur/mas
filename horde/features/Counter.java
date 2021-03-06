package sim.app.horde.features;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;

public class Counter extends Feature
{
	private static final long serialVersionUID = 1;

	public Counter()
	{
		super("Counter");
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{

		return parent.getCounter(Macro.COUNTER_BASIC);

		/* // we need to extract the counter of the current behavior
		   int b = parent.getCurrentBehavior();
		   if (b == Macro.UNKNOWN_BEHAVIOR)  // uh oh
		   return 0.0;
		   Behavior m = parent.getBehaviors()[b];
		   if (m == null) // uh oh
		   return 0.0;
		   if (!(m instanceof Macro)) // uh oh
		   return 0.0;
		   return ((Macro)m).getCounter(Macro.COUNTER_BASIC);
		*/
	}
}
