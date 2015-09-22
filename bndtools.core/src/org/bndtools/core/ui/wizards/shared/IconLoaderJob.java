package org.bndtools.core.ui.wizards.shared;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.templating.Template;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;

import aQute.lib.io.IO;
import bndtools.Plugin;

/*
 * Loads icons from the templates in a background job, and updates the UI in batches when icons become available.
 */
public class IconLoaderJob extends Job {

    private final ILog log = Plugin.getDefault().getLog();

    private final List<Template> templates;
    private final int batchLimit;
    private final StructuredViewer viewer;

    private final Map<Template,Image> loadedImageMap;

    public IconLoaderJob(List<Template> templates, StructuredViewer viewer, Map<Template,Image> loadedImageMap, int batchLimit) {
        super("load template icons");
        this.templates = templates;
        this.viewer = viewer;
        this.loadedImageMap = loadedImageMap;
        this.batchLimit = batchLimit;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, templates.size());

        Map<Template,byte[]> batch = new IdentityHashMap<>();

        for (Template template : templates) {
            URI iconUri = template.getIcon();
            if (iconUri != null) {
                try (InputStream in = iconUri.toURL().openStream()) {
                    byte[] bytes = IO.read(in);
                    batch.put(template, bytes);

                    if (batch.size() >= batchLimit) {
                        processBatch(batch);
                        batch = new IdentityHashMap<>();
                    }
                } catch (IOException e) {
                    log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading icon for template '" + template.getName() + "'", e));
                }
            }
            progress.worked(1);
        }
        processBatch(batch);
        return Status.OK_STATUS;
    }

    private void processBatch(final Map<Template,byte[]> batch) {
        if (batch.isEmpty())
            return;
        final Control control = viewer.getControl();
        if (control != null && !control.isDisposed()) {
            control.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (control.isDisposed())
                        return;
                    List<Object> toUpdate = new ArrayList<>(batch.size());

                    for (Entry<Template,byte[]> entry : batch.entrySet()) {
                        Template template = entry.getKey();
                        byte[] imgBytes = entry.getValue();

                        try {
                            ImageData imgData = new ImageData(new ByteArrayInputStream(imgBytes));
                            Image image = new Image(control.getDisplay(), imgData);

                            Image old = loadedImageMap.put(template, image);
                            if (old != null && !old.isDisposed())
                                old.dispose();
                            toUpdate.add(template);
                        } catch (Exception e) {
                            log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading image data for template icon: " + template.getName(), e));
                        }
                    }

                    viewer.update(toUpdate.toArray(new Object[toUpdate.size()]), null);
                }
            });
        }
    }

}
