package sim.app.horde;

import javax.swing.*;
import sim.app.horde.behaviors.*;

/**
 * This is implemented by HordeWithUI
 */

public interface ImplementsHordeUI
{
	// stuff used by ButtonArray
	public void reset();
	public JPopupMenu createTargetMenu(final TrainableMacro macro, final Targeting targeting);
}
