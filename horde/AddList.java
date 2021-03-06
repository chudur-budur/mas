package sim.app.horde;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

/**
   ADDLIST

   <p>A JSplitPane which displays candidate elements in the bottom pane
   and lets you add bottom-pane elements into the top pane or delete elements
   from the top pane.  Used by Horde to display available features.

   <p>You can also *include* a list element, meaning copy it to the top pane.
   Typically the user does this on his own but an application might want to
   have a few items in the top pane to start with.

   <p>List elements are specified by ADDLISTCALLBACKs, objects which provide
   all the information about the list element and events it might receive.
*/

public class AddList extends JSplitPane
{
	private static final long serialVersionUID = 1;
	class HolderPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;
		AddListCallback holder;
		JComponent comp;
	}

	class TopCellRenderer implements ListCellRenderer
	{
		public Component getListCellRendererComponent(
		    JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			JComponent c = (topVector.elementAt(index));

			if (isSelected)
			{
				c.setBackground(list.getSelectionBackground());
				c.setForeground(list.getSelectionForeground());
			}
			else
			{
				c.setBackground(list.getBackground());
				c.setForeground(list.getForeground());
			}
			return c;
		}
	};

	JList topList = new JList();
	JList bottomList = new JList();
	Vector<HolderPanel> topVector = new Vector<HolderPanel>();
	Vector<AddListCallback> bottomVector = new Vector<AddListCallback>();

	JButton addButton = new JButton("+");          // plus
	JButton deleteButton = new JButton("\u2212");  // minus

	public AddList()
	{
		super(JSplitPane.VERTICAL_SPLIT, true);
		addButton.putClientProperty( "JButton.buttonType", "textured" );
		addButton.putClientProperty( "JComponent.sizeVariant", "small" );
		addButton.putClientProperty( "JButton.buttonType", "segmented" );
		addButton.putClientProperty( "JButton.segmentPosition", "first" );
		deleteButton.putClientProperty( "JButton.buttonType", "textured" );
		deleteButton.putClientProperty( "JComponent.sizeVariant", "small" );
		deleteButton.putClientProperty( "JButton.buttonType", "segmented" );
		deleteButton.putClientProperty( "JButton.segmentPosition", "last" );
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(1,2));
		p.add(addButton);
		p.add(deleteButton);
		JPanel p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(p, BorderLayout.SOUTH);
		JScrollPane topPane = new JScrollPane(topList);
		p1.add(topPane, BorderLayout.CENTER);
		setTopComponent(p1);
		setBottomComponent(new JScrollPane(bottomList));
		topList.setListData(topVector);
		bottomList.setListData(bottomVector);
		addButton.setSelected(false);
		deleteButton.setSelected(false);
		topList.setCellRenderer(new TopCellRenderer());

		bottomList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				addButton.setSelected(bottomList.getSelectedIndex() != -1);
			}
		});

		topList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				deleteButton.setSelected(topList.getSelectedIndex() != -1);
			}
		});

		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int i = bottomList.getSelectedIndex();
				if (i > -1) includeElement(i);
			}
		});

		deleteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int i = topList.getSelectedIndex();
				if (i > -1) unincludeElement(i);
			}
		});

		topList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				sendEvent(e);
			}
			public void mouseEntered(MouseEvent e)
			{
				sendEvent(e);
			}
			public void mouseExited(MouseEvent e)
			{
				sendEvent(e);
			}
			public void mousePressed(MouseEvent e)
			{
				sendEvent(e);
			}
			public void mouseReleased(MouseEvent e)
			{
				sendEvent(e);
			}
		});

		topList.addMouseMotionListener(new MouseMotionAdapter()
		{
			public void mouseDragged(MouseEvent e)
			{
				sendEvent(e);
			}
			public void mouseMoved(MouseEvent e)
			{
				sendEvent(e);
			}
		});

		bottomList.addMouseListener(new MouseAdapter()
		{
			// listen for double clicks
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					int i = bottomList.getSelectedIndex();
					if (i > -1) includeElement(i);
				}
			}
		});

		topList.setFixedCellWidth(180);
	}

	// redispatches an event to the element in the cell if appropriate.
	// This lets us do things like have buttons in cells which pop up menus.
	void sendEvent(MouseEvent e)
	{
		int i = topList.locationToIndex(e.getPoint());
		if (i < 0) return;
		Point loc = topList.indexToLocation(i);
		HolderPanel hp = (topVector.elementAt(i));
		JComponent c = hp.comp;
		e.setSource(c);
		e.translatePoint(0, -loc.y);
		hp.holder.setComponentLocation(loc);
		c.dispatchEvent(e);
	}


	/** Adds an element to the bottom pane, in the form of an AddListCallback.
	    The component in the callback must be (1) cloneable and (2) have a useful toString() */
	public void addElement(AddListCallback element)
	{
		bottomVector.add(element);
		bottomList.setListData(bottomVector);
	}

	/** Adds an element to the bottom pane in the form of a simple label.  An AddListCallback is constructed
	    automatically for it. */
	public void addElement(final String label)
	{
		addElement(new AddListCallback()
		{
			public void setComponentLocation(Point p) { }
			public JComponent copyElement()
			{
				return new JButton(label);
			}
			public void unincludeElement(JComponent element)
			{
				return ;
			}
			public String toString()
			{
				return label;
			}
		});
	}

	/** Includes an element in the top list.  It's possible for this element to not be
	    in the bottom list already, but it's bad style to do so. */
	public void includeElement(AddListCallback a)
	{
		if (a == null) return;                  // system said no
		JComponent component = a.copyElement();

		HolderPanel p = new HolderPanel();
		p.holder = a;
		p.comp = component;
		p.setLayout(new BorderLayout());
		JLabel l = new JLabel(" ");
		l.setOpaque(false);
		p.add(l, BorderLayout.WEST);

		JPanel p1 = new JPanel();
		p1.setOpaque(false);
		p1.setLayout(new BorderLayout());
		p1.add(component, BorderLayout.WEST);
		JLabel l2 = new JLabel("");
		l2.setOpaque(false);
		p1.add(l2, BorderLayout.CENTER);
		p.add(p1, BorderLayout.CENTER);
		topVector.add(p);
		topList.setListData(topVector);
	}

	// copies an element to the top list
	void includeElement(int index)
	{
		includeElement((bottomVector.elementAt(index)));
	}

	// removes an element from the top list
	void unincludeElement(int index)
	{
		HolderPanel p = (topVector.elementAt(index));
		p.holder.unincludeElement(p.comp);
		topVector.remove(index);
		topList.setListData(topVector);
	}


	/** The component must be (1) cloneable and (2) have a useful toString(). */
	public void clear()
	{
		topVector.clear();
		bottomVector.clear();
	}
}