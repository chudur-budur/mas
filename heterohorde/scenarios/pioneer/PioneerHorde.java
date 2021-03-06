package sim.app.horde.scenarios.pioneer;

import java.util.ArrayList;

import sim.app.horde.agent.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.behaviors.MacroObserver;
import sim.app.horde.behaviors.TrainableMacro;

public class PioneerHorde extends Horde implements MacroObserver
    {
    private static final long serialVersionUID = 1L;

    public ArrayList<PioneerAgent> agents;

    // String robotIPAddress[] = { "10.0.0.102", "10.0.0.109", "10.0.0.103" };
    // String robotIPAddress[] = { "10.0.0.102", "10.0.0.109" };
    String robotIPAddress[] = { "10.0.0.102", "10.0.0.103", "10.0.0.109", "10.0.0.107" };

    // String robotIPAddress[] = { "10.0.0.103" };

    public void setBasicBehaviorLocation(String s)
        {
        BASIC_BEHAVIORS_LOCATION = s;
        }

    public void setBasicTargetLocation(String s)
        {
        BASIC_TARGETS_LOCATION = s;
        }

    public void setBasicFeatureLocation(String s)
        {
        BASIC_FEATURES_LOCATION = s;
        }

    public void setTrainableMacroDirectory(String s)
        {
        TRAINABLE_MACRO_DIRECTORY = s;
        }

    public PioneerHorde() {
        super(System.currentTimeMillis());

        setBasicBehaviorLocation("scenarios/pioneer/pioneer.behaviors");
        setBasicTargetLocation("scenarios/pioneer/pioneer.targets");
        setBasicFeatureLocation("scenarios/pioneer/pioneer.features");
        setTrainableMacroDirectory("/tmp/");
        // setTrainableMacroDirectory(HumanoidHorde.getPathInDirectory("trained"));

        agents = new ArrayList<PioneerAgent>();

        for (int i = 0; i < robotIPAddress.length; i++)
            {
            System.out.println("Connecting to: " + robotIPAddress[i]);
            PioneerAgent a = new PioneerAgent(this, robotIPAddress[i]);

            if (i == 0) // arbitrarily pick the first agent as the training
                // agent.
                trainingAgent = a;
            agents.add(a);
            }
        resetBehavior();
        }

    public void transitioned(Macro macro, int from, int to)
        {
        }

    public void distributeAndRestartBehaviors()
        {
        for (int i = 0; i < agents.size(); i++)
            {
            Agent agent = agents.get(i);

            // if I'm not the training agent, steal from him
            if (agent != trainingAgent && trainingAgent != null && trainingAgent.getBehavior() != null)
                agent.setBehavior((TrainableMacro) (trainingAgent.getBehavior().clone()));

            // now restart
            agent.restart(this);
            }
        }

    }
