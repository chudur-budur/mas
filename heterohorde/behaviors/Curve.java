package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.agent.Agent;

public class Curve extends Behavior
    {
    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }

    protected Forward forward;
    protected Rotate rotate;

    public Object clone()
        {
        Curve f = (Curve)(super.clone());
        f.forward = forward == null ? null : (Forward)(forward.clone());
        f.rotate = rotate == null ? null : (Rotate)(rotate.clone());
        return f;
        }

        
    public Curve()
        {
        this(0.1, 0.2);
        } 

    public Curve(double forwardSpeed, double rotationalRate)
        {
        forward = new Forward(forwardSpeed);
        rotate = new Rotate(rotationalRate);
        name = "Curve";
        } 

    public void go(Agent agent, Macro parent, Horde horde)
        {
        super.go(agent, parent, horde);
        forward.go(agent, parent, horde);
        rotate.go(agent, parent, horde);

        }
    }
