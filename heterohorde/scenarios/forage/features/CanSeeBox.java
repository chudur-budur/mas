package sim.app.horde.scenarios.forage.features;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.Feature;
import sim.app.horde.objects.Body;
import sim.app.horde.scenarios.forage.Box;
import sim.app.horde.scenarios.forage.ForageHorde;
import sim.app.horde.scenarios.forage.agent.Forager;
import sim.app.horde.targets.Target;
import sim.util.Bag;
import sim.util.Double2D;

public class CanSeeBox extends Feature
    {

    private static final long serialVersionUID = 1L;
    
    public static final String getType() { return "forager"; }

    public CanSeeBox() {
        super("Can See Box");
        targets = new Target[0];
        targetNames = new String[0];
        }

    public double getValue(Agent agent, Macro parent, Horde horde)
        {
        ForageHorde simhorde = (ForageHorde) horde;
        Forager simagent = (Forager) agent;
        Double2D loc = simagent.getLocation();
        Bag boxes = simhorde.obstacles.getAllObjects(); // we'll just do a scan
        double bestDistance = Double.MAX_VALUE; // loc.distanceSq(best.getTargetLocation(simagent,
        // simhorde));

        for (int i = 0; i < boxes.numObjs; i++)
            {
            Box o = (Box) (boxes.objs[i]);
            
            if (!((Body)o).isEnoughAttachments())
                {
                double d = loc.distanceSq(o.getTargetLocation(simagent, simhorde));
                if (d < bestDistance) bestDistance = d;
                }
            }

        if (bestDistance <= Forager.RANGE * Forager.RANGE)
            {
            simagent.setStatus(Forager.SEE_BOX_STATUS);
            return 1;
            }
        //simagent.maskStatus(Forager.SEE_BOX_STATUS);

        return 0;
        }
    }
