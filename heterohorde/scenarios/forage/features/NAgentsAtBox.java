package sim.app.horde.scenarios.forage.features;

import java.util.List;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.Feature;
import sim.app.horde.scenarios.forage.agent.Forager;
import sim.app.horde.scenarios.forage.agent.Supervisor;
import sim.app.horde.targets.Target;

import com.sun.xml.internal.ws.util.StringUtils;

public class NAgentsAtBox extends Feature
    {
    private static final long serialVersionUID = 1L;

    int N;
    
    public static final String getType() { return "supervisor"; }
        
    public NAgentsAtBox() { this(10); }
    
    public NAgentsAtBox(String [] params) 
        { 
        this (Integer.parseInt(params[0]));
        }
        
    public NAgentsAtBox(int n)
        {
        super("NAgentsAtBox("+n+")");
        N = n ;
        targets = new Target[0];
        targetNames = new String[0];
        }

    public double getValue(Agent agent, Macro parent, Horde horde)
        {
        int cnt=0;
        Supervisor s = (Supervisor)agent; 
        List<Agent> foragers = null; //s.getSubsidiaryAgents("Forager");
        for (int i =0; i < foragers.size(); i++) { 
            if (((Forager)foragers.get(i)).getStatus() == Forager.ATTACHED_STATUS) { 
                cnt++; 
                }
            }
                
        if (cnt >= N) return 1;
        return 0; 
        }
    }


