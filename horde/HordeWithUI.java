package sim.app.horde;

import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.simple.*;
import javax.swing.event.*;
import sim.portrayal.*;
import java.awt.event.*;
import sim.util.*;
import sim.field.continuous.*;
import java.awt.geom.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;
import sim.app.horde.objects.*;
import sim.portrayal.grid.*;
import sim.util.gui.*;


public abstract class HordeWithUI extends GUIState implements ImplementsHordeUI
{
	private static final long serialVersionUID = 1;

	protected long consoleDelay = 30;
	public void setConsoleDelay(long val)
	{
		consoleDelay = val;
	}
	public long getConsoleDelay()
	{
		return consoleDelay;
	}

	int featureLevel = 0;

	private HordeWithUI()
	{
		// this should never be called.
		// Instead call super(new Horde(System.currentTimeMillis()))
		// or use SimHorde
		super(null);
	}

	public HordeWithUI(SimState state)
	{
		super(state);
		System.err.println("--- HordeWithUI.HordeWithUI() :"
		                   + " called GUIState(SimState).");
	}

	public Object getSimulationInspectedObject()
	{
		return state;
	}  // non-volatile

	public static String getName()
	{
		return "The Horde";
	}

	public boolean addListBuilt = false;

	// called at various times to reset the behavior of
	// the agent and reset the buttons to reflect the
	// new behaviors loaded into the agent. Called by
	// start() for example, and also by the "Reset" button
	public void reset()
	{
		// reset the behavior and reload it into the agent
		((Horde)state).resetBehavior();
		Macro macro = ((Horde) state).getTrainingMacro();
		buttons.setBasicBehaviorButtons(macro);
	}

	public void start()
	{
		super.start();

		reset();

		Macro macro = ((Horde) state).getTrainingMacro();
		buttons.setBasicBehaviorButtons(macro);

		if (addListBuilt) // level of permissible features changed
		{
			if (featureLevel != ((Horde)state).featureLevel)
			{
				featureLevel = ((Horde)state).featureLevel;
				addListBuilt = false;
			}
		}

		// clear the addlist and rebuild it if it's not built yet.
		// At this point the parameters were defined in the TrainableMacro
		if (!addListBuilt)
		{
			buildAddList();
			addListBuilt = true;
		}
	}

	public Inspector getInspector()
	{
		Inspector i = super.getInspector();
		i.setVolatile(true);
		return i;
	}

	public ButtonArray buttons;
	public AddList addlist;


	public ButtonArray buildButtonArray(Controller c)
	{
		ButtonArray buttons = new ButtonArray(this);
		JFrame buttonsFrame = new JFrame();
		buttonsFrame.getContentPane().setLayout(new BorderLayout());
		buttonsFrame.getContentPane().add(buttons, BorderLayout.CENTER);
		buttonsFrame.setSize(new Dimension(1000, 150));
		buttonsFrame.setResizable(true);
		buttonsFrame.setTitle("Actions");
		c.registerFrame(buttonsFrame);
		buttonsFrame.setVisible(true);
		return buttons;
	}

	public void init(final Controller c)
	{
		super.init(c);

		// make the addlist
		addlist = new AddList();
		JFrame addlistframe = new JFrame();
		addlistframe.getContentPane().setLayout(new BorderLayout());
		addlistframe.getContentPane().add(addlist, BorderLayout.CENTER);
		addlistframe.setSize(new Dimension(200,400));
		addlistframe.setResizable(true);
		addlistframe.setTitle("Features");
		c.registerFrame(addlistframe);
		addlistframe.setVisible(false);

		buttons = buildButtonArray(c);

		// make the buttons
		/*
		buttons = new ButtonArray(this);
		JFrame buttonsFrame = new JFrame();
		buttonsFrame.getContentPane().setLayout(new BorderLayout());
		buttonsFrame.getContentPane().add(buttons, BorderLayout.CENTER);
		buttonsFrame.setSize(new Dimension(800, 200));
		buttonsFrame.setResizable(true);
		buttonsFrame.setTitle("Actions");
		c.registerFrame(buttonsFrame);
		buttonsFrame.setVisible(true);
		*/
	}

	/***** Code for setting up the Behavior List ******/

