package aQute.gui.layout;

import static java.lang.Math.*;

import java.awt.*;
import java.util.*;

import javax.swing.*;

public class Stramien implements LayoutManager {
	final Container	container;
	final int		gutter, width, interline, height, borderTop, borderLeft;

	static class Entry {
		int			x, y, w, h;
		Component	component;
	}

	final ArrayList<Entry>	components	= new ArrayList<Entry>();

	public Stramien(Container c, int gutter, int width, int interline,
			int height, int borderLeft, int borderTop ) {
		this.container = c;
		this.gutter = gutter;
		this.width = width;
		this.interline = interline;
		this.height = height;
		this.borderTop = borderTop;
		this.borderLeft = borderLeft;
		c.setLayout(this);
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		throw new UnsupportedOperationException("Use the layout manager to add");
	}

	@Override
	public void layoutContainer(Container parent) {
		int cols = (parent.getWidth() - gutter) / (width + gutter);
		int rows = (parent.getHeight() - interline) / (height + interline);
		for (Entry entry : components) {
			int x, y, w, h;
			if (entry.x >= 0)
				x = gutter + entry.x * (gutter + width);
			else
				x = gutter + (cols + entry.x) * (gutter + width);

			if (entry.y >= 0)
				y = interline + entry.y * (interline + height);
			else
				y = interline + (rows + entry.y) * (interline + height);

			if (entry.w > 0)
				w = entry.w * (gutter + width) - gutter;
			else
				w = ((cols + entry.w) - entry.x) * (gutter + width) - gutter;

			if (entry.h > 0)
				h = entry.h * (interline + height) - interline;
			else
				h = ((rows + entry.h) - entry.y) * (interline + height) - interline;

			entry.component.setBounds(x, y, w, h);
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		int maxX = 0;
		int maxY = 0;
		int minX = 0;
		int minY = 0;

		for (Entry entry : components) {
			maxX = max(maxX, entry.x);
			minX = min(minX, entry.x);
			maxY = max(maxY, entry.y);
			minY = min(minY, entry.y);
		}

		int w = gutter + (maxX - minX + 1) * (gutter + width);
		int h = interline + (maxY - minY + 1) * (interline + height);
		return new Dimension(w, h);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return minimumLayoutSize(parent);
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		for (Iterator<Entry> i = components.iterator(); i.hasNext();) {
			Entry entry = i.next();
			if (entry.component.equals(comp)) {
				i.remove();
				return;
			}
		}
	}

	
	Stramien container(int x, int w, int y, int h) {
		JPanel panel = new JPanel();
		Stramien stramien = new Stramien( panel, gutter, width, interline, height, 0, 0);
		add( x, w, y, h, panel);
		return stramien;
	}
	
	
	public Stramien add(int col, int width, int row, int height,
			Component component) {
		Entry entry = new Entry();
		entry.x = col;
		entry.y = row;
		entry.w = width;
		entry.h = height;
		entry.component = component;
		components.add(entry);
		container.add(component);
		return this;
	}

	public static void main(String args[]) {
		JFrame frame = new JFrame();
		
		Stramien stramien = new Stramien(frame, 8, 32, 8, 20, 0,0);

		JToolBar tb = new JToolBar();
		tb.setBackground(Color.green);
		JPanel p1 = new JPanel();
		JPanel p2 = new JPanel();
		JButton cancel = new JButton("cancel");
		JButton commit = new JButton("commit");
		
		p1.setBackground(Color.blue);
		p2.setBackground(Color.red);
		stramien.add(0, 0, 0, 2, tb). //
				add(0, 4, 2, 0, p1). //
				add(4, 0, 2, -1, p2). //
				add(-4,2, -1, 1, cancel). //
				add(-2, 2, -1, 1, commit);

		frame.setMinimumSize( new Dimension(400,400));
		frame.setVisible(true);
	}
}
