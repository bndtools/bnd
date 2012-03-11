package org.bndtools.core.utils.jface;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.TextStyle;

public class StrikeoutStyler extends Styler {

    private final Styler base;

    public StrikeoutStyler(Styler base) {
        this.base = base;
    }

    @Override
    public void applyStyles(TextStyle textStyle) {
        if (base != null)
            base.applyStyles(textStyle);
        textStyle.strikeout = true;
    }
}