package org.bndtools.utils.jface;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

public class HyperlinkStyler extends Styler {

	private final Color color;

	public HyperlinkStyler() {
		this(Display.getCurrent());
	}

	public HyperlinkStyler(Display display) {
		color = display.getSystemColor(SWT.COLOR_BLUE);
	}

	@Override
	public void applyStyles(TextStyle style) {
		style.foreground = color;

		style.underline = true;
		style.underlineColor = color;
		style.underlineStyle = SWT.UNDERLINE_SINGLE;
	}

}
