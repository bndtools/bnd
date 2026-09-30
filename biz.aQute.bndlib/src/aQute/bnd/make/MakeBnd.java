package aQute.bnd.make;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.MakePlugin;

public class MakeBnd implements MakePlugin, Constants {

	@SuppressWarnings("deprecation")
	@Override
	public Resource make(Builder builder, String destination, Map<String, String> argumentsOnMake) throws Exception {
		String type = argumentsOnMake.get("type");
		if (!"bnd".equals(type))
			return null;

		String recipe = argumentsOnMake.get("recipe");
		if (recipe == null) {
			builder.error("No recipe specified on a make instruction for %s, args=%s", destination, argumentsOnMake);
			return null;
		}
		File bndfile = builder.getFile(recipe);
		if (!bndfile.isFile()) {
			return null;
		}

		// We do not use a parent because then we would
		// build ourselves again. So we can not blindly
		// inherit the properties.
		Set<Jar> inheritedClasspath = new HashSet<>(builder.getClasspath());
		Builder bchild = builder.getSubBuilder();
		try {
			bchild.removeBundleSpecificHeaders();

			// We must make sure that we do not include ourselves again!
			bchild.setProperty(Constants.INCLUDE_RESOURCE, "");
			bchild.setProperty(Constants.INCLUDERESOURCE, "");
			bchild.setProperties(bndfile, builder.getBase());

			Jar jar = bchild.build();
			builder.getInfo(bchild, bndfile.getName() + ": ");

			if (jar != null) {
				if (builder.hasSources()) {
					Jar dot = builder.getJar();
					if (dot != null) {
						for (String key : jar.getResources()
							.keySet()) {
							if (key.startsWith("OSGI-OPT/src/"))
								dot.putResource(key, jar.getResource(key));
						}
					}
				}

				Jar copy = copyJar(jar);
				jar.setDerived();
				return new JarResource(copy);
			} else {
				builder.error("Could not create make resource, args=%s", argumentsOnMake);
				return null;
			}
		} finally {
			for (Jar classpath : bchild.getClasspath()) {
				if (inheritedClasspath.contains(classpath))
					bchild.removeClose(classpath);
			}
			bchild.close();
		}
	}

	private static Jar copyJar(Jar source) throws Exception {
		Jar copy = new Jar(source.getName());
		copy.setManifestName(source.getManifestName());
		copy.setCompression(source.hasCompression());
		if (source.isReproducible()) {
			copy.setReproducible(Long.toString(source.getReproducibleTimestamp() / 1000));
		}
		copy.updateModified(source.lastModified(), "Copied jar");
		Manifest manifest = source.getManifest();
		if (manifest != null) {
			copy.setManifest(new Manifest(manifest));
		} else {
			copy.setDoNotTouchManifest();
		}
		for (Map.Entry<String, Resource> entry : source.getResources()
			.entrySet()) {
			String path = entry.getKey();
			Resource resource = entry.getValue();
			if (manifest != null && path.equals(copy.getManifestName())) {
				continue;
			}
			if (resource instanceof JarResource jarResource) {
				resource = new JarResource(copyJar(jarResource.getJar()));
			}
			copy.putResource(path, resource);
		}
		return copy;
	}

}
