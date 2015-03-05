package sim.app.horde.scenarios.forage;

import java.awt.Color;

import sim.app.horde.*;
import sim.engine.SimState;
import sim.portrayal.continuous.*;
import sim.portrayal.simple.*;

public class ForageHordeWithUI extends HordeWithUI
{
	ContinuousPortrayal2D blocksPortrayal = new ContinuousPortrayal2D();
	ContinuousPortrayal2D homePortrayal = new ContinuousPortrayal2D();

	public ForageHordeWithUI()
	{
		this(new ForageHorde(System.currentTimeMillis()));
	}

	public ForageHordeWithUI(SimState state)
	{
		super(state);
	}

	public void setupPortrayals()
	{
		final ForageHorde horde = (ForageHorde) state;

		blocksPortrayal.setField(horde.boxesField);
		blocksPortrayal.setPortrayalForAll(new MovablePortrayal2D(null));
		blocksPortrayal.setFrame(Color.BLUE);

		homePortrayal.setPortrayalForAll(new LabelledPortrayal2D(null, null));
		homePortrayal.setField(horde.homeField);

		super.setupPortrayals();
	}

	public void attachPortrayals()
	{
		display.attach(blocksPortrayal, "Boxes");
		display.attach(agentsPortrayal, "Agents");
		display.attach(homePortrayal, "Home Base");
		super.attachPortrayals();
	}

	public static void main(String[] args)
	{
		new ForageHordeWithUI().createController();
	}
}
