/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "bndtools.release";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static void log(String msg, int msgType) {
		ILog log = getDefault().getLog();
		Status status = new Status(msgType, getDefault().getBundle().getSymbolicName(), msgType, msg + "\n", null);
		log.log(status);
		
	}

    void async(Runnable run) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(run);
        } else
            run.run();
    }
    public void message(final String msg) {
        async(new Runnable() {
            public void run() {
                MessageDialog.openInformation(null, "Release Bundle", msg);
            }
        });
    }

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	protected void initializeImageRegistry(ImageRegistry reg) {
		
		// Images
		ImageDescriptor id = getImageDescriptor("icons/tree16/bundle_obj.gif");
		reg.put("bundle", id);
		
		id = getImageDescriptor("icons/tree16/class_obj.gif");
		reg.put("class", id);
		
		id = getImageDescriptor("icons/tree16/field_public_obj.gif");
		reg.put("field", id);
		
		id = getImageDescriptor("icons/tree16/methpub_obj.gif");
		reg.put("method", id);
		
		id = getImageDescriptor("icons/tree16/package_obj.gif");
		reg.put("package", id);

		
		// Overlays
		id = getImageDescriptor("icons/ovr16/addition_red_ovr.gif");
		reg.put("major_add", id);
		
		id = getImageDescriptor("icons/ovr16/change_red_ovr.gif");
		reg.put("major_modify", id);
		
		id = getImageDescriptor("icons/ovr16/deletion_red_ovr.gif");
		reg.put("major_remove", id);
		
		id = getImageDescriptor("icons/ovr16/addition_blue_ovr.gif");
		reg.put("minor_add", id);
		
		id = getImageDescriptor("icons/ovr16/change_blue_ovr.gif");
		reg.put("minor_modify", id);
		
		id = getImageDescriptor("icons/ovr16/deletion_blue_ovr.gif");
		reg.put("minor_remove", id);
		
		id = getImageDescriptor("icons/ovr16/change_green_ovr.gif");
		reg.put("micro_modify", id);
		
		id = getImageDescriptor("icons/ovr16/deletion_green_ovr.gif");
		reg.put("micro_remove", id);

		id = getImageDescriptor("icons/ovr16/export_ovr.gif");
		reg.put("export", id);
		
		id = getImageDescriptor("icons/ovr16/import_ovr.gif");
		reg.put("import", id);

		id = getImageDescriptor("icons/ovr16/import_export_ovr.gif");
		reg.put("import_export", id);

		id = getImageDescriptor("icons/ovr16/static_ovr.gif");
		reg.put("static", id);

		// Images with overlay
		OverlayImageDescriptor oid = new OverlayImageDescriptor(reg, "package", "import");
		oid.setXValue(8);
		oid.setYValue(0);
		reg.put("package_import", oid);

		oid = new OverlayImageDescriptor(reg, "package", "export");
		oid.setXValue(8);
		oid.setYValue(0);
		reg.put("package_export", oid);

		oid = new OverlayImageDescriptor(reg, "package", "import_export");
		oid.setXValue(8);
		oid.setYValue(0);
		reg.put("package_import_export", oid);

		oid = new OverlayImageDescriptor(reg, "field", "static");
		oid.setXValue(0);
		oid.setYValue(0);
		reg.put("static_field", oid);

		oid = new OverlayImageDescriptor(reg, "method", "static");
		oid.setXValue(0);
		oid.setYValue(0);
		reg.put("static_method", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import", "major_add");
		reg.put("package_import_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_import", "minor_remove");
		reg.put("package_import_minor_remove", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import", "minor_modify");
		reg.put("package_import_minor_modify", oid);

		oid = new OverlayImageDescriptor(reg, "package_import", "micro_remove");
		reg.put("package_import_micro_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "major_add");
		reg.put("package_export_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "major_modify");
		reg.put("package_export_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_export", "major_remove");
		reg.put("package_export_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "minor_add");
		reg.put("package_export_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "minor_modify");
		reg.put("package_export_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_export", "minor_remove");
		reg.put("package_export_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "micro_modify");
		reg.put("package_export_micro_modify", oid);
		

		oid = new OverlayImageDescriptor(reg, "package_import_export", "major_add");
		reg.put("package_import_export_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "major_modify");
		reg.put("package_import_export_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import_export", "major_remove");
		reg.put("package_import_export_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "minor_add");
		reg.put("package_import_export_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "minor_modify");
		reg.put("package_import_export_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import_export", "minor_remove");
		reg.put("package_import_export_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "micro_modify");
		reg.put("package_import_export_micro_modify", oid);

		
		
		
		oid = new OverlayImageDescriptor(reg, "class", "major_add");
		reg.put("class_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "class", "major_modify");
		reg.put("class_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "class", "major_remove");
		reg.put("class_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "class", "minor_add");
		reg.put("class_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "class", "minor_modify");
		reg.put("class_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "class", "minor_remove");
		reg.put("class_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "field", "major_add");
		reg.put("field_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "field", "major_modify");
		reg.put("field_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "field", "major_remove");
		reg.put("field_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "field", "minor_add");
		reg.put("field_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "field", "minor_modify");
		reg.put("field_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "field", "minor_remove");
		reg.put("field_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "major_add");
		reg.put("static_field_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "major_modify");
		reg.put("static_field_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_field", "major_remove");
		reg.put("static_field_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "minor_add");
		reg.put("static_field_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "minor_modify");
		reg.put("static_field_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_field", "minor_remove");
		reg.put("static_field_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "method", "major_add");
		reg.put("method_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "method", "major_modify");
		reg.put("method_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "method", "major_remove");
		reg.put("method_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "method", "minor_add");
		reg.put("method_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "method", "minor_modify");
		reg.put("method_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "method", "minor_remove");
		reg.put("method_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "major_add");
		reg.put("static_method_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "major_modify");
		reg.put("static_method_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_method", "major_remove");
		reg.put("static_method_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "minor_add");
		reg.put("static_method_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "minor_modify");
		reg.put("static_method_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_method", "minor_remove");
		reg.put("static_method_minor_remove", oid);
}

}
