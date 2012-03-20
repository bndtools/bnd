package org.bndtools.core.ui;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * A {@link FormPage} that allows updating of tab titles, images and overlay images.
 * 
 * @author Neil Bartlett
 *
 */
public abstract class ExtendedFormEditor extends FormEditor {

    private ImageDescriptor baseImageDescriptor;
    private Image titleImage;

    private ImageDescriptor overlaidTitleImageDescriptor;
    private Image overlaidTitleImage;

    @Override
    public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
        super.setInitializationData(cfig, propertyName, data);

        String strIcon = cfig.getAttribute("icon");//$NON-NLS-1$
        if (strIcon == null) {
            return;
        }

        baseImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(cfig.getContributor().getName(), strIcon);
        if (baseImageDescriptor == null) {
            return;
        }

        titleImage = JFaceResources.getResources().createImageWithDefault(baseImageDescriptor);
    }

    @Override
    public Image getTitleImage() {
        if (overlaidTitleImage != null) {
            return overlaidTitleImage;
        }

        if (titleImage != null) {
            return titleImage;
        }

        return getDefaultImage();
    }

    @Override
    public void dispose() {
        if (baseImageDescriptor != null)
            JFaceResources.getResources().destroyImage(baseImageDescriptor);
        if (overlaidTitleImageDescriptor != null)
            JFaceResources.getResources().destroyImage(overlaidTitleImageDescriptor);
    }

    public void setOverlayTitleImage(ImageDescriptor overlay) {
        if (overlay == null) {
            overlaidTitleImage = null;
            firePropertyChange(PROP_TITLE);
            if (overlaidTitleImageDescriptor != null)
                JFaceResources.getResources().destroyImage(overlaidTitleImageDescriptor);
            overlaidTitleImageDescriptor = null;
        } else {
            DecorationOverlayIcon newOverlaidDesc = new DecorationOverlayIcon(titleImage, overlay, IDecoration.BOTTOM_LEFT);
            overlaidTitleImage = JFaceResources.getResources().createImage(newOverlaidDesc);
            firePropertyChange(PROP_TITLE);
            if (overlaidTitleImageDescriptor != null)
                JFaceResources.getResources().destroyImage(overlaidTitleImageDescriptor);
            overlaidTitleImageDescriptor = newOverlaidDesc;
        }
    }

    public void updatePageTitle(IFormPage page) {
        int index = pages.indexOf(page);
        if (index != -1) {
            setPageImage(index, page.getTitleImage());
            setPageText(index, page.getTitle());
        }
    }

    @Override
    protected void configurePage(int index, IFormPage page) throws PartInitException {
        super.configurePage(index, page);
        Image image = page.getTitleImage();
        if (image != null) setPageImage(index, page.getTitleImage());
    }


}
