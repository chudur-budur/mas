package sim.app.horde;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import sim.portrayal.DrawInfo2D;

public class ControllerAgent extends SimAgent
{
	private static final long serialVersionUID = 1L;

	public ArrayList subsidiaryAgents;
	public int getNumSubsidiaryAgents()
	{
		return subsidiaryAgents.size();
	}
	public void addSubsidiaryAgent(Agent a)
	{
		subsidiaryAgents.add(a);
		a.controller = this;
	}

	public ControllerAgent(Horde horde, int level)
	{
		super(horde, level);
		subsidiaryAgents = new ArrayList();
		scale = scale * 2 * level;
	}

	// draw in bright pink, and twice as large as the other agents
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		paint = Color.pink;
		super.drawBypass(object, graphics, info);
	}
}
