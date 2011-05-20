package bndtools.editor.project;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


public class OSGiFrameworkLabelProvider extends LabelProvider {

    private final Map<URL, Image> images = new HashMap<URL, Image>();

    @Override
    public String getText(Object element) {
        OSGiFramework fwk = (OSGiFramework) element;
        return fwk.toString();
    }

    @Override
    public Image getImage(Object element) {
        OSGiFramework fwk = (OSGiFramework) element;

        Image image = null;

        if (fwk.getIcon() != null) {
            image = images.get(fwk.getIcon());
            if (image == null) {
                InputStream stream = null;
                try {
                    stream = fwk.getIcon().openStream();
                    image = new Image(Display.getCurrent(), stream);
                } catch (IOException e) {
                } finally {
                    try {
                        if (stream != null) stream.close();
                    } catch (IOException e) {
                    }
                }

                if (image != null)
                    images.put(fwk.getIcon(), image);
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
