package org.bndtools.core.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class SashFormPanelMaximiser {

    private final Image maximiseImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/maximize.png").createImage();
    private final Image restoreImg;

    private final SashForm sashForm;

    /**
     * Styles: SWT.HORIZONTAL, SWT.VERTICAL
     * 
     * @param sashForm
     * @param style
     */
    public SashFormPanelMaximiser(SashForm sashForm) {
        this.sashForm = sashForm;
        if ((sashForm.getOrientation() | SWT.VERTICAL) > 0)
            restoreImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tile-vertical.png").createImage();
        else
            restoreImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tile-horizontal.png").createImage();
    }

    public ToolItem createToolItem(final Composite panel, ToolBar toolbar) {
        final ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        item.setImage(maximiseImg);
        item.setToolTipText("Maximise");

        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Control maximised = sashForm.getMaximizedControl();
                if (maximised == panel) {
                    maximised = null;
                    item.setImage(maximiseImg);
                    item.setToolTipText("Maximise");
                } else {
                    maximised = panel;
                    item.setImage(restoreImg);
                    item.setToolTipText("Restore");
                }
                sashForm.setMaximizedControl(maximised);
            }
        });

        return item;
    }

    public void dispose() {
        maximiseImg.dispose();
        restoreImg.dispose();
    }

}
