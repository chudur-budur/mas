package sim.app.horde.targets;
import sim.app.horde.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.*;
import sim.util.*;

/**

   WRAPPER
        
   <p>Wrappers are Targets attached to lower-level Behaviors and Features which enable them to refer to one of the
   Parameters in the outer TrainableMacro.  This is basically a parameter binding mechanism.  Thus Wrappers have
   an index which refers to a member of the Targets array in the outer TrainableMacro (you can get one of the
   Parameters with macro.getTarget(index).
        
   <p>Wrappers are typically inserted in TrainableMacros to displace Parameters that previously existed
   there after they've been loaded and are ready to be used as a subsidiary Behavior in an outer TrainableMacro.
   Wrappers have names.  Typically they're set to the name of the Parameter which they replaced.  Also the intial
   index value of the Wrapper is commonly set to the original index of the Parameter it replaces, though it could
   certainly be set to zero.
        
   <p>Wrappers override all the standard Target methods to instead call the same methods on their underlying Parameter,
   which is returned by the method getParameter(parent).  When the same methods are called, the underlying Parameter
   is not given the Macro "parent" but rather "parent"'s parent.
*/


public class Wrapper extends Target
    {
    private static final long serialVersionUID = 1;
    int index;  // a pointer to the particular parameter in the TrainableMacro's Targets array
    String name;
        
    // doesn't call start or stop on the underlying target -- we assume that's done by the macro
        
    public Wrapper(String name, int index)  { this.index = index; this.name = name; }
        
    public Target getParameter(Behavior parent)
        {
        return parent.getTarget(index);
        }  // inlined for sure
        
    public Target getTopParameter(Behavior parent) 
        {
        Target t = getParameter(parent); 
        if (t instanceof Wrapper) { 
            return ((Wrapper) t).getTopParameter(parent.getParent()); 
            }
        return t;
        }
        
    /*
      public String getParentList(Behavior parent) 
      {
      if (parent == null) return "!";
      Target t = getParameter(parent); 
      if (t == null) return parent.getName() + index + " ?";
        
      String s = parent.getName();
      if (t instanceof Wrapper) { 
      s = s + "/" + getParentList(parent.getParent());
      }
      return s;
      }
        
      public String allParameters(Behavior parent)
      {
      String s = "";
      for(int i = 0; i < parent.getNumTargets(); i++)
      s = s + parent.getTarget(i) + "/";
      return s;
      }
        
      public String allParameterNames(Behavior parent)
      {
      String s = "";
      for(int i = 0; i < parent.getNumTargets(); i++)
      s = s + parent.getTargetName(i) + "/";
      return s;
      }
    */
    
    public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
        {
        return getParameter(parent).getTargetable(agent, parent.getParent(), horde);
        }

    public int getStatus(Agent agent, Macro parent, Horde horde)
        {
        return getParameter(parent).getStatus(agent, parent.getParent(), horde);
        }

    public int getRank(Agent agent, Macro parent, Horde horde)
        {
        return getParameter(parent).getRank(agent, parent.getParent(), horde);
        }

    public Double2D getLocation(Agent agent, Macro parent, Horde horde)
        {
        return getParameter(parent).getLocation(agent, parent.getParent(), horde);
        }
                
    public double getOrientation(Agent agent, Macro parent, Horde horde)
        {
        return getParameter(parent).getOrientation(agent, parent.getParent(), horde);
        }
        
    public boolean isTargeting(Macro parent, Parameter other)
        {
        return getParameter(parent) == other;
        }
        
    public void setName(String s) { name = s; }
    public String getName() { return name; }
    public int getIndex() { return index; }
    public void setIndex(int i) { index = i; }
            
    public String toString() { return getName(); }
    }
