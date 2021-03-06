package sim.app.horde.scenarios.boxpushing.features; 

import sim.app.horde.features.*; 
import sim.app.horde.agent.*; 
import sim.app.horde.behaviors.Macro;
import sim.app.horde.*; 
import sim.app.horde.scenarios.boxpushing.*; 

public class EveryFlockbotDone extends CategoricalFeature
    {

    private static final long serialVersionUID = 1L;

    public EveryFlockbotDone()
        {
        super("EveryFlockbotDone", new String[] {"NotDone", "Done"});
        }
        

    public double getValue(Agent agent, Macro parent, Horde horde) 
        {
        FlockbotController fc = (FlockbotController) agent; 
        if (FlockbotController.NUM_FLOCKBOTS == fc.getDoneCount())
            return 1; 
        else 
            return 0; 
                
        }
        
    }