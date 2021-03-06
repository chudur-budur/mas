package sim.app.horde.behaviors;

import sim.app.horde.agent.Agent;
import sim.app.horde.transitions.*;
import sim.app.horde.targets.*;
import sim.app.horde.*;

import java.io.*;
import java.util.*;


/** MACRO
        
    <p>A Behavior which is a simple composition of other Behaviors arranged in
    a finite-state automaton (the behaviors are states in the automaton).  Transitions
    are TRANSITION objects which you may specify.  There is one transition object
    for each behavior in the automaton.
        
    <p>You can hard-code a Macro by simply setting up behaviors and transitions
    as you like.  However most Macros are learned, using a subclass called TRAINABLEMACRO.
        
    <p>Macros have a special flag called 'done' which signals that the Macro believes
    that it is done.  There is a feature called DONE which senses this flag and can be
    used in parent macros to move away from this Macro now that it's done.  It's up to the
    user to take advantage of this or not.
        
    <p>Macros begin with the current behavior set at UNKNOWN_BEHAVIOR.  When start() is called,
    the macro's current behavior is then set to INITIAL_BEHAVIOR.  Then start() is called
    on INITIAL_BEHAVIOR.
        
    <p>When go() is called, the Macro works as follows.  First, it calls the transition associated
    with the current behavior.  This transition function tells it where to transition to.  It then
    sets the current behavior to this new value.  If the new current behavior differs from the old
    current behavior, stop() is called on the old behavior and start() is called on the new behavior.
    Then it calls go() on the new current behavior.  This means that INITIAL_BEHAVIOR may never
    have go() called on it -- we could immediately transition to a different behavior.
        
    <p>When stop() is called, the Macro calls stop() on its underlying current behavior.
*/
        

