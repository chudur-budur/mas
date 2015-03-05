package sim.app.horde.behaviors;
import sim.app.horde.objects.*;
import sim.app.horde.*;
import sim.util.*;

public class PlaceMarker extends Behavior
{
	private static final long serialVersionUID = 1;
	public PlaceMarker()
	{
		name = "PlaceMarker";    // good default
		setKeyStroke('p');
	}
	int countX = 0;

	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent,parent,horde);

		SimAgent simagent = (SimAgent) agent;

		Double2D loc = simagent.getLocation();
		Marker placedMarker = new Marker("Marker " + countX++);
		((SimHorde)horde).markers.setObjectLocation(placedMarker, loc);
	}
}
