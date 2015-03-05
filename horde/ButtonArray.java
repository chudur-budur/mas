package sim.app.horde;
import sim.app.horde.behaviors.*;
import sim.app.horde.classifiers.Domain;
import sim.app.horde.classifiers.Example;
import sim.app.horde.features.Feature;
import sim.app.horde.transitions.LearnedTransition;
import sim.util.gui.*;
import sim.display.*;
import sim.engine.Stoppable;
import sim.portrayal.*;

import javax.swing.table.*;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * BUTTONARRAY
 * <p>Maintains the array of buttons at the bottom of Horde, including the scroll
 * view with the various behavior buttons installed,
 *
 * <p>BUTTONARRAY is a MACROOBSERVER so it knows when to change the hilight
 * of various behavior buttons as the Macro behavior transitions from button to button.
 */

public class ButtonArray extends JPanel implements MacroObserver
{
	public JButton makeLittleButton(String text)
	{
		JButton button = null ;
		String[] lines = text.split("\\n");
		if(lines.length > 1)
		{
			button = new JButton() ;
			Box box = new Box(BoxLayout.Y_AXIS);
			for(int i = 0 ; i < lines.length ; i++)
			{
				JLabel label = new JLabel(lines[i]);
				label.setAlignmentX(Component.CENTER_ALIGNMENT);
				box.add(label);
			}
			box.add(Box.createGlue());
			button.add(box, BorderLayout.NORTH);
		}
		else
			button = new JButton(text);
		// button.putClientProperty( "JComponent.sizeVariant", "small" );  // or "mini" ?
		button.putClientProperty( "JButton.buttonType", "bevel" );  // or "mini" ?
		return button;
	}

	private static final long serialVersionUID = 1;

	// the currently highlighted button (duh)
	int hilightedButton = -1;

	// various widgets
	ScrollableFlowPanel subarray = new ScrollableFlowPanel();
	JCheckBox recordingChkBx = new JCheckBox("Recording", true);
	JCheckBox manualChkBx = new JCheckBox("Manual\nDrive", false);
	JCheckBox irlChkBx = new JCheckBox("IRL Mode");
	// recording.putClientProperty("JComponent.sizeVariant", "mini");
	JButton pauseBtn = makeLittleButton("Pause / Unpause\nSimulation");
	JButton saveBtn = makeLittleButton("Save\nModel");
	JButton classifierBtn = makeLittleButton("Show\nModel");
	JButton resetBtn = makeLittleButton("Reset\nSimulation");
	JCheckBox snapshotChkBx = new JCheckBox("Save Snapshots", false);
	//	JButton undoBtn = makeLittleButton("Undo\nAction");
	//	JButton editExamplesBtn = makeLittleButton("Edit Stored\nExamples");
	//	JTextField saveField = new JTextField(20);
	//	JTextField keyField = new JTextField(1);
	//	JTextField levelField = new JTextField("0", 2);
	//	JButton logBtn = makeLittleButton("Save\nLog");
	GUIState guistate;