public class Macro extends Behavior
    {
    private static final long serialVersionUID = 1;

    // Single State
    boolean singleState = false;
    public boolean isSingleState() { return this.singleState; }
    public void setSingleState(boolean inputFlag) { singleState = inputFlag; }
    public Transition getTransition(int i)
        {
        if (singleState) return transitions[0];
        else return transitions[i];
        }
    
    public void setTransition(int i, Transition val)
        {
        if (singleState) transitions[0] = val;
        else transitions[i] = val;
        }
        


    // Flags
    public static final boolean FLAGS_ARE_EXCLUSIVE = false;
    public static final int NUM_FLAGS = 2;
    public static final int FLAG_DONE = 0;
    public static final int FLAG_FAILED = 1;
    boolean[] flags = new boolean[NUM_FLAGS];
    public boolean getFlag(int flag) { return flags[flag]; }
    public void setFlag(int flag, boolean val, boolean possiblyPropagate)
        {
        if (FLAGS_ARE_EXCLUSIVE) resetFlags();
        flags[flag] = val;
        if (possiblyPropagate && propagateFlags && _parent != null)
            _parent.setFlag(flag, val, possiblyPropagate);
        }
    public void resetFlags() { for(int i = 0; i < NUM_FLAGS; i++) flags[i] = false; }
    
    boolean propagateFlags = false;
    public void setPropagateFlags(boolean val) { propagateFlags = val; }
    public boolean getPropagateFlags() { return propagateFlags; }
    


    // Counters
    public static final int NUM_COUNTERS = 1;
    public static final int COUNTER_BASIC = 0;
    int[] counters = new int[NUM_COUNTERS];
    public int getCounter(int counter) { return counters[counter]; }
    public void setCounter(int counter, int val) { counters[counter] = val; }
    public void incrementCounter(int counter, int delta) { counters[counter]+= delta; }
    public void resetCounters() { for(int i = 0; i < NUM_COUNTERS; i++) counters[i] = 0; }

    // Timers
    public static final int NUM_TIMERS = 1;
    public static final int TIMER_BASIC = 0;
    public static final int CLEARED_TIMER = -1;
    int[] timers = new int[NUM_TIMERS];
    
                                                              
    public void resetTimer(int timer) { timers[timer] = (int)System.currentTimeMillis(); }  // let's hope it's always positive
    public void clearTimers() { timers = new int[] { Macro.CLEARED_TIMER};  for(int i = 0; i < NUM_TIMERS; i++) timers[i] = CLEARED_TIMER; }
    public int getTimer(int timer) 
        {
        int val = timers[timer];
        if (val == CLEARED_TIMER) return CLEARED_TIMER;
        int currentTime = (int) (0x000000007FFFFFFF & System.currentTimeMillis());
        if (currentTime < val)  // this condition shouldn't ever happen
            throw new RuntimeException("Timer was less than time val, this shouldn't happen");
        else return currentTime - val;
        }

    // Associated objects
    // If the target is null, it's assumed to be the "default target" -- Me, for example
    public static final int NUM_ASSOCIATED_OBJECTS = 1;
    public static final int ASSOCIATED_OBJECT_BASIC = 0;
    Targetable associatedObjects[] = new Targetable[NUM_ASSOCIATED_OBJECTS];
    public Targetable getAssociatedObject(int assoc) { return associatedObjects[assoc]; }
    public void setAssociatedObject(int assoc, Targetable val) { associatedObjects[assoc] = val; }
    public void resetAssociatedObjects() { for(int i = 0; i < NUM_ASSOCIATED_OBJECTS; i++) associatedObjects[i] = null; }

    public void setDone(boolean isDone) { flags[FLAG_DONE] = isDone; }
    public Behavior[] behaviors;
    protected Transition[] transitions;
    public int currentBehavior = UNKNOWN_BEHAVIOR;
        
    public String[] getBehaviorNames()
        {
        String[] names = new String[behaviors.length];
        for(int i = 0; i < behaviors.length; i++)
            { 
            if (behaviors[i] == null)
            	names[i] = "null";
            else
            	names[i] = behaviors[i].getName();
            }
        return names;
        }
    
    public Object clone()
        {
        Macro f = (Macro)(super.clone());
                        
        // clone behaviors
        f.behaviors = new Behavior[behaviors.length];
        for(int i = 0 ; i < f.behaviors.length; i++)
            if (behaviors[i] != null)
                // && (behaviors[i] instanceof Macro || behaviors[i] instanceof LevelBehavior))  // oops, even basic behaviors need to be cloned, because they may have different targets
                { f.behaviors[i] = (Behavior)(behaviors[i].clone()); }
        // else f.behaviors[i] = behaviors[i];  // just copy over
                                
        // clone transitions
        f.transitions = new Transition[behaviors.length];
        if (singleState)
            {
            f.setTransition(0, getTransition(0) == null ? null : (Transition)(getTransition(0).clone()));
            }
        else
            {
            for(int i = 0 ; i < f.behaviors.length; i++)
                f.setTransition(i, getTransition(i) == null ? null : (Transition)(getTransition(i).clone()));
            }
                
        // clone flags, counters, associatedObjects
        f.flags = (boolean[])(flags.clone());
        f.counters = (int[])(counters.clone());
        
        
        if (associatedObjects != null)
            f.associatedObjects = (Targetable[])(associatedObjects.clone());  // we don't clone the target
        return f;
        }
        
        
    public static final int INITIAL_BEHAVIOR = 0;         // this will be "START"
    public static final int UNKNOWN_BEHAVIOR = -1;
        
    public Behavior[] getBehaviors() { return behaviors; }
    protected Transition[] getTransitions() { return transitions; }  // should ONLY be used by TrainableMacro
    public int getCurrentBehavior() { return currentBehavior; }
        
    public Macro()
        {
        this(new Behavior[] { new Start() }, new Transition[] { null });  // dummy initial behaviors and transitions
        }
        
    public Macro(Behavior[] behaviors, Transition[] transitions)
        {
        super();
        this.behaviors = behaviors;
        this.transitions = transitions;
        name = "Macro";
        currentBehavior = UNKNOWN_BEHAVIOR; 
        }

    /** Searches through the behavior array for a behavior which matches the given name, and returns its behavior index number. */
    public int indexOfBehaviorNamed(String name)
        {
        for(int i = 0 ; i < behaviors.length; i++)
            if (behaviors[i].getName().equalsIgnoreCase(name)) return i;
        return -1;
        }

    public void start(Agent agent, Macro parent, Horde horde)
        {
        super.start(agent, parent, horde);
        currentBehavior = INITIAL_BEHAVIOR;
        resetCounters();
        clearTimers();
        resetAssociatedObjects();
        if (behaviors[currentBehavior] !=null && behaviors[currentBehavior] instanceof Flag) 
            throw new RuntimeException("INITIAL BEHAVIOR == " + behaviors[currentBehavior] + "!  This should never be allowed to happen.");
        resetFlags();
        behaviors[currentBehavior].start(agent, this, horde);
        Transition transition = getTransition(currentBehavior);
        if (transition != null) transition.start(agent, this, horde);
        if (horde.observer != null) horde.observer.transitioned(this, UNKNOWN_BEHAVIOR, INITIAL_BEHAVIOR);
        }

    public void stop(Agent agent, Macro parent, Horde horde) 
        {
        super.stop(agent, parent, horde);
        if (currentBehavior != UNKNOWN_BEHAVIOR)
            {
            behaviors[currentBehavior].stop(agent, this, horde);
            Transition transition = getTransition(currentBehavior);
            if (transition != null) transition.stop(agent, this, horde);
            }
        int oldBehavior = currentBehavior;
        currentBehavior = UNKNOWN_BEHAVIOR;
        if (horde.observer != null) horde.observer.transitioned(this, oldBehavior, currentBehavior);
        }

    /** Calls super.go() but doesn't do any Macro.go() code.  Used by TrainableMacro to do its own go() method but
        still use Behavior.go() code as well. */
    protected final void goBypass(Agent agent, Macro parent, Horde horde) { super.go(agent, parent, horde); }
    
    transient Macro _parent = null;  // used to establish back-pointers for flag propagation in setFlag
    public void go(Agent agent, Macro parent, Horde horde)       
        {
        super.go(agent, parent, horde);
        _parent = parent;
        if (currentBehavior == UNKNOWN_BEHAVIOR) // should never happen
            throw new RuntimeException("go() called on UNKNOWN_BEHAVIOR. This should not be able to happen.");
                        
        int newBehavior = currentBehavior;
                
        Transition transition = getTransition(currentBehavior);
        if (transition != null) newBehavior = transition.change(agent, this, horde);
                
        if (behaviors[newBehavior] instanceof Flag)  // "done"
            {
            fireFlag(((Flag)(behaviors[newBehavior])).getFlag(), agent, parent, horde);
            newBehavior = INITIAL_BEHAVIOR;     // immediately transition
            }
        if (newBehavior != currentBehavior)
            {
            behaviors[currentBehavior].stop(agent, this, horde);
            if (transition != null) transition.stop(agent, this, horde);

            // we set the currenBehavor here so that in agent.behaviorBacktrace()
            // or Behavior.behaviorBacktrace() the current behavior is properly
            // listed even if it's called during the start(...) method of the 
            // new behavior.  This often happens if we need to debug the start(...)
            // method when it's the only place in the behavior that anything happens.
                        
            currentBehavior = newBehavior;

            behaviors[newBehavior].start(agent, this, horde);
            Transition newTransition = getTransition(newBehavior);
            if (newTransition != null) newTransition.start(agent, this, horde);
            resetFlags();
            }
            
        // this will now be the new behavior
        behaviors[currentBehavior].go(agent, this, horde);
        }


    /**
     * Sets done in the parent macro only.
     */
    public void fireDone(Agent agent, Macro parent, Horde horde) {
        if (parent != null)
            parent.setDone(true);
        }
    
    /** Signals Done and sets it in the parent, potentially propagating the flag */
    public void fireFlag(int flag, Agent agent, Macro parent, Horde horde)
        {
        if (parent != null) parent.setFlag(flag, true, true);
        }

    public void write(PrintWriter writer, HashSet<String> behaviorsSoFar) { }

    public void performTransition(int newBehavior, Agent agent, Horde horde)
        {
        if (currentBehavior > -1) { 
            behaviors[currentBehavior].stop(agent, this, horde);
            Transition trans = getTransition(currentBehavior);
            if (trans != null) trans.stop(agent, this, horde);
            }
                
        behaviors[newBehavior].start(agent, this, horde);
        Transition newTransition = getTransition(newBehavior);
        if (newTransition != null) newTransition.start(agent, this, horde);

        currentBehavior = newBehavior;
        resetFlags();
        } 

    protected StringBuilder getBehaviorBacktrace(StringBuilder builder)
        {
        // default:
        super.getBehaviorBacktrace(builder);
        if (currentBehavior < 0)
            {
            builder.append("--> INVALID CHILD");
            return builder;
            }
        else
            return getBehaviors()[currentBehavior].getBehaviorBacktrace(builder);
        }
        
    }
