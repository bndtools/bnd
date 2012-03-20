package org.bndtools.core.utils.jface;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.TextStyle;

public class StrikeoutStyler extends Styler {

    private final Styler base;
    private final Color strikeoutColor;

    public StrikeoutStyler() {
        this(null, null);
    }

    public StrikeoutStyler(Styler base) {
        this(base, null);
    }

    public StrikeoutStyler(Styler base, Color strikeoutColor) {
        this.base = base;
        this.strikeoutColor = strikeoutColor;
    }

    @Override
    public void applyStyles(TextStyle textStyle) {
        if (base != null)
            base.applyStyles(textStyle);
        textStyle.strikeout = true;

        if (strikeoutColor != null)
            textStyle.strikeoutColor = strikeoutColor;
    }
}