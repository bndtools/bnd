package bndtools.editor.project;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class OSGiFrameworkLabelProvider extends LabelProvider {

	private final Map<URI, Image> images = new HashMap<>();

	@Override
	public String getText(Object element) {
		OSGiFramework fwk = (OSGiFramework) element;
		return fwk.toString();
	}

	@Override
	public Image getImage(Object element) {
		OSGiFramework fwk = (OSGiFramework) element;
		URL fwkIcon = fwk.getIcon();
		URI fwkIconURI = null;
		try {
			fwkIconURI = fwkIcon.toURI();
		} catch (URISyntaxException e1) {}

		Image image = null;

		if (fwkIconURI != null) {
			image = images.get(fwkIconURI);
			if (image == null) {
				try (InputStream stream = fwkIcon.openStream()) {
					image = new Image(Display.getCurrent(), stream);
				} catch (IOException e) {}
				if (image != null)
					images.put(fwkIconURI, image);
			}
		}

		return image;
	}

	@Override
	public void dispose() {
		for (Image image : images.values()) {
			image.dispose();
		}
		super.dispose();
	}
}
