package sim.app.horde.behaviors;
import sim.app.horde.*;

public abstract class CollectiveBehavior extends Behavior
{
	private static final long serialVersionUID = 1;
	String behaviorName = "WRONG";  // override this

	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);
		//((SimHorde)horde).toSocket.println(behaviorName);
		Horde.toSocket.println(behaviorName);
	}
}