	public ButtonArray(final GUIState guistate)
	{
		this.guistate = guistate;
		setLayout(new BorderLayout());
		setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		Box box = new Box(BoxLayout.X_AXIS);

		box.add(snapshotChkBx);
		box.add(classifierBtn);
		box.add(saveBtn);
		box.add(recordingChkBx);
		box.add(manualChkBx);
		box.add(irlChkBx);
		box.add(pauseBtn);
		box.add(resetBtn);
		//box.add(undoBtn);
		//box.add(editExamplesBtn);
		//box.add(logBtn);

		box.add(Box.createGlue());
		add(box, BorderLayout.NORTH);

		JScrollPane pane = new JScrollPane(subarray, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(pane, BorderLayout.CENTER);

		// log button
		/*ActionListener logBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					doLog((Horde)(guistate.state));
				}
			}
		};
		logBtn.addActionListener(logBtnListener);*/

		// show model button
		ActionListener classifierBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					((Horde)(guistate.state)).showClassifiers();
				}
			}
		};
		classifierBtn.addActionListener(classifierBtnListener);

		// save model button
		ActionListener saveBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					doSave(((Horde)(guistate.state)));
				}
			}
		};
		saveBtn.addActionListener(saveBtnListener);

		// pasue simulation button
		ActionListener pauseBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				((Console)(guistate.controller)).pressPause();
			}
		};
		pauseBtn.addActionListener(pauseBtnListener);
		pauseBtn.registerKeyboardAction(pauseBtnListener, KeyStroke.getKeyStroke('\n'),
		                                JComponent.WHEN_IN_FOCUSED_WINDOW);

		// reset simulation
		ActionListener resetBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					((ImplementsHordeUI)guistate).reset();
					if(snapshotChkBx.isEnabled())
						((Horde)(guistate.state)).setSaveSnapshots(
						    snapshotChkBx.isSelected());
					if(recordingChkBx.isEnabled())
						((Horde)(guistate.state)).setRecording(
						    recordingChkBx.isSelected());
					if(manualChkBx.isSelected())
					{
						recordingChkBx.setEnabled(false);
						snapshotChkBx.setEnabled(false);
						saveBtn.setEnabled(false);
						classifierBtn.setEnabled(false);
						((Horde)(guistate.state)).setManualDriving(true);
						// after this point, HiTAB is off
					}
					else
					{
						((Horde)(guistate.state)).setManualDriving(false);
						snapshotChkBx.setEnabled(true);
						recordingChkBx.setEnabled(true);
						saveBtn.setEnabled(true);
						classifierBtn.setEnabled(true);
					}
					if(irlChkBx.isSelected())
					{
						recordingChkBx.setEnabled(false);
						snapshotChkBx.setEnabled(false);
						saveBtn.setEnabled(false);
						classifierBtn.setEnabled(false);
						// after this point, HiTAB is off
						((Horde)(guistate.state)).setIrlMode(true);
					}
					else
					{
						((Horde)(guistate.state)).setIrlMode(false);
						snapshotChkBx.setEnabled(true);
						recordingChkBx.setEnabled(true);
						saveBtn.setEnabled(true);
						classifierBtn.setEnabled(true);
					}
				}
			}
		};
		resetBtn.addActionListener(resetBtnListener);

		// undo action button
		/*ActionListener undoBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					doUndo((Horde)guistate.state) ;
				}
			}
		};
		undoBtn.addActionListener(undoBtnListener);*/

		// edit examples button
		/*ActionListener editExamplesBtnListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					doEditExamples((Horde)guistate.state) ;
				}
			}
		};
		editExamplesBtn.addActionListener(editExamplesBtnListener);*/

		// manual drive checkbox
		ActionListener manualChkBxListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					if(manualChkBx.isSelected())
					{
						recordingChkBx.setEnabled(false);
						snapshotChkBx.setEnabled(false);
						saveBtn.setEnabled(false);
						classifierBtn.setEnabled(false);
						((Horde)(guistate.state)).setManualDriving(true);
						// after this point, HiTAB is off
					}
					else
					{
						((Horde)(guistate.state)).setManualDriving(false);
						snapshotChkBx.setEnabled(true);
						recordingChkBx.setEnabled(true);
						saveBtn.setEnabled(true);
						classifierBtn.setEnabled(true);
					}
				}
			}
		};
		manualChkBx.addActionListener(manualChkBxListener);

		// Inverse RL checkbox
		ActionListener irlChkBxListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					if(irlChkBx.isSelected())
					{
						recordingChkBx.setEnabled(false);
						snapshotChkBx.setEnabled(false);
						saveBtn.setEnabled(false);
						classifierBtn.setEnabled(false);
						// after this point, HiTAB is off
						((Horde)(guistate.state)).setIrlMode(true);
					}
					else
					{
						((Horde)(guistate.state)).setIrlMode(false);
						snapshotChkBx.setEnabled(true);
						recordingChkBx.setEnabled(true);
						saveBtn.setEnabled(true);
						classifierBtn.setEnabled(true);
					}
				}
			}
		};
		irlChkBx.addActionListener(irlChkBxListener);

		// save snapshot checkbox
		ActionListener snapshotChkBxListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					if(snapshotChkBx.isEnabled())
						((Horde)(guistate.state)).setSaveSnapshots(
						    snapshotChkBx.isSelected());
				}
			}
		};
		snapshotChkBx.addActionListener(snapshotChkBxListener);

		// add action listeners for the recordingChkBx button
		ActionListener recordingChkBxListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					if(recordingChkBx.isEnabled())
						((Horde)(guistate.state)).setRecording(
						    recordingChkBx.isSelected());
				}
			}
		};
		recordingChkBx.addActionListener(recordingChkBxListener);

		// this is the opposite because when the keystroke
		// is pressed the button state hasn't changed
		ActionListener recordingChkBxListener2 = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(guistate.state.schedule)
				{
					if(recordingChkBx.isEnabled())
					{
						recordingChkBx.setSelected(!recordingChkBx.isSelected());
						((Horde)(guistate.state)).setRecording(
						    recordingChkBx.isSelected());
					}
				}
			}
		};
		recordingChkBx.registerKeyboardAction(recordingChkBxListener2, KeyStroke.getKeyStroke('z'),
		                                      JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Called when the agent's macro has decided to transition from FROM to TO;
	 * We then update the current hilighted behavior button as a result,
	 * A method defined by MacroObserver.
	 */
	public void transitioned(Macro macro, final int from, final int to)
	{
		Horde horde = (Horde)(guistate.state);
		// it's the relevant one, don't listen to the others
		if (macro == horde.getTrainingMacro())
		{
			if (from != to)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						synchronized(guistate.state.schedule)
						{
							hilightCurrentBehavior(to);
						}
					}
				});
			}
		}
	}

	/**
	 * Called when user pressed "Edit Examples",
	 * provides a JFrame allowing direct editing of ALL the examples.
	 */
	class ExampleTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		ArrayList<String> columnNames = new ArrayList<String>();
		ArrayList<ArrayList<Object>> data = new ArrayList< ArrayList<Object>>();

		public int getColumnCount()
		{
			return columnNames.size();
		}

		public int getRowCount()
		{
			return data.size();
		}

		public String getColumnName(int idx)
		{
			return columnNames.get(idx);
		}

		public Class<?> getColumnClass(int c)
		{
			return getValueAt(0, c).getClass();
		}

		public boolean isCellEditable(int row, int col)
		{
			if (row > 0)
				return true;
			return false;
		}

		public Object getValueAt(int row, int col)
		{

			if (col > columnNames.size())
				return new String("Col is too big!");

			if (row > data.size())
				return new String("Row is too big!");

			return data.get(row).get(col);
		}

		public void addColumnName(String str)
		{
			columnNames.add(str);
		}

		public void setValueAt(Object value, int row, int col)
		{
			data.get(row).add(col, value);
		}

		public void addExample(Example e, Domain d, String previousClassification)
		{
			ArrayList<Object> row = new ArrayList<Object>();

			row.add(previousClassification);

			for(int i = 0; i < e.values.length; i++)
				row.add(new Double(e.values[i]));

			row.add(new String(d.classes[e.classification]));

			data.add(row);

			String result= (e.continuation ? "true" : "false");
			row.add(result);
		}

	};

	public void doEditExamples(Horde horde)
	{
		ExampleTableModel etm = new ExampleTableModel();

		TrainableMacro tm = horde.getTrainingMacro();

		for (int i=0; i < tm.transitions.length; i++)
		{
			if (tm.transitions[i] == null) continue;
			LearnedTransition lt = ((LearnedTransition) tm.transitions[i]);

			if (i == 0)
			{
				etm.addColumnName("Original Behavior");
				Feature [] features = lt.getFeatures();
				for (int k=0; k < features.length; k++)
					etm.addColumnName(features[k].toString());
				etm.addColumnName("New Behavior");
				etm.addColumnName("Continuation");
			}

			ArrayList<Example> examples = lt.getExamples();
			for (int j=0; j < examples.size(); j++)
			{
				etm.addExample(examples.get(j), lt.getDomain(), tm.behaviors[i].getName());
			}
		}

		System.out.println(etm.data.size());

		JTable table = new JTable(etm);

		//table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);
		JScrollPane scroller = new JScrollPane(table);

		//JScrollPane scroller = new JScrollPane(table,
		//			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		//			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//scroller.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

		JFrame frame = new JFrame();
		frame.setTitle("Example Database");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(scroller, BorderLayout.CENTER);
		frame.setResizable(true);
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Called when the user pressed "Undo";
	 * We delete the last example from the example database.
	 */
	public void doUndo(Horde horde)
	{
		TrainableMacro tm = horde.getTrainingMacro();
		tm.removeLastExample();
	}

	/**
	 * Called when the user pressed "Log",
	 * We save out a log file of the exemplars and domain.
	 */
	public void doLog(Horde horde)
	{
		FileDialog fd = new FileDialog((Console)(guistate.controller),
		                               "Save Log...", FileDialog.SAVE);
		fd.setFile("Put the log name here not the filename");
		fd.setVisible(true);
		if (fd.getFile()!=null)
			horde.getTrainingMacro().logExemplars(fd.getDirectory(), fd.getFile(), horde);
	}

	/**
	 * Called when the user pressed "Save",
	 * We save out the current behavior and its classifiers.
	 */
	public void doSave(Horde horde)
	{
		// init the dialog panel
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		final JTextField saveField = new JTextField(20);
		JTextField keyField = new JTextField(2);
		JTextField levelField = new JTextField("0", 2);

		Box b = new Box(BoxLayout.X_AXIS);

		JPanel panel = new JPanel();
		panel.setBorder(new javax.swing.border.TitledBorder("Name"));
		panel.add(saveField, BorderLayout.CENTER);
		b.add(panel);

		JPanel panel2 = new JPanel();
		panel2.setBorder(new javax.swing.border.TitledBorder("Key"));
		panel2.add(keyField, BorderLayout.CENTER);
		b.add(panel2);

		JPanel panel3 = new JPanel();
		panel3.setBorder(new javax.swing.border.TitledBorder("Level"));
		panel3.add(levelField, BorderLayout.CENTER);
		b.add(panel3);

		b.add(Box.createGlue());
		p.add(b, BorderLayout.CENTER);

		if (JOptionPane.showConfirmDialog(null, p, "Save Behavior...",
		                                  JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

		String name = saveField.getText().trim();
		if (name.equals(""))
		{
			JOptionPane.showMessageDialog(null, "The name you provided is empty.",
			                              "Cannot Save", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String[] s = horde.provideAllSavedMacroNames();
		for(int i =0; i < s.length; i++)
		{
			if (s[i].equalsIgnoreCase(name))
			{
				if (JOptionPane.showConfirmDialog(null, "The name you have chosen (" + name
				                                  + ") already exists."
				                                  + "Do you wish to overwrite it?",
				                                  "Overwrite Existing Macro?",
				                                  JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
					return;
				else break;  // no more comparisons
			}
		}

		String keystring = "" + keyField.getText();
		char ch = 0;
		if (keystring.length() > 0) ch = keystring.charAt(0);

		int level = Integer.valueOf(levelField.getText().trim());

		// save it!!!
		horde.save(name, KeyStroke.getKeyStroke(ch), level);
	}

	/**
	 * Returns the given button in the ButtonArray.
	 */
	JButton getButton(int val)
	{
		return (JButton)(((Box)(subarray.getComponent(val))).getComponent(0));
	}

	/**
	 * Hilights the given button and de-hlights the others,
	 * Sets the current hilightedButton to this button.
	 */
	public void hilightCurrentBehavior(int val)
	{
		try
		{
			if (hilightedButton >= 0) getButton(hilightedButton).setSelected(false);
			hilightedButton = -1;
			int i = subarray.getComponentCount();
			if (val < 0 || val >= i) return;  // bah -- probably UNKNOWN_BEHAVIOR

			getButton(val).setSelected(true);
		}
		catch (Exception e) { }
		hilightedButton = val;
	}

	/** Called when the window is closed.  Presently does nothing. */
	public void quit()
	{
		System.err.println("--- ButtonArray::quit()");
	}

	/**
	 * Called to change the behaviors to those in the given Macro;
	 * Updates all the buttons, adds new menus and listeners to them,
	 * and updates the recordingChkBx button's listeners.
	 */
	public void setBasicBehaviorButtons(final Macro macro)
	{
		synchronized(guistate.state.schedule)
		{
			subarray.removeAll();
			if (macro != null)
			{
				for(int bindex = 0; bindex < macro.behaviors.length; bindex++)
				{
					Box box = new Box(BoxLayout.X_AXIS);
					final int behaviorIndex = bindex;
					char ksc = getKeyStrokeChar(macro.behaviors[bindex].getKeyStroke());
					String bn = macro.behaviors[bindex].getButtonName();
					System.err.println("--- ButtonArray.setBasicBehaviorButtins() :"
					                   + " setting button "
					                   + ksc + "/" + bn);
					JButton button = new JButton((ksc == 0 ? "" : ksc + " ") + bn);
					ActionListener listener = new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							synchronized(guistate.state.schedule)
							{
								Horde horde = (Horde)(guistate.state);
								if (recordingChkBx.isEnabled() &&
								        !recordingChkBx.isSelected())
								{
									recordingChkBx.setSelected(true);
									horde.setRecording(
									    recordingChkBx.isSelected());
								}
								horde.getTrainingMacro().
								userChangedBehavior(
								    (Horde)(guistate.state), behaviorIndex);
							}
						}
					};

					if (macro.behaviors[bindex] instanceof Start)  // Start is special!
					{
						listener = new ActionListener()
						{
							public void actionPerformed(ActionEvent e)
							{
								synchronized(guistate.state.schedule)
								{
									Horde horde = (Horde)(guistate.state);
									if (recordingChkBx.isEnabled() &&
									        !recordingChkBx.isSelected())
									{
										recordingChkBx.setSelected(true);
										horde.setRecording(
										    recordingChkBx.isSelected());
									}
									horde.getTrainingMacro().stop(
									    horde.getTrainingAgent(),
									    null, horde);
									horde.getTrainingMacro().start(
									    horde.getTrainingAgent(),
									    null, horde);
								}
							}
						};
					}

					// if we have a Macro, respond to a right mouse click
					// by showing the Macro's Inspector
					if (macro.behaviors[bindex] instanceof Macro)
					{
						final int behaviourIndex = bindex;
						MouseListener ml = new MouseListener()
						{
							public void mouseClicked(MouseEvent arg0)
							{
								if ((arg0.getModifiers() & InputEvent.BUTTON3_MASK) ==
								        InputEvent.BUTTON3_MASK)
								{
									Inspector simpleInspector = new SimpleInspector(
									    macro.behaviors[behaviorIndex],
									    guistate, null,
									    guistate.getMaximumPropertiesForInspector());
									final Stoppable stopper = simpleInspector.reviseStopper(
									                              guistate.scheduleRepeatingImmediatelyAfter(
									                                  simpleInspector.getUpdateSteppable()));
									guistate.controller.registerInspector(
									    simpleInspector,stopper);
									JFrame frame = simpleInspector.createFrame(stopper);
									frame.setVisible(true);
								}
							}

							public void mouseEntered(MouseEvent arg0)
							{
							}

							public void mouseExited(MouseEvent arg0)
							{
							}

							public void mousePressed(MouseEvent arg0)
							{
							}

							public void mouseReleased(MouseEvent arg0)
							{
							}
						};
						button.addMouseListener(ml);
					}


					button.addActionListener(listener);
					button.setIcon(NumberTextField.I_BELLY);
					button.setSelectedIcon(NumberTextField.I_BELLY_PRESSED);

					// this is an obsolete method but I don't care
					// -- ActionMap is a pain to figure out, this is much easier
					if (macro.behaviors[bindex].getKeyStroke() != null)
						button.registerKeyboardAction(listener,
						                              macro.behaviors[bindex].getKeyStroke(),
						                              JComponent.WHEN_IN_FOCUSED_WINDOW);
					box.add(button);

					if (macro instanceof TrainableMacro)  // it always is
					{
						TrainableMacro tm = (TrainableMacro) macro;
						//Horde horde = (Horde)(guistate.state);
						final JPopupMenu menu = ((ImplementsHordeUI)guistate).createTargetMenu(
						                            tm, tm.behaviors[bindex]);
						if (menu != null)  // need to add it
						{
							JToggleButton togglebutton = new JToggleButton(Display2D.LAYERS_ICON);
							togglebutton.setPressedIcon(Display2D.LAYERS_ICON_P);
							togglebutton.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
							togglebutton.setBorderPainted(false);
							togglebutton.setContentAreaFilled(false);
							final JToggleButton b = togglebutton;

							b.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									menu.show(b, b.getWidth(), 0);
								}
							});
							box.add(b);
						}
					}
					subarray.add(box);
				}
			}
			revalidate();
		}
	}

	/**
	 * Returns the key stroke character if there is one, else returns 0x0,
	 * This is used to pretty-print up/down/left/right arrows on the buttons, that's all.
	 */
	public static char getKeyStrokeChar(KeyStroke stroke)
	{
		if (stroke == null) return (char) 0;
		else if (stroke == Behavior.KS_UP) return '\u2191';
		else if (stroke == Behavior.KS_DOWN) return '\u2193';
		else if (stroke == Behavior.KS_LEFT) return '\u2190';
		else if (stroke == Behavior.KS_RIGHT) return '\u2192';
		else return stroke.getKeyChar();
	}
}
