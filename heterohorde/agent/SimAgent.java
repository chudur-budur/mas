/*
 * Copyright 2006 by Sean Luke and George Mason University Licensed under the Academic Free License version 3.0 See the
 * file "LICENSE" for more information
 */
package sim.app.horde.agent;

import java.awt.*;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.objects.*;
import sim.engine.*;
import sim.portrayal.*;
import sim.util.*;
import ec.util.*;

/**
 * SimAgent
 */

public class SimAgent extends Agent implements Steppable, Orientable2D, Targetable
    {
    private static final long serialVersionUID = 1L;

    /** The type of Behaviors and features designed to work with SimAgents. */
    public static String TYPE_SIMAGENT = "sim";
    /** Parameter for shapes in SimAgent. */
    public static Parameter P_SHAPE = new Parameter("shape");
    /** Parameter for colors in SimAgent. */
    public static Parameter P_COLOR = new Parameter("color");
    /** Parameter for whether drawing is done filled in SimAgent. */
    public static Parameter P_FILLED = new Parameter("filled");
    /** Parameter for the draw scale in SimAgent. */
    public static Parameter P_SCALE = new Parameter("scale");
    
    
    /// VARIABLES WHICH AFFECT FEATURES OF SIMAGENTS
    
    /** Current Location of the SimAgent. */
    Double2D loc = new Double2D(0,0);
    /** Previous Location of the SimAgent.  Used to update distanceTraveled. */
    Double2D prevLoc = loc;
    /** Orientation of the SimAgent. */
    double orientation = 0;
    /** Orientation as a unit vector of the SimAgent. */
    Double2D orientationVector = new Double2D(1.0, 0.0);
    /** Status (used in behavior SetStatus and Feature Status) of the SimAgent */
    int status = 0;
    /** Rank (used in various rank comparison functions) of the SimAgent.
        This might go away if we do some within-AgentGroup ranking or something more
        useful. */              
    int rank;
    /** The total distance traveled.  Used by the DistanceTraveled feature. */
    double distanceTraveled;
    /** The parameter value, settable via the Targetable interface.  SimAgents can be 
        themselves temporary parameters (A, B, C) of the simulation, for testing purposes.
        If none of these (0, 1, 2), then -1 indicates that the SimAgent is not
        presently a parameter Targetable. */
    int parameterValue = -1; // if I am a parameter target(A/B/C), what am I? Else I'm -1

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        if (selected)
            horde.setTrainingAgent(this);  // attempt it.  It may not be permitted.
        return super.setSelected(wrapper, selected);
        }

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        // first draw basic stuff -- target information, etc.
        if (parameterValue >= 0)
            setPaint(SimHorde.parameterObjectColor[parameterValue]);
        else if (isTheTrainingAgent())
            setPaint(Color.BLACK);
        else
            setPaint(defaultPaint);
        
        /*
        // I am an immediate subsidiary
        SimControllerAgent controller = (SimControllerAgent)(getGroup().getController());
        if (controller != null && controller.isTraining())
        {
        Color gColor = graphics.getColor();
        Double2D l = controller.getLocation();
        double dx = l.x - loc.x;
        double dy = l.y - loc.y;
        int x2 = (int) (info.draw.x + info.draw.width * dx);
        int y2 = (int) (info.draw.y + info.draw.height * dy);
        graphics.setColor(Color.lightGray);
        graphics.drawLine((int) info.draw.x, (int) info.draw.y, x2, y2);
        graphics.setColor(gColor);
        }
        // I am training myself
        else if (controller != null && isTraining())
        {
        Color gColor = graphics.getColor();
        Double2D l = controller.getLocation();
        double dx = l.x - loc.x;
        double dy = l.y - loc.y;
        int x2 = (int) (info.draw.x + info.draw.width * dx);
        int y2 = (int) (info.draw.y + info.draw.height * dy);
        graphics.setColor(Color.RED);
        graphics.drawLine((int) info.draw.x, (int) info.draw.y, x2, y2);
        graphics.setColor(gColor);
        }
        */
        
        super.draw(object, graphics, info);
        }

    public void setup(ec.util.ParameterDatabase db)
        {
        super.setup(db);
        rank = ((SimHorde)horde).assignRank();
        resetDistanceTraveled();
        if (db.exists(P_SHAPE, null))
            setShape(db.getString(P_SHAPE, null));
        else
            setShape(SHAPE_CIRCLE);
        if (db.exists(P_COLOR, null))
            setDefaultColor(db.getString(P_COLOR, null));
        else
            setPaint(Color.BLACK);
        setFilled(db.getBoolean(P_FILLED, null, true));
        if (db.exists(P_SCALE, null))
            {
            double s = db.getDouble(P_SCALE, null, 0.0);
            if (s <= 0.0)
                {
                System.err.println("WARNING: invalid scale " + s + ", must be > 0.0 in " + db.getLabel() + ", using 1.0");
                s = 1.0;
                }
            setScale(s);
            }
        else setScale(1.0);             
        }

    public void restart(Horde horde)
        {
        super.restart(horde);
        if (started) resetDistanceTraveled();
        }

    /** Updates the distance traveled, steps the agent, and nudges any manipulated objects. */
    public void step(SimState state)
        {
        distanceTraveled += this.prevLoc.distance(loc);
        prevLoc = loc;
        super.step(state);
        nudge((Horde) state, loc, prevLoc);
        }
        
        
        
        
        
    ///// AGENT STATISTICS FOR VARIOUS FEATURES
        
    public double getDistanceTraveled()
        {
        return distanceTraveled;
        }

    public void resetDistanceTraveled()
        {
        distanceTraveled = 0;
        }
        
    boolean stuck;
    public void setStuck(boolean val)
        {
        stuck = val;
        }

    public boolean getStuck()
        {
        return stuck;
        }


        
        
    ///// METHODS FOR TARGETABLE INTERFACE
    ///// AND THEIR EQUIVALENT SIMPLER JAVA BEAN PROPERTY METHODS


    public void setParameterValue(int index)
        {
        parameterValue = index;
        }
        
    public boolean getTargetIntersects(Agent agent, Horde horde, Double2D location, double slopSquared)
        {
        return getTargetLocation(agent, horde).distanceSq(location) <= slopSquared;
        }

    public Double2D getTargetLocation(Agent agent, Horde horde)
        {
        return getLocation();
        }

    public Double2D getLocation()
        {
        return loc;
        }

    /** Returns the previous location of the agent prior to last step */
    public Double2D getPrevLocation()
        {
        return prevLoc;
        }

    public void setTargetLocation(Agent agent, Horde horde, Double2D location)
        {
        setLocation(location);
        }

    public void setLocation(Double2D location)
        {
        prevLoc = loc;
        loc = location;
        ((SimHorde) horde).agents.setObjectLocation(this, location);
        }

    public void setTargetStatus(Agent agent, Horde horde, int status)
        {
        setStatus(status);
        }

    public void setStatus(int status)
        {
        this.status = status;
        }

    public int getTargetStatus(Agent agent, Horde horde)
        {
        return getStatus();
        }

    public int getStatus()
        {
        return status;
        }

    public int getTargetRank(Agent agent, Horde horde)
        {
        return getRank();
        }

    public int getRank()
        {
        return rank;
        }

    public void setTargetRank(Agent agent, Horde horde, int rank)
        {
        setRank(rank);
        }

    public void setRank(int rank)
        {
        this.rank = rank;
        }




    ////// ORIENTATION METHODS
        
    public void setOrientation(double val)
        {
        double x = Math.cos(val);
        double y = Math.sin(val);
        //orientation = Math.atan2(y, x); // normalize to between -Pi and +Pi
        
        // [[ cheaper normalization, saves an atan2 -- Sean]]
        orientation = Utilities.normalizeAngle(val);  // this function ranges between 0 and 2PI
        if (orientation > Math.PI) orientation -= Math.PI * 2; // move to between -Pi and +Pi
        orientationVector = new Double2D(x, y);
        }

    public double getOrientation()
        {
        return orientation;
        }

    public Interval domOrientation()
        {
        return new Interval(-Math.PI, Math.PI);
        }

    /** Returns the orientation as a normalized vector. */
    public Double2D getOrientationVector()
        {
        return orientationVector;
        }

    /** Versions of setOrientation for the Orientable2D interface. */
    public void setOrientation2D(double val)
        {
        setOrientation(val);
        }

    public double orientation2D()
        {
        return orientation;
        }




    /////// OBJECT MANIPULATION METHODS

    /** The object presently being manipulated. */
    public Targetable manipulated = null;
    
    /** Attaches to a Target to manipulate it. */
    public void setManipulated(Targetable m) 
        { 
        if (manipulated != null && manipulated instanceof Body)
            ((Body)manipulated).decrementAttachment();
            
        manipulated = m;
        if (manipulated != null)
            {
            manipulatedGoalLoc = manipulated.getTargetLocation(this, horde);
            if (manipulated instanceof Body)
                ((Body)manipulated).incrementAttachment();
            }
        
        }
    
    public Targetable getManipulated()
        {
        return manipulated;
        }
    
    
    Double2D manipulatedGoalLoc = new Double2D();
    public static final double NUDGE_MULTIPLIER = 0.1;

    /** Nudges a target in the same direction as the agent is moving. */
    public void nudge(Horde horde, Double2D agentLoc, Double2D previousAgentLoc)
        {
        if (manipulated == null)  // nothing there
            return;
            
        manipulatedGoalLoc = manipulatedGoalLoc.add(agentLoc.subtract(previousAgentLoc));
        
        if (manipulated instanceof Body)
            {
            if (((Body)manipulated).collision(agentLoc))  // don't move yet
                return;
            }
            
        Double2D curloc = manipulated.getTargetLocation(this, horde);
        Double2D diff = manipulatedGoalLoc.subtract(curloc).multiply(NUDGE_MULTIPLIER);
        manipulated.setTargetLocation(this, horde, curloc.add(diff));
        }




    }
