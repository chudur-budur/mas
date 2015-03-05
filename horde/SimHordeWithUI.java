package sim.app.horde;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;
import sim.app.horde.objects.*;
import sim.portrayal.grid.*;
import sim.util.gui.*;


public class SimHordeWithUI extends HordeWithUI
{
	private static final long serialVersionUID = 1;

	public Display2D display;
	public JFrame displayFrame;
	public boolean mouseInWindow = false;

	public static int DISPLAY_X = 500;
	public static int DISPLAY_Y = 500;

	public FastValueGridPortrayal2D groundPortrayal = new FastValueGridPortrayal2D(true);
	public FastValueGridPortrayal2D foodPortrayal = new FastValueGridPortrayal2D("Food");
	public ContinuousPortrayal2D agentsPortrayal = new ContinuousPortrayal2D();
	public ContinuousPortrayal2D obstaclesPortrayal = new ContinuousPortrayal2D();
	public ContinuousPortrayal2D regionsPortrayal = new ContinuousPortrayal2D();
	public ContinuousPortrayal2D placesPortrayal = new ContinuousPortrayal2D();

	public String[] getGroundDomain()
	{
		return new String[] { "Dirt", "Cement", "Water", "Dead Puppies" };
	}

	public SimHordeWithUI()
	{
		super(new SimHorde(System.currentTimeMillis()));
		System.err.println("--- SimHordeWithUI.SimHordeWithUI() :"
		                   + " created a new SimHorde and called HordeWithUI(SimHorde)");
	}

	public SimHordeWithUI(SimState state)
	{
		super(state);
		System.err.println("--- SimHordeWithUI.SimHordeWithUI(SimState) :"
		                   + " called HordeWithUI(SimState)");
	}

	public void load(SimState state)
	{
		super.load(state);
		setupPortrayals();
	}

	public void start()
	{
		super.start();

		// resize the display to reflect the new arena
		if (((SimHorde)state).height < ((SimHorde)state).width)
		{
			display.insideDisplay.width = DISPLAY_X;
			display.insideDisplay.height = display.insideDisplay.width *
			                               ((SimHorde)state).height / ((SimHorde)state).width;
		}
		else
		{
			display.insideDisplay.height = DISPLAY_Y;
			display.insideDisplay.width = display.insideDisplay.height *
			                              ((SimHorde)state).width / ((SimHorde)state).height;
		}
		display.detatchAll();
		attachPortrayals();

		setupPortrayals();
		((Horde) state).observer = buttons;
		agentsPortrayal.setFrame(Color.BLUE);
		agentsPortrayal.setAxes(Color.RED);
	}


	public void attachPortrayals()
	{
		display.attach(groundPortrayal, "The Ground!");
		display.attach(foodPortrayal, "The Food!");
		display.attach(regionsPortrayal, "The Regions!");
		display.attach(placesPortrayal, "Targets!");
		display.attach(agentsPortrayal, "The Horde Cometh!");
		display.attach(obstaclesPortrayal, "The Obstacles!");
	}


