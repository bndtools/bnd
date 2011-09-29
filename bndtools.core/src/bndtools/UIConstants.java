/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

public class UIConstants {
	public static final char[] AUTO_ACTIVATION_CLASSNAME = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890._".toCharArray(); //$NON-NLS-1$
	public static final Styler ITALIC_QUALIFIER_STYLER = new ItalicStyler(JFaceResources.DEFAULT_FONT, JFacePreferences.QUALIFIER_COLOR, null);
	public static final Styler ERROR_STYLER = new ItalicStyler(JFaceResources.DEFAULT_FONT, JFacePreferences.ERROR_COLOR, null);

	private static class ItalicStyler extends Styler {
		private final String fontName;
		private final String fForegroundColorName;
		private final String fBackgroundColorName;

		public ItalicStyler(String fontName, String foregroundColorName,
				String backgroundColorName) {
			this.fontName = fontName;
			fForegroundColorName = foregroundColorName;
			fBackgroundColorName = backgroundColorName;
		}

		@Override
		public void applyStyles(TextStyle textStyle) {
			ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
			Font font = JFaceResources.getFontRegistry().getItalic(fontName);
			if (fForegroundColorName != null) {
				textStyle.foreground = colorRegistry.get(fForegroundColorName);
			}
			if (fBackgroundColorName != null) {
				textStyle.background = colorRegistry.get(fBackgroundColorName);
			}
			textStyle.font = font;
		}
	}
}
