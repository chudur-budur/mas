package sim.app.horde.objects;

import sim.app.horde.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.agent.SimAgent;

import java.awt.geom.*;
import java.awt.*;
import sim.util.*;
import sim.portrayal.*;
import sim.field.continuous.*;
import java.awt.font.*;

/**
   BODY
        
   <p>A BODY is a shape which can act as a Targetable and display itself in the environment.
   At present the only bodies are OBSTACLEs.
*/


public class Body extends SimplePortrayal2D implements Targetable, Fixed2D
    {
    private static final long serialVersionUID = 1;
    public Double2D loc;
    public Shape shape;
    public int status=0;
    Area area;
    AffineTransform transform = new AffineTransform();
    protected Paint defaultPaint = Color.gray;
    protected Paint paint = null;
    Paint[] borders = new Paint[]{null, Color.black, Color.orange};
    Stroke stroke = new BasicStroke(2.0f);
    Paint border = borders[0];
    protected Continuous2D field;
    String name = "Body";
    boolean movable = true;
    boolean hittable = true;
    
    public void setMovable(boolean val) { movable = val; }
    public boolean isMovable() { return movable; }
    public void setHittable(boolean val) { hittable = val; }
    public boolean isHittable() { return hittable; }
    
    public void setName(String val) { name = val; }
    public String getName() { return name; }
        
    int attachments = 0;
    protected int minimumAttachments = 1;
        
    public void setMinimumAttachments(int val) { minimumAttachments = val; } 
    public int getMinimumAttachments() { return minimumAttachments; }
    public void incrementAttachment() { attachments++; }
    public void decrementAttachment() { attachments--; if (attachments < 0) attachments = 0; }
    public int getAttachments() { return attachments; }

    boolean temporarilyEnough = false;
    public void setBypassAttachments(boolean val) { temporarilyEnough = val; }
    public boolean isBypassAttachments() { return temporarilyEnough; }
    public boolean isEnoughAttachments() { return temporarilyEnough || attachments >= minimumAttachments; }
        
    public void setDefaultPaint(Paint p) 
        {
        if (p == null) p = Color.gray;
        defaultPaint = p; 
        }

    public void setParameterValue(int index)
        {
        if (index >= 0) paint = SimHorde.parameterObjectColor[index];
        else paint = null; 
        }

    public void setTargetRank(Agent agent, Horde horde, int rank) {} 
    public int getTargetRank(Agent agent, Horde horde) { return 0; }

    public String toString() { return name; }

    public int getTargetStatus(Agent agent, Horde horde)
        {
        return this.status;
        }

    public void setTargetStatus(Agent agent, Horde horde, int status)
        {
        this.status=status;
        if (status >= 0 && status < borders.length) 
            border = borders[status];
        //else System.err.println("Status out of bounds for border in Body.java: " + status);
        else if (status >= borders.length)
            border = borders[borders.length - 1];
        else if (status < 0)
            border = borders[0];
        }

    public boolean getTargetIntersects(Agent agent, Horde horde, Double2D location, double slopSquared)
        {
        return area.contains(location.x - loc.x, location.y - loc.y) || 
            getTargetLocation(agent, horde).distanceSq(location) <= slopSquared;
        }

    public Double2D getTargetLocation(Agent agent, Horde horde)
        {
        SimAgent simagent = (SimAgent)agent;
        return sim.app.horde.Utilities.closestPoint(field.getObjectLocation(this), this.shape, simagent.getLocation());
        }
                
    /** Sets relative to the value returned by getTargetLocation(), which is presently the closest point. */
    public void setTargetLocation(Agent agent, Horde horde, Double2D location)
        {
        Double2D curloc = getTargetLocation(agent, horde);
        Double2D diff = location.subtract(curloc);
        loc = loc.add(diff);
        if (field != null) field.setObjectLocation(this, loc);
        }
                
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        transform.setToTranslation(info.draw.x, info.draw.y);
        transform.scale(info.draw.width, info.draw.height);
        Area b = area.createTransformedArea(transform);
        graphics.setPaint(paint == null ? defaultPaint : paint);
        graphics.fill(b);
        if (border!=null)
            {
            Stroke s = graphics.getStroke();
            graphics.setStroke(stroke);
            graphics.setPaint(border);
            graphics.draw(b);
            graphics.setStroke(s);
            }
        }

    public boolean hitObject(Object object, DrawInfo2D range)
        {
//        if (this instanceof CircularBody) 
//              System.err.println(hittable);
        if (!hittable) return false;
        
        transform.setToTranslation(range.draw.x, range.draw.y);
        transform.scale(range.draw.width, range.draw.height);
        Area b = area.createTransformedArea(transform);
        return b.intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
        }

    protected Body()
        {
        setup();        // this is called to allow anonymous subclasses to have constructors (mostly to change the paint)
        }
                
    protected void setup()
        {
        }
    
    public Body(String string, int fontSize, Continuous2D field, Double2D location)
        {
        this(new Font("Serif", 0, fontSize).createGlyphVector(new FontRenderContext(
                    new AffineTransform(),false,true),string).getOutline(),
            field,
            location);
        name = "Body \"" + string + "\"";
        }
        
    public Body(Shape shape, Continuous2D field, Double2D location)
        {
        this.field = field;
        this.shape = shape;
        loc = location;
        area = new Area(shape);
        setup();        // this is called to allow anonymous subclasses to have constructors (mostly to change the paint)
        }
                
    public boolean collision(Double2D p)
        {
        double x = p.x - loc.x;
        double y = p.y - loc.y;
        return area.contains(x,y);
        }               

    public boolean collision(Area a)
        {
        transform.setToTranslation(loc.x, loc.y);
        Area b = area.createTransformedArea(transform);
        b.intersect(a);
        return b.isEmpty();
        }               

    public boolean collision(Shape sh)
        {
        return collision(new Area(sh));
        }    

    public boolean maySetLocation(Object field, Object location)
        {
        if (movable) { loc = (Double2D)location; return true; }
        else return false;
        } 

    // not sure if we need this, since you can't select what you can't hit, 
    // so this method is likely never called
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return hittable;
        }
    }
