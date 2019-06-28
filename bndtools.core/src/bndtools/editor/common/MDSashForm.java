package bndtools.editor.common;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;

public class MDSashForm extends SashForm {

	ArrayList<Sash>	sashes		= new ArrayList<>();
	Listener		listener	= e -> {
									switch (e.type) {
										case SWT.MouseEnter :
											e.widget.setData("hover", Boolean.TRUE);				//$NON-NLS-1$
											((Control) e.widget).redraw();
											break;
										case SWT.MouseExit :
											e.widget.setData("hover", null);						//$NON-NLS-1$
											((Control) e.widget).redraw();
											break;
										case SWT.Paint :
											onSashPaint(e);
											break;
										case SWT.Resize :
											hookSashListeners();
											break;
										default :
											break;
									}
								};

	private Color	bg;
	private Color	fg;

	public MDSashForm(Composite parent, int style, Color bg, Color fg) {
		super(parent, style);
		this.bg = bg;
		this.fg = fg;
	}

	public MDSashForm(Composite parent, int style, IManagedForm managedForm) {
		super(parent, style);

		FormColors colors = managedForm.getToolkit()
			.getColors();
		bg = colors.getColor(IFormColors.TB_BG);
		fg = colors.getColor(IFormColors.TB_BORDER);
	}

	@Override
	public void layout(boolean changed) {
		super.layout(changed);
		hookSashListeners();
	}

	@Override
	public void layout(Control[] children) {
		super.layout(children);
		hookSashListeners();
	}

	private void hookSashListeners() {
		purgeSashes();
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Sash) {
				Sash sash = (Sash) children[i];
				if (sashes.contains(sash))
					continue;
				sash.addListener(SWT.Paint, listener);
				sash.addListener(SWT.MouseEnter, listener);
				sash.addListener(SWT.MouseExit, listener);
				sashes.add(sash);
			}
		}
	}

	private void purgeSashes() {
		for (Iterator<Sash> iter = sashes.iterator(); iter.hasNext();) {
			Sash sash = iter.next();
			if (sash.isDisposed())
				iter.remove();
		}
	}

	private void onSashPaint(Event e) {
		Sash sash = (Sash) e.widget;
		boolean vertical = (sash.getStyle() & SWT.VERTICAL) != 0;
		GC gc = e.gc;
		Boolean hover = (Boolean) sash.getData("hover"); //$NON-NLS-1$
		gc.setBackground(bg);
		gc.setForeground(fg);
		Point size = sash.getSize();
		if (vertical) {
			if (hover != null)
				gc.fillRectangle(0, 0, size.x, size.y);
			// else
			// gc.drawLine(1, 0, 1, size.y-1);
		} else {
			if (hover != null)
				gc.fillRectangle(0, 0, size.x, size.y);
			// else
			// gc.drawLine(0, 1, size.x-1, 1);
		}
	}

	public void hookResizeListener() {
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Sash)
				continue;
			children[i].addListener(SWT.Resize, listener);
		}
	}
}
