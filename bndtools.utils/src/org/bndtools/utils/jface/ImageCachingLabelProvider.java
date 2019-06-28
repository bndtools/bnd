package org.bndtools.utils.jface;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ImageCachingLabelProvider extends StyledCellLabelProvider {

	private static final Bundle			bundle	= FrameworkUtil.getBundle(ImageCachingLabelProvider.class);

	private final Map<String, Image>	cache	= new HashMap<>();
	private final String				pluginId;

	private final ILog					log;

	public ImageCachingLabelProvider(String pluginId) {
		this.pluginId = pluginId;

		this.log = (bundle != null) ? InternalPlatform.getDefault()
			.getLog(bundle) : null;
	}

	protected synchronized Image getImage(String path, boolean returnMissingImageOnError) {
		Image image = cache.get(path);
		if (image != null)
			return image;

		ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, path);
		if (descriptor == null) {
			String error = String.format("Missing image descriptor for plugin ID %s, path %s", pluginId, path);
			if (returnMissingImageOnError) {
				if (log != null)
					log.log(new Status(IStatus.ERROR, pluginId, 0, error, null));
				descriptor = ImageDescriptor.getMissingImageDescriptor();
			} else {
				throw new IllegalArgumentException(error);
			}
		}
		image = descriptor.createImage(returnMissingImageOnError);
		cache.put(path, image);

		return image;
	}

	@Override
	public void dispose() {
		super.dispose();

		synchronized (this) {
			for (Entry<String, Image> entry : cache.entrySet()) {
				Image image = entry.getValue();
				if (!image.isDisposed())
					image.dispose();
			}
		}
	}
}
