package org.bndtools.utils.jface;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

public class BoldStyler extends Styler {

	public static BoldStyler	INSTANCE_DEFAULT	= new BoldStyler(JFaceResources.DEFAULT_FONT, null, null);
	public static BoldStyler	INSTANCE_COUNTER	= new BoldStyler(JFaceResources.DEFAULT_FONT,
		JFacePreferences.COUNTER_COLOR, null);

	private final String		fontName;
	private final String		fForegroundColorName;
	private final String		fBackgroundColorName;

	public BoldStyler(String fontName, String foregroundColorName, String backgroundColorName) {
		this.fontName = fontName;
		fForegroundColorName = foregroundColorName;
		fBackgroundColorName = backgroundColorName;
	}

	@Override
	public void applyStyles(TextStyle textStyle) {
		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		Font font = JFaceResources.getFontRegistry()
			.getBold(fontName);
		if (fForegroundColorName != null) {
			textStyle.foreground = colorRegistry.get(fForegroundColorName);
		}
		if (fBackgroundColorName != null) {
			textStyle.background = colorRegistry.get(fBackgroundColorName);
		}
		textStyle.font = font;
	}
}
