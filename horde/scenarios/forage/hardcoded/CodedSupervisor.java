package sim.app.horde.scenarios.forage.hardcoded;

import sim.app.horde.*;
import sim.app.horde.scenarios.forage.*;

public class CodedSupervisor extends Supervisor
{
	private static final long serialVersionUID = 1L;

	public static final int SUPERVISE = 0;
	public static final int GRAB_BOX = 1;

	public CodedSupervisor(Horde horde, int level)
	{
		super(horde, level);
		coded = true;
	}

	public int getSupervisorLevel()
	{
		return level;
	}
}
