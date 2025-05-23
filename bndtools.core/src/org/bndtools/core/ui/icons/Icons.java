package org.bndtools.core.ui.icons;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public final class Icons {

	private static final String			ICONS_MISSING_GIF	= "icons/missing.gif";
	private static final ISharedImages	shared;
	static {
		ISharedImages images;
		try {
			images = PlatformUI.getWorkbench()
				.getSharedImages();
		} catch (Throwable e) {
			images = new ISharedImages() {
				@Override
				public Image getImage(String symbolicName) {
					return null;
				}

				@Override
				public ImageDescriptor getImageDescriptor(String symbolicName) {
					return null;
				}
			};
		}
		shared = images;

	}

	private static class Key {
		final String	name;
		final String[]	decorators	= new String[4];

		Key(String name, String... decorators) {
			this.name = name;
			System.arraycopy(decorators, 0, this.decorators, 0, Math.min(4, decorators.length));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(decorators);
			result = prime * result + Objects.hash(name);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			return Arrays.equals(decorators, other.decorators) && Objects.equals(name, other.name);
		}

	}

	private static Properties		properties	= new Properties();
	private static Map<Key, Image>	images		= new HashMap<>();

	static {
		try (InputStream in = Icons.class.getResourceAsStream("/icons.properties")) {
			properties.load(in);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load icons.properties");
		}
	}

	public static String path(String name) {
		return properties.getProperty("icons." + name, ICONS_MISSING_GIF);
	}

	public static ImageDescriptor desc(String name) {
		return desc(name, false);
	}

	public static ImageDescriptor desc(String name, boolean nullIfAbsent) {
		if (name == null) {
			if (nullIfAbsent)
				return null;
			name = "unknown";
		} else if (name.indexOf('/') >= 0) {
			return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, name);
		}

		String path = path(name);

		if (isSystem(path)) {
			return shared.getImageDescriptor(path.substring(1));
		}

		if (path == ICONS_MISSING_GIF) {
			ImageDescriptor imageDescriptor = shared.getImageDescriptor(name);
			if (imageDescriptor != null)
				return imageDescriptor;

			System.out.println(" missing " + name);
			if (nullIfAbsent)
				return null;
		}

		ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, path);
		if (desc == null) {
			System.out.println(" missing badly " + name);

		}
		return desc;
	}

	public static void clear() {
		synchronized (images) {
			images.entrySet()
				.removeIf(e -> {
					String path = path(e.getKey().name);

					if (!isSystem(path))
						e.getValue()
							.dispose();

					return true;
				});
		}
	}

	/**
	 * @return a shared / managed Image. Callers must NOT call .dispose()
	 */
	public static Image image(String name, boolean nullIfAbsent) {

		ImageDescriptor desc = desc(name, nullIfAbsent);
		if (desc == null)
			return null;

		Key k = new Key(name);
		synchronized (images) {
			Image image = images.get(k);
			if (image == null) {
				image = desc.createImage();
				images.put(k, image);
			}
			return image;
		}
	}

	/**
	 * @return a shared / managed Image. Callers must NOT call .dispose()
	 */
	public static Image image(String name, String... decorators) {
		synchronized (images) {
			Key k = new Key(name, decorators);
			Image image = images.get(k);
			if (image != null && !image.isDisposed())
				return image;

			if (decorators.length == 0) {
				ImageDescriptor desc = desc(name, false);
				if (desc == null) {
					desc = desc(ICONS_MISSING_GIF);
				}
				Image baseImage = desc.createImage();
				images.put(k, baseImage);
				return baseImage;
			}

			Image baseImage = image(name);

			ImageDescriptor overlays[] = new ImageDescriptor[decorators.length];
			for (int i = 0; i < decorators.length; i++) {
				overlays[i] = desc(decorators[i], true);
			}
			DecorationOverlayIcon decoratedImage = new DecorationOverlayIcon(baseImage, overlays);
			Image result = decoratedImage.createImage();
			images.put(k, result);
			return result;
		}
	}

	private static boolean isSystem(String path) {
		return path.startsWith("$");
	}

	public static class IconBuilder {
		private final String	baseImage;
		private final String[]	descriptor	= new String[4];

		IconBuilder(String icon) {
			baseImage = icon;
		}

		public IconBuilder topLeft(String imageName) {
			this.descriptor[IDecoration.TOP_LEFT] = imageName;
			return this;
		}

		public IconBuilder topRight(String imageName) {
			this.descriptor[IDecoration.TOP_RIGHT] = imageName;
			return this;
		}

		public IconBuilder bottomLeft(String imageName) {
			this.descriptor[IDecoration.BOTTOM_LEFT] = imageName;
			return this;
		}

		public IconBuilder bottomRight(String imageName) {
			this.descriptor[IDecoration.BOTTOM_RIGHT] = imageName;
			return this;
		}

		public Image build() {
			return image(baseImage, descriptor);
		}

	}

	public static IconBuilder builder(String icon) {
		return new IconBuilder(icon);
	}
}