	public JPopupMenu createTargetMenu(final TrainableMacro macro, final Targeting targeting)
	{
		// ground targets or wrappers from the behavior
		final Target[] arguments = targeting.getTargets();
		final String[] argumentNames = targeting.getTargetNames();
		// targets they are allowed to be (in wrapper form or ground)
		final Target[] targets = Target.provideAllTargets(macro);
		// return null if no arguments used
		boolean empty = true;
		for(int i = 0; i < arguments.length; i++)
			if (arguments[i] != null)
			{
				empty = false;
				break;
			}
		if (empty) return null;

		// build a menu
		JPopupMenu targetMenu = new JPopupMenu();
		for(int i = 0; i < arguments.length; i++)
			if (arguments[i] != null)
			{
				JMenu menu = new JMenu(argumentNames[i]);
				ButtonGroup group = new ButtonGroup();
				for(int j = 0; j < targets.length; j++)
				{
					final JRadioButtonMenuItem jmi =
					    new JRadioButtonMenuItem("Assign to " + targets[j]);
					group.add(jmi);

					// we need to identify which target is currently being used.
					if (arguments[i] instanceof Wrapper &&
					        targets[j] instanceof Parameter )
					{
						if (((Wrapper)(arguments[i])).isTargeting(macro,
						        (Parameter)(targets[j])))
							jmi.setSelected(true);
					}
					else    // next check if it's a base class.  Let's compare classes
					{
						// it's the one!!!
						if (arguments[i].getClass() == targets[j].getClass())
							jmi.setSelected(true);
					}

					final int _i = i;
					final int _j = j;
					// now add the action listener
					jmi.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							synchronized(state.schedule)
							{
								jmi.setSelected(true);
								System.err.println("TARGET " + targeting + " "
								                   + targeting.getTargetName(_i) +  " SET TO...");
								if (targets[_j] instanceof Parameter)
								{
									Wrapper w = new Wrapper(targeting.getTargetName(_i),
									                        ((Parameter)targets[_j]).getIndex());
									System.err.println("Wrapper " + w);
									// make a wrapper for the Parameter
									targeting.setTarget(_i, w);
								}
								else
								{
									System.err.println("Ground " + targets[_j]);
									// it's just a ground target, don't make a
									// wrapper for it
									targeting.setTarget(_i, targets[_j]);
								}
							}
							// is this necessary?  I think not, and it's really annoying -- Sean
							reset();
						}
					});
					menu.add(jmi);
				}
				targetMenu.add(menu);
			}
		//else System.out.println("Parameter " + i + "("+argumentNames[i]+") not used");
		return targetMenu;
	}

	public AddListCallback buildAddListElement(final Feature _feature)
	{
		return new AddListCallback()
		{
			Feature feature = null;
			Point loc = null;

			public void setComponentLocation(Point loc)
			{
				this.loc = loc;
			}
			public String toString()
			{
				return "" + _feature.getName();
			}
			public void unincludeElement(JComponent element)
			{
				Horde horde = (Horde)state;
				if (feature!=null) horde.removeCurrentFeature(feature);
				else throw new RuntimeException("Unknown feature removed."
					                                + " This shouldn't happen");
				reset();
			}
			public JComponent copyElement()
			{
				Horde horde = (Horde)state;
				feature = (Feature)(_feature.clone());
				final JButton b = new JButton(feature.getName());
				final JPopupMenu menu = createTargetMenu(horde.getTrainingMacro(), feature);
				b.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (menu!= null) menu.show(addlist,
							                           loc.x + b.getWidth(), loc.y);
					}
				});
				horde.addCurrentFeature(feature);

				reset();
				return b;
			}
		};
	}

	public void buildAddList()
	{
		// clear the currrent features
		((Horde)state).clearCurrentFeatures();

		addlist.clear();

		// add the addable features
		final Feature[] f = Feature.provideAllFeatures(featureLevel);
		for(int i = 0; i < f.length; i++)
			addlist.addElement(buildAddListElement(f[i]));

		// add the default features and put them in the Horde as well
		Feature[] def = ((Horde)state).defaultFeatures();
		for(int i = 0; i < def.length; i++)
			addlist.includeElement(buildAddListElement(def[i]));
	}
}
