package bndtools.release.ui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import bndtools.release.Activator;
import bndtools.release.ui.OverlayImage.Overlay;

public class BundleTreeImages {

	public static final String		IMPORT_EXPORT	= "importexport16";		//$NON-NLS-1$
	public static final String		DELTA			= "delta16";			//$NON-NLS-1$
	public static final String		TYPES			= "types16";			//$NON-NLS-1$
	public static final String		MODIFIERS		= "modifiers16";		//$NON-NLS-1$

	public static final String		BUNDLE_PATH		= "icons/bundletree";	//$NON-NLS-1$

	// Used for testing outside eclipse
	private static ImageRegistry	imageRegistry;

	public static Image resolveImage(String type, String delta, String impExp, String modifier) {
		// String tmpType = type;
		String tmpType = TYPES + '_' + type;
		Image imgType = getImageRegistry().get(tmpType);
		if (imgType == null) {
			tmpType = TYPES + "_unknown"; //$NON-NLS-1$
			imgType = getImageRegistry().get(tmpType);
			if (imgType == null) {
				return null;
			}
		}
		List<Overlay> overlays = new ArrayList<>();
		Image img = getImageRegistry().get(IMPORT_EXPORT + '_' + impExp);
		if (img != null) {
			overlays.add(new Overlay(IMPORT_EXPORT + '_' + impExp));
		}
		img = getImageRegistry().get(DELTA + '_' + delta);
		if (img != null) {
			Overlay overlay = new Overlay(DELTA + '_' + delta);
			overlay.setXValue(1);
			overlay.setYValue(1);
			overlays.add(overlay);
		}
		img = getImageRegistry().get(MODIFIERS + '_' + modifier);
		if (img != null) {
			overlays.add(new Overlay(MODIFIERS + '_' + modifier));
		}
		OverlayImage descr = new OverlayImage(getImageRegistry(), tmpType, overlays);
		return descr.getImage();
	}

	public static synchronized ImageRegistry getImageRegistry() {
		if (Activator.getDefault() == null) {
			if (imageRegistry == null) {
				imageRegistry = new ImageRegistry();
				initImageRegistry(imageRegistry);
			}
			return imageRegistry;
		}
		return Activator.getDefault()
			.getImageRegistry();
	}

	private static void initImageRegistry(ImageRegistry registry) {
		File root = new File("resources/" + BundleTreeImages.BUNDLE_PATH); //$NON-NLS-1$
		try {
			loadImages(root, BundleTreeImages.DELTA, registry);
			loadImages(root, BundleTreeImages.IMPORT_EXPORT, registry);
			loadImages(root, BundleTreeImages.MODIFIERS, registry);
			loadImages(root, BundleTreeImages.TYPES, registry);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	private static void loadImages(File iconRootDirectory, String parent, ImageRegistry registry)
		throws MalformedURLException {
		File icons = new File(iconRootDirectory, parent);
		File[] files = icons.listFiles();
		for (File file : files) {
			if (file.isFile() && file.getName()
				.endsWith(".gif")) { //$NON-NLS-1$
				URL url = file.toURI()
					.toURL();
				String name = getResourceName(url);
				ImageDescriptor id = ImageDescriptor.createFromURL(url);
				registry.put(parent + "_" + name, id); //$NON-NLS-1$
			}
		}
	}

	private static String getResourceName(URL url) {
		int idx = url.getPath()
			.lastIndexOf('/');
		String name = url.getPath()
			.substring(idx + 1);
		return name.substring(0, name.lastIndexOf('.'));
	}
}