	public void setupPortrayals()
	{
		final SimHorde horde = (SimHorde) state;
		agentsPortrayal.setField(horde.agents);

		OrientedPortrayal2D op2d = new OrientedPortrayal2D(null, 0, 1.0);

		LabelledPortrayal2D lp2d = new LabelledPortrayal2D(op2d, null)
		{
			private static final long serialVersionUID = 1L;

			public String getLabel(Object object, DrawInfo2D info)
			{
				SimAgent simagent = (SimAgent)object;
				return "Rank: " + simagent.rank +
				       (simagent.status != 0 ? ("\nStatus: " + simagent.status) : "");
			}
		};

		lp2d.setOnlyLabelWhenSelected(true);
		agentsPortrayal.setPortrayalForAll(new AdjustablePortrayal2D(lp2d));

		obstaclesPortrayal.setField(horde.obstacles);
		regionsPortrayal.setField(horde.regions);

		placesPortrayal.setPortrayalForAll(new LabelledPortrayal2D(null, null));
		placesPortrayal.setField(horde.markers);

		foodPortrayal.setField(horde.food);
		groundPortrayal.setField(horde.ground);
		groundPortrayal.setDirtyField(true);  // force a reload even though it's immutable
		groundPortrayal.setPortrayalForAll(new ValuePortrayal2D()
		{
			public Inspector getInspector(LocationWrapper wrapper, GUIState state)
			{
				return new SimpleInspector(
				           new CustomIntFilter(wrapper), state, "Properties");
			}
		});
		// no ground for now
		groundPortrayal.setMap(new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), new Color(0,0,0,0)));
		// no food for now
		foodPortrayal.setMap(new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), new Color(0,0,0,0)));

		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.white);

		// redraw the display
		display.repaint();
	}


	public void init(final Controller c)
	{
		super.init(c);

		// make the displayer
		display = new Display2D(DISPLAY_X, DISPLAY_Y, this);
		display.setClipping(false);
		display.setSelectionMode(Display2D.SELECTION_MODE_SINGLE);

		// Go to non-scrollbar mode
		//display.setMouseChangesOffset(true);
		//display.display.setHorizontalScrollBarPolicy(
		//	ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//display.display.setVerticalScrollBarPolicy(
		//	ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		displayFrame = display.createFrame();
		displayFrame.setTitle("The Horde");
		// register the frame so it appears in the "Display" list);
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);
		displayFrame.remove(display);  // add it separately below

		// attach the portrayals
		attachPortrayals();

		displayFrame.getContentPane().add(display, BorderLayout.CENTER);

		/// tell the console to be slower
		((Console) c).setPlaySleep(consoleDelay);

		/// tell the console to require confirmation for stopping
		((Console) c).setRequiresConfirmationToStop(true);

		// add mouse motion listener
		MouseInputAdapter adapter = new MouseInputAdapter()
		{
			// the object we're dragging
			Object node = null;
			// the wrapper for the object -- useful for selection
			LocationWrapper nodeWrapper = null;
			// our computed difference to be nifty
			Double2D nodeLocDelta = null;
			//updates the mouse locations for the mouse pointer target
			public void mouseMoved(MouseEvent e)
			{
				//only update the mouse pointer if
				//it is in the window and the simulation has started
				if(mouseInWindow && ((Horde)state).started)
				{
					//convert window coordinates into horde cooridinates
					Point2D.Double mousePosition = new Point2D.Double(e.getX(),
					        e.getY());
					Point2D.Double dummy = new Point2D.Double();
					((SimHorde)state).mouseLoc = (Double2D)
					                             (agentsPortrayal.getPositionLocation(mousePosition,
					                                     display.getDrawInfo2D(agentsPortrayal, dummy)));
				}
			}

			public void mouseEntered (MouseEvent e)
			{
				mouseInWindow = true;
			}

			public void mouseExited (MouseEvent e)
			{
				mouseInWindow = false;
			}

			// figure out what object we clicked on (if any) and what the
			// computed difference is.
			public void mousePressed(MouseEvent e)
			{

				final Point point = e.getPoint();
				Continuous2D field = (Continuous2D) (placesPortrayal.getField());
				Continuous2D field2 = (Continuous2D) (agentsPortrayal.getField());
				Continuous2D field3 = (Continuous2D) (obstaclesPortrayal.getField());
				Continuous2D field4 = (Continuous2D) (regionsPortrayal.getField());
				if (field == null)
					return;
				node = null;

				// go through all the objects at the clicked point
				// The objectsHitBy method doesn't return objects:
				// it returns LocationWrappers. You can extract the
				// object by calling getObject() on the LocationWrapper.
				Rectangle2D.Double rect = new Rectangle2D.Double(point.x, point.y, 1, 1);

				Bag hitPlaces = new Bag();
				if (display.getDrawInfo2D(placesPortrayal, rect) != null)
					placesPortrayal.hitObjects(
					    display.getDrawInfo2D(placesPortrayal, rect), hitPlaces);
				Bag hitAgents = new Bag();
				if (display.getDrawInfo2D(agentsPortrayal, rect) != null)
					agentsPortrayal.hitObjects(
					    display.getDrawInfo2D(agentsPortrayal, rect), hitAgents);
				Bag hitObstacles = new Bag();
				if (display.getDrawInfo2D(obstaclesPortrayal, rect) != null)
					obstaclesPortrayal.hitObjects(
					    display.getDrawInfo2D(obstaclesPortrayal, rect),
					    hitObstacles);
				Bag hitRegions = new Bag();
				if (display.getDrawInfo2D(regionsPortrayal, rect) != null)
					regionsPortrayal.hitObjects(
					    display.getDrawInfo2D(regionsPortrayal, rect), hitRegions);

				// We want to grab objects in the same order that
				// they're portrayed: obstacles first, then agents,
				// then places, then regions. The most important
				// items should be last, because we'll grab the
				// topmost element in the bag.
				Bag hit = new Bag();
				hit.addAll(hitRegions);
				hit.addAll(hitPlaces);
				hit.addAll(hitAgents);
				hit.addAll(hitObstacles);

				if (hit.numObjs > 0)
				{
					// grab the topmost one from the user's perspective
					nodeWrapper = ((LocationWrapper) hit.objs[hit.numObjs - 1]);
					node = nodeWrapper.getObject();
					// display.performSelection(nodeWrapper);
					Double2D mouseLoc = new Double2D(0,0);
					// where the node is actually located
					Double2D nodeLoc = (Double2D) (field.getObjectLocation(node));
					if (display.getDrawInfo2D(placesPortrayal, point) != null &&
					        nodeLoc != null)
						mouseLoc = (Double2D)
						           (placesPortrayal.getClipLocation(
						                display.getDrawInfo2D(placesPortrayal,
						                                      point)));
					if (nodeLoc == null &&
					        display.getDrawInfo2D(agentsPortrayal, point) != null)
					{
						// where the node is actually located
						nodeLoc = (Double2D) (field2.getObjectLocation(node));
						mouseLoc = (Double2D) (agentsPortrayal.getClipLocation(
						                           display.getDrawInfo2D(agentsPortrayal, point)));
					}
					if (nodeLoc == null &&
					        display.getDrawInfo2D(regionsPortrayal, point) != null)
					{
						// where the node is actually located
						nodeLoc = (Double2D)(field3.getObjectLocation(node));
						mouseLoc = (Double2D)(regionsPortrayal.getClipLocation(
						                          display.getDrawInfo2D(regionsPortrayal, point)));
					}
					if (nodeLoc == null &&
					        display.getDrawInfo2D(obstaclesPortrayal, point) != null)
					{
						// where the node is actually located
						nodeLoc = (Double2D)(field4.getObjectLocation(node));
						mouseLoc = (Double2D)(obstaclesPortrayal.getClipLocation(
						                          display.getDrawInfo2D(obstaclesPortrayal, point)));
					}
					nodeLocDelta = new Double2D(nodeLoc.x - mouseLoc.x,
					                            nodeLoc.y - mouseLoc.y);
				}
				// get the other displays and inspectors to update their locations
				c.refresh();
				// we need to refresh here only in order to display that the node
				// is now selected, btw: c must be final.
			}

			public void mouseReleased(MouseEvent e)
			{
				node = null;
				// showing perspective reward
				((Horde)state).showPerspectiveReward();
			}

			// We move the node in our Field, adding in the computed difference as necessary
			public void mouseDragged(MouseEvent e)
			{
				final Point point = e.getPoint();
				Continuous2D field = (Continuous2D) (placesPortrayal.getField());
				Continuous2D field2 = (Continuous2D) (agentsPortrayal.getField());
				Continuous2D field3 = (Continuous2D) (obstaclesPortrayal.getField());
				Continuous2D field4 = (Continuous2D) (regionsPortrayal.getField());
				if (node == null || field == null ||
				        display.getDrawInfo2D(placesPortrayal, point) == null)
					return;
				// where the mouse dragged to
				Double2D mouseLoc = (Double2D)(placesPortrayal.getClipLocation(
				                                   display.getDrawInfo2D(placesPortrayal, point)));
				// add in computed difference
				Double2D newBallLoc = new Double2D(nodeLocDelta.x + mouseLoc.x,
				                                   nodeLocDelta.y + mouseLoc.y);
				if (field.getObjectLocation(node) != null)
					field.setObjectLocation(node, newBallLoc);
				else if (field3.getObjectLocation(node) != null)
				{
					field3.setObjectLocation(node, newBallLoc);
					((Body)node).loc = newBallLoc;
				}
				else if (field4.getObjectLocation(node) != null)
				{
					field4.setObjectLocation(node, newBallLoc);
					((Body)node).loc = newBallLoc;
				}
				else  // it's field2, we handle that differently
					((SimAgent) node).setLocation(newBallLoc);
				// get the other displays and inspectors to update their locations
				c.refresh();
				// btw: c must be final.
			}
		};

		// We then attach our listener to the "INSIDE DISPLAY" that's part of the Display2D.
		// The insideDisplay is the object inside the scrollview which does the actual drawing.
		display.insideDisplay.addMouseListener(adapter);
		display.insideDisplay.addMouseMotionListener(adapter);
		////////// END MOVEMENT CODE

		///////// BEGIN POPUP MENU CODE
		MouseInputAdapter popup = new MouseInputAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				final Point point = e.getPoint();
				if ( e.getClickCount() == 1 && ((e.getModifiers() &
				                                 InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) )
				{
					Continuous2D field = (Continuous2D) (placesPortrayal.getField());
					if (field == null) return;
					Rectangle2D.Double rect = new Rectangle2D.Double(point.x,
					        point.y, 1, 1);
					Bag hit = new Bag();
					placesPortrayal.hitObjects(display.getDrawInfo2D(placesPortrayal,
					                           rect), hit);
					Bag hit2 = new Bag();
					agentsPortrayal.hitObjects(display.getDrawInfo2D(placesPortrayal,
					                           rect), hit);
					hit.addAll(hit2);
					Bag hit3 = new Bag();
					obstaclesPortrayal.hitObjects(display.getDrawInfo2D(placesPortrayal,
					                              rect), hit);
					hit.addAll(hit3);
					Bag hit4 = new Bag();
					regionsPortrayal.hitObjects(display.getDrawInfo2D(placesPortrayal,
					                            rect), hit);
					hit.addAll(hit4);
					if (hit.size() > 0)
						selectTarget(display.insideDisplay, point,
						             (Horde)state,
						             ((LocationWrapper)(hit.objs[hit.numObjs-1])).
						             getObject());
				}
			}
		};
		display.insideDisplay.addMouseListener(popup);
	}

	public void selectTarget(Component component, Point point, final Horde horde, Object obj)
	{
		if (!(obj instanceof Targetable)) return;
		final Targetable targetable = (Targetable)obj;
		JPopupMenu popup = new JPopupMenu("Parameter Target");
		for(int i = 0 ; i < Horde.initialParameterObjectName.length; i++)
		{
			final int _i = i;
			JMenuItem menu = new JMenuItem("<html><font color=\""
			                               + SimHorde.parameterObjectColorName[i] + "\">"
			                               + SimHorde.initialParameterObjectName[i]);
			menu.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					((SimHorde)horde).setParameterObject(_i, targetable);
					display.repaint();
				}
			});
			popup.add(menu);
		}
		//Code added by Ian to add status settings to shift r-click menu
		for(int i = 0 ; i < 4; i++)
		{
			final int _i = i;
			JMenuItem menu = new JMenuItem("Set status = " + i);
			menu.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					targetable.setTargetStatus(null,null,_i);
					display.repaint();
				}
			});
			popup.add(menu);
		}
		//End code added by Ian
		popup.show(component, point.x, point.y);
	}

	public void quit()
	{
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

	// public static Paint getTiledPaintForImage(String fileNameRelativeToHorde)
	//     {
	//     return getTiledPaintForImage(Horde.class.getResource(fileNameRelativeToHorde));
	//     }

	// public static Paint getTiledPaintForImage(java.net.URL resource)
	//     {
	//     java.awt.image.BufferedImage b = null;
	//     System.err.println(resource);
	//     try { b = javax.imageio.ImageIO.read(resource); }
	//     catch (java.io.IOException e)
	//         { e.printStackTrace(); return null; }
	//     return new TexturePaint(b, new Rectangle(0,0,b.getWidth(), b.getHeight()));
	//     }

	// we have to make an actual class rather than an
	// anonymous class because reflection doesn't like anonymous subclasses.
	public class CustomIntFilter extends ValuePortrayal2D.IntFilter
	{
		public CustomIntFilter(LocationWrapper wrapper)
		{
			super(wrapper);
		}
		public Object domValue()
		{
			return getGroundDomain();
		}
	}

	public static void main(String[] args)
	{
		// randomizes by currentTimeMillis
		new SimHordeWithUI().createController();
		System.err.println("--- SimHordeWithUI.main() : "
		                   + "called new SimHordeWithUI().createController()");
	}
}
