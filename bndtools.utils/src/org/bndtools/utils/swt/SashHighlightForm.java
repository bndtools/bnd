package org.bndtools.utils.swt;

import java.lang.reflect.Field;

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Sash;

/**
 * <p>
 * SWT's {@link SashForm} is moronic. There is no way to set the colour of just
 * the sashes... calling {@link #setBackground(Color)} results in setting the
 * background colour of the <b>entire</b> form area.
 * </p>
 * <p>
 * This class has to use nasty hacks to give us access to the superclass fields
 * that we need to modify. It's a toss up between this and completely
 * reimplementing {@link SashForm}.
 * </p>
 *
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
public class SashHighlightForm extends SashForm {

	public SashHighlightForm(Composite parent, int style) {
		super(parent, style);
	}

	public void setSashBackground(Color color) {
		try {
			Field bgfield = SashForm.class.getDeclaredField("background");
			bgfield.setAccessible(true);
			bgfield.set(this, color);

			Field sashesField = SashForm.class.getDeclaredField("sashes");
			sashesField.setAccessible(true);
			Sash[] sashes = (Sash[]) sashesField.get(this);
			for (Sash sash : sashes) {
				sash.setBackground(color);
			}
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public void setSashForeground(Color color) {
		try {
			Field fgfield = SashForm.class.getDeclaredField("foreground");
			fgfield.setAccessible(true);
			fgfield.set(this, color);

			Field sashesField = SashForm.class.getDeclaredField("sashes");
			sashesField.setAccessible(true);
			Sash[] sashes = (Sash[]) sashesField.get(this);
			for (Sash sash : sashes) {
				sash.setForeground(color);
			}
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
