package aQute.bnd.make;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.osgi.About;
import aQute.bnd.osgi.AbstractResource;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.MakePlugin;

public class MakeJar implements MakePlugin {

	@Override
	public Resource make(Builder builder, String destination, Map<String, String> argumentsOnMake) throws Exception {
		String type = argumentsOnMake.get("type"); //$NON-NLS-1$
		if (!"jar".equals(type)) { //$NON-NLS-1$
			return null;
		}
		String input = argumentsOnMake.get("input"); //$NON-NLS-1$
		if (input == null) {
			builder.error("No input specified on a make instruction for %s, args=%s", destination, argumentsOnMake); //$NON-NLS-1$
			return null;
		}
		File folder = builder.getFile(input);
		if (!folder.isDirectory()) {
			return null;
		}
		boolean noExtra = Boolean.parseBoolean(argumentsOnMake.get("noExtra")); //$NON-NLS-1$
		boolean reproducible = Boolean.parseBoolean(argumentsOnMake.get("reproducible")); //$NON-NLS-1$
		return new AbstractResource(System.currentTimeMillis()) {

			@Override
			protected byte[] getBytes() throws Exception {
				try (Jar jar = new Jar(folder)) {
					Manifest manifest = new Manifest();
					Attributes mainAttributes = manifest.getMainAttributes();
					mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
					if (!noExtra) {
						mainAttributes.putValue(Constants.CREATED_BY, String.format("%s (%s)", //$NON-NLS-1$
							System.getProperty("java.version"), System.getProperty("java.vendor"))); //$NON-NLS-1$ //$NON-NLS-2$
						mainAttributes.putValue(Constants.TOOL, "Bnd-" + About.getBndVersion()); //$NON-NLS-1$
						if (!reproducible) {
							mainAttributes.putValue(Constants.BND_LASTMODIFIED, Long.toString(lastModified()));
						}
					}
					jar.setManifest(manifest);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					jar.write(stream);
					return stream.toByteArray();
				}
			}
		};
	}

}
