package sim.app.horde;

import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.*;
import java.awt.event.*;
import sim.app.horde.agent.Agent;
import java.util.*;

import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;


public abstract class HordeWithUI extends GUIState implements MacroObserver
    {
    private static final long serialVersionUID = 1;

    private HordeWithUI() { super(null); }  // this should never be called.  Instead call super(new Horde(System.currentTimeMillis())) or use SimHorde
    
    public HordeWithUI(SimState state) { super(state); }

    public Object getSimulationInspectedObject()
        {
        return state;
        }  // non-volatile
                
    public static String getName()
        {
        return "The Horde";
        }

    public void resetting()
        {
        buildAddList();  // redo the features window
        buttons.setBehaviors(((Horde) state).getTrainingMacro());  // redo the action button array
        }

    public void start()
        {
        // we want the observer to be set by the time super.start is called
        // so that when the training agent is set for the first time, the
        // observer is properly called to inform it of this.
        ((Horde) state).observer = this;

        // NOW call super.start
        super.start();
        }

    public Inspector getInspector() 
        { 
        Inspector i = super.getInspector(); 
        i.setVolatile(true);
        return i;
        }
        
    public ButtonArray buttons;
    public AddList addlist;

    public void init(final sim.display.Controller c)
        {
        super.init(c);
                
        // make the addlist
        addlist = new AddList();

        ((Console)c).getTabPane().addTab("Features", addlist);
        ((Console)c).getTabPane().removeTabAt(0);  // About
        ((Console)c).getTabPane().removeTabAt(1);  // Console, after About has been removed
        ((Console)c).getTabPane().addTab("Actions", buttons = new ButtonArray(this));
        }

    /***** Code for setting up the Behavior List ******/

    public JPopupMenu createTargetMenu(final TrainableMacro macro, final Targeting targeting)
        {
        final Target[] arguments = targeting.getTargets();                      // ground targets or wrappers from the behavior
        final String[] argumentNames = targeting.getTargetNames();
        final Target[] targets = Target.provideAllTargets(macro);                   // targets they are allowed to be (in wrapper form or ground)
        // return null if no arguments used
        boolean empty = true;
        for(int i = 0; i < arguments.length; i++) 
            if (arguments[i] != null) 
                {
                empty = false; 
                break; 
                }
            else System.err.println("NULL TARGET " + i + " " + targeting + " this should not happen!");
        if (empty) return null;
        
        // build a menu
        final JPopupMenu targetMenu = new JPopupMenu();
        for(int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                {
                final JMenu menu = new JMenu(argumentNames[i]);
                ButtonGroup group = new ButtonGroup();
                // this holds the index of the most recently selected menu option
                final int[] selected = new int[] { -1 };  // this better change!
                for(int j = 0; j < targets.length; j++)
                    {
                    final JRadioButtonMenuItem jmi = new JRadioButtonMenuItem("Assign to " + targets[j]);
                    group.add(jmi);
                                        
                    // we need to identify which target is currently being used.
                    if (arguments[i] instanceof Wrapper && targets[j] instanceof Parameter )
                        {
                        if (((Wrapper)(arguments[i])).isTargeting(macro, (Parameter)(targets[j])))
                            {
                            jmi.setSelected(true);
                            selected[0] = j;
                            }
                            
                        }
                    else    // next check if it's a base class.  Let's compare classes
                        {
                        if (arguments[i].getClass() == targets[j].getClass())  // it's the one!!!
                            {
                            jmi.setSelected(true);
                            selected[0] = j;
                            }
                        }
                                        
                    final int _i = i;
                    final int _j = j;
                    final ButtonGroup _group = group;
                    // now add the action listener
                    jmi.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            synchronized(state.schedule)
                                {
                                final int RESULT_RESET = 0;
                                final int RESULT_DONT_RESET = 1;
                                final int RESULT_CANCEL = 2;
                                int result = RESULT_DONT_RESET;  // the default, assuming the num examples is > 0
                                
                                // reset the model and examples and distribute to all agents
                                Horde horde = (Horde)state;
                                if (macro != horde.getTrainingMacro())  // a quick double check
                                    throw new RuntimeException("Targets changed on non training agent.  This shouldn't be possible.");

                                if (macro.getNumExamples() > 0)
                                    {
                                    Object[] options = { "Reset", "Don't Reset", "Cancel" };
                                    result = JOptionPane.showOptionDialog(null,
                                        "You should probably reset the examples if you change a target",
                                        "Examples Should Be Reset",
                                        JOptionPane.DEFAULT_OPTION,
                                        JOptionPane.WARNING_MESSAGE,
                                        null, options, options[0]);
                                    }
                                                                        
                                if (result == RESULT_CANCEL)
                                    {
                                    if (_j == -1) System.err.println("Menu was -1.  This should never happen.");
                                    else menu.getItem(selected[0]).setSelected(true);
                                    return;
                                    }

                                jmi.setSelected(true);
                                if (targets[_j] instanceof Parameter)
                                    {
                                    Parameter p = (Parameter)targets[_j];
                                    Wrapper w = new Wrapper(p.getName(), p.getIndex());
                                    targeting.setTarget(_i, w);  // make a wrapper for the Parameter
                                    selected[0] = _j;
                                    }
                                else
                                    {
                                    targeting.setTarget(_i, targets[_j]);  // it's just a ground target, don't make a wrapper for it
                                    selected[0] = _j;
                                    }
                                
                                // reset the model and examples and distribute to all agents
                                if (macro != horde.getTrainingMacro())  // a quick double check
                                    throw new RuntimeException("Targets changed on non training agent.  This shouldn't be possible.");
                                macro.reset(horde, macro.behaviors, macro.features);
                                horde.distributeAndRestartBehaviors();
                                }
                            }
                        });
                                        
                    menu.add(jmi);
                    }
                targetMenu.add(menu);
                }
        return targetMenu;
        }



    public AddListCallback buildAddListElement(final Feature _feature)
        {
        return new AddListCallback()
            {
            HashMap features = new HashMap();  // a hashmap so we can have more than one feature in the top list
            Point loc = null;
                                
            public void setComponentLocation(Point loc) { this.loc = loc; }
            public String toString() { return "" + _feature.getName(); }
            public void unincludeElement(JComponent element)
                {
                Horde horde = (Horde)state;
                Feature feature = (Feature)(features.get(element));
                if (feature!=null) horde.removeCurrentFeature(feature);
                else throw new RuntimeException("Unknown feature removed.  This shouldn't happen");
                }
            public JComponent promoteElement(boolean user)
                {
                Horde horde = (Horde)state;
                Feature feature = _feature;
                if (user) feature = (Feature)(feature.clone());
                final JButton b = new JButton(feature.getName());
                final JPopupMenu menu = createTargetMenu(horde.getTrainingMacro(), feature);
                b.addActionListener(new ActionListener() 
                    { public void actionPerformed(ActionEvent e) { if (menu!= null) menu.show(addlist, loc.x + b.getWidth(), loc.y); } });

                        MouseListener ml = new MouseListener()
                            {
                            public void mouseClicked(MouseEvent arg0)
                                {
                                if ((arg0.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
                                    {
                                    SimpleInspector simpleInspector = new SimpleInspector(_feature, HordeWithUI.this, null, HordeWithUI.this.getMaximumPropertiesForInspector());
                                    final Stoppable stopper = simpleInspector.reviseStopper(
                                        HordeWithUI.this.scheduleRepeatingImmediatelyAfter(simpleInspector.getUpdateSteppable()));
                                    HordeWithUI.this.controller.registerInspector(simpleInspector, stopper);
                                    JFrame frame = simpleInspector.createFrame(stopper);
                                    frame.setVisible(true);
                                    }
                                }

                            public void mouseEntered(MouseEvent arg0) { }
                            public void mouseExited(MouseEvent arg0) { }
                            public void mousePressed(MouseEvent arg0) { }
                            public void mouseReleased(MouseEvent arg0) { }
                            };

                        b.addMouseListener(ml);


                horde.addCurrentFeature(feature, user);
                features.put(b, feature);

                return b;
                }
            };
        }


    
    public void buildAddList()
        {
        Horde horde = (Horde)state;
        
        addlist.clear(); 

        Agent trainingAgent = horde.getTrainingAgent();
        
        // add the addable features
        Feature[] f = Feature.provideAllFeatures(trainingAgent);
        for(int i = 0; i < f.length; i++)
            addlist.addElement(buildAddListElement(f[i]));

        // add the existing features
        f = trainingAgent.getBehavior().features;
        for(int i = 0; i < f.length; i++)
            addlist.includeElement(buildAddListElement(f[i]), false);
        }




    /** Called when the agent's macro has decided to transition from FROM to TO.
        We then update the current hilighted behavior button as a result. 
        A method defined by MacroObserver. */
    public void transitioned(Macro macro, final int from, final int to)
        {
        Horde horde = (Horde) (state);

        if (macro == horde.getTrainingMacro()) // it's the relevant one, don't listen to the others
            {
            if (from != to)
                {
                SwingUtilities.invokeLater(new Runnable()
                    {
                    public void run() { synchronized(state.schedule) { buttons.hilightCurrentBehavior(to); } }
                    });
                }
            }
        }

    /** Called when the training agent has been changed.  A method defined by MacroObserver. */
    public void trainingAgentChanged()
        {
        buildAddList();  // redo the features window
        buttons.setBehaviors(((Horde)state).getTrainingMacro());  // redo the action button array
        }

    }
