package org.bndtools.utils.jface;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

public class ItalicStyler extends Styler {

	public static final Styler	INSTANCE_DEFAULT	= new ItalicStyler(JFaceResources.DEFAULT_FONT, null, null);
	public static final Styler	INSTANCE_QUALIFIER	= new ItalicStyler(JFaceResources.DEFAULT_FONT,
		JFacePreferences.QUALIFIER_COLOR, null);
	public static final Styler	INSTANCE_ERROR		= new ItalicStyler(JFaceResources.DEFAULT_FONT,
		JFacePreferences.ERROR_COLOR, null);

	private final String		fontName;
	private final String		fForegroundColorName;
	private final String		fBackgroundColorName;

	public ItalicStyler(String fontName, String foregroundColorName, String backgroundColorName) {
		this.fontName = fontName;
		fForegroundColorName = foregroundColorName;
		fBackgroundColorName = backgroundColorName;
	}

	@Override
	public void applyStyles(TextStyle textStyle) {
		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		Font font = JFaceResources.getFontRegistry()
			.getItalic(fontName);
		if (fForegroundColorName != null) {
			textStyle.foreground = colorRegistry.get(fForegroundColorName);
		}
		if (fBackgroundColorName != null) {
			textStyle.background = colorRegistry.get(fBackgroundColorName);
		}
		textStyle.font = font;
	}
}
