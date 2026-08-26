package org.bndtools.core.ui;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Viewer tooltip support that behaves like the JDT editor hovers: the hover
 * never grows beyond the parent window shell, shows scrollbars only when the
 * content is clipped, and can be pinned by clicking into it. The pinned popup
 * is a resizable, scrollable {@link PopupDialog} that closes on Esc or when it
 * loses focus.
 */
public class StickyToolTipSupport extends ColumnViewerToolTipSupport {

	/** Minimum distance kept between the tooltip edges and the parent shell edges. */
	private static final int	SHELL_INSET		= 24;
	private static final String	FOCUS_HINT		= "Click to focus";
	private static final String	CLOSE_HINT		= "Resize and scroll as needed. Press 'Esc' to close";

	private final Control		control;

	protected StickyToolTipSupport(ColumnViewer viewer, int style, boolean manualActivationMethod) {
		super(viewer, style, manualActivationMethod);
		this.control = viewer.getControl();
		// keep the tooltip open when the user clicks into it (to pin it)
		setHideOnMouseDown(false);
	}

	public static void enableFor(ColumnViewer viewer) {
		new StickyToolTipSupport(viewer, ToolTip.NO_RECREATE, false);
	}

	public static void enableFor(ColumnViewer viewer, int style) {
		new StickyToolTipSupport(viewer, style, false);
	}

	@Override
	protected Composite createViewerToolTipContentArea(Event event, ViewerCell cell, Composite parent) {
		final String tipText = getText(event) != null ? getText(event) : "";
		final Color fg = getForegroundColor(event);
		final Color bg = getBackgroundColor(event);
		final Font font = getFont(event);

		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		comp.setLayout(layout);
		if (bg != null)
			comp.setBackground(bg);

		final StyledText text = createTextArea(comp, tipText, fg, bg, font);

		Label sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label status = new Label(comp, SWT.NONE);
		status.setText(FOCUS_HINT);
		if (bg != null)
			status.setBackground(bg);
		status.setForeground(status.getDisplay()
			.getSystemColor(SWT.COLOR_DARK_GRAY));
		GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusData.horizontalIndent = 5;
		status.setLayoutData(statusData);

		// clamp the text area so the packed tooltip never exceeds the parent shell
		Point maxSize = getMaxTipSize();
		int chromeHeight = sep.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + status.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		Point preferred = text.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
		if (preferred.x > maxSize.x)
			textData.widthHint = maxSize.x;
		int maxTextHeight = Math.max(50, maxSize.y - chromeHeight);
		if (preferred.y > maxTextHeight)
			textData.heightHint = maxTextHeight;
		text.setLayoutData(textData);

		// clicking into the hover pins it as a resizable, scrollable popup
		Listener pinListener = e -> {
			Shell tipShell = text.getShell();
			Rectangle tipBounds = tipShell.getBounds();
			int topIndex = text.getTopIndex();
			int hOffset = text.getHorizontalPixel();
			hide();
			StickyPopup popup = new StickyPopup(control.getShell(), tipText, fg, bg, font, tipBounds);
			popup.open();
			popup.restoreScrollPosition(topIndex, hOffset);
		};
		text.addListener(SWT.MouseDown, pinListener);
		status.addListener(SWT.MouseDown, pinListener);

		return comp;
	}

	@Override
	public Point getLocation(Point tipSize, Event event) {
		// keep the tooltip fully inside the parent window shell
		Point location = super.getLocation(tipSize, event);
		Rectangle bounds = control.getShell()
			.getBounds();
		location.x = Math.max(bounds.x, Math.min(location.x, bounds.x + bounds.width - tipSize.x));
		location.y = Math.max(bounds.y, Math.min(location.y, bounds.y + bounds.height - tipSize.y));
		return location;
	}

	private Point getMaxTipSize() {
		Rectangle bounds = control.getShell()
			.getBounds();
		return new Point(Math.max(200, bounds.width - 2 * SHELL_INSET), Math.max(100, bounds.height - 2 * SHELL_INSET));
	}

	private static StyledText createTextArea(Composite parent, String content, Color fg, Color bg, Font font) {
		StyledText text = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		// scrollbars appear only when the content is actually clipped
		text.setAlwaysShowScrollBars(false);
		text.setMargins(5, 5, 5, 5);
		text.setCaret(null);
		if (font != null)
			text.setFont(font);
		if (fg != null)
			text.setForeground(fg);
		if (bg != null)
			text.setBackground(bg);
		text.setText(content);
		return text;
	}

	/**
	 * The pinned variant of the hover: a resizable popup that keeps the hover
	 * bounds, closes on Esc or focus loss.
	 */
	private static class StickyPopup extends PopupDialog {

		private final String	text;
		private final Color		fg;
		private final Color		bg;
		private final Font		font;
		private final Rectangle	initialBounds;
		private final Rectangle	clampBounds;
		private StyledText		textArea;

		StickyPopup(Shell parent, String text, Color fg, Color bg, Font font, Rectangle initialBounds) {
			super(parent, SWT.RESIZE | SWT.TOOL, true, false, false, false, false, null, CLOSE_HINT);
			this.text = text;
			this.fg = fg;
			this.bg = bg;
			this.font = font;
			this.initialBounds = initialBounds;
			this.clampBounds = parent.getBounds();
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			textArea = createTextArea(parent, text, fg, bg, font);
			textArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			textArea.addListener(SWT.Traverse, e -> {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
					close();
				}
			});
			return textArea;
		}

		@Override
		protected Control createContents(Composite parent) {
			Control contents = super.createContents(parent);
			// re-apply custom presentation, PopupDialog overwrites it with defaults
			if (font != null)
				textArea.setFont(font);
			if (fg != null)
				textArea.setForeground(fg);
			if (bg != null)
				textArea.setBackground(bg);
			return contents;
		}

		@Override
		protected Color getForeground() {
			return fg != null ? fg : super.getForeground();
		}

		@Override
		protected Color getBackground() {
			return bg != null ? bg : super.getBackground();
		}

		@Override
		protected Control getFocusControl() {
			return textArea;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(initialBounds.width, initialBounds.height);
		}

		@Override
		protected Point getInitialLocation(Point size) {
			// open at the hover position, clamped to the parent shell
			Point location = new Point(initialBounds.x, initialBounds.y);
			location.x = Math.max(clampBounds.x, Math.min(location.x, clampBounds.x + clampBounds.width - size.x));
			location.y = Math.max(clampBounds.y, Math.min(location.y, clampBounds.y + clampBounds.height - size.y));
			return location;
		}

		void restoreScrollPosition(int topIndex, int horizontalPixel) {
			if (textArea != null && !textArea.isDisposed()) {
				textArea.setTopIndex(topIndex);
				textArea.setHorizontalPixel(horizontalPixel);
			}
		}
	}
}
