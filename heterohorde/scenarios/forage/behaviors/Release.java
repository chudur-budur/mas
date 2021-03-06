package sim.app.horde.scenarios.forage.behaviors;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Behavior;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.objects.Body;
import sim.app.horde.scenarios.forage.agent.Forager;
import sim.app.horde.scenarios.forage.agent.Supervisor;

public class Release extends Behavior
    {
    private static final long serialVersionUID = 1;
    
    public static final String getType() { return "forager"; }

    public Release() { name = "Release"; setKeyStroke('r'); }

    public boolean shouldAddDefaultExample() { return false; }

    public void go(Agent agent, Macro parent, Horde horde)
        {
        super.go(agent, parent, horde);

        Forager simagent = (Forager) agent;

        if (simagent.manipulated != null)
            {
            int status = simagent.manipulated.getTargetStatus(simagent, horde);
            if (status > 0) status--;
            simagent.manipulated.setTargetStatus(simagent, horde, status);
            ((Body) simagent.manipulated).decrementAttachment();
            simagent.manipulated = null;

            //parent.finished = true;
            }

        //simagent.resetBiggest();
        // simagent.resetParent();

        Supervisor ctrl = (Supervisor) simagent.getGroup().getController();

        if (ctrl != null) ctrl.resetBiggest(simagent);
        }

    public void stop(Agent agent, Macro parent, Horde horde)
        {
        super.stop(agent, parent, horde);
        //parent.finished = true;
        }
    }
