package aQute.bnd.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.strings.Strings;

public class EclipseBuildProperties {

	final List<Library> libraries = new ArrayList<>();
	private Processor properties;

	class Library {

		EclipseManifest	manifest;
		List<String>	includes	= new ArrayList<>();
		List<String>	excludes	= new ArrayList<>();
		List<String>	sources		= new ArrayList<>();
		List<String>	extra		= new ArrayList<>();
		String output;
		
		Library(String libName, Processor properties) throws IOException {
			String outputProperty = properties.getProperty("output."+libName, "bin");
			output = ensureDir(outputProperty);
			if (properties.getProperty("source."+libName) != null)
				for (String  s : Strings.split(properties.get("source."+libName)))
					this.sources.add( ensureDir(s));
			if (properties.getProperty("bin.includes") != null)
				this.includes.addAll(Strings.split(properties.get("bin.includes")));
			if (properties.getProperty("bin.excludes") != null)
				this.excludes.addAll(Strings.split(properties.get("bin.excludes")));
			if (properties.getProperty("exclude." + libName) != null)
				this.excludes.addAll(Strings.split(properties.get("exclude." + libName)));

			String manifest = properties.getProperty("manifest." + libName, "META-INF/MANIFEST.MF");
			File file = properties.getFile(manifest);
			if (!file.isFile()) {
				properties.error("Cannot find manifest file %s for lib %s", file);
			}
			this.manifest = new EclipseManifest(properties, manifest);
		}

		private String ensureDir(String property) {
			return property.endsWith("/") ? property : property+"/";
		}

		public EclipseManifest getManifest() {
			return manifest;
		}

		public void move(Jar content, BndConversionPaths srcMainJava, BndConversionPaths srcMainResources,
				BndConversionPaths srcTestJava, BndConversionPaths srcTestResources) {

			for (String sourceDir : sources) {
				if (srcTestJava.has(sourceDir))
					srcTestJava.move(content, sourceDir);
				else if (srcMainJava.has(sourceDir)) {
					srcMainJava.move(content, sourceDir);
				} else {
					srcMainJava.addDirectory(sourceDir);
					srcMainJava.move(content, sourceDir);
				}
			}

			for (String resource : includes) {
				if ( resource.equals("."))
					continue;
				
				String to = srcMainResources.directories.get(0) + resource;
				if ( content.move(resource, to) == 0) {
					properties.warning("No resources were found %s", resource);
				}
			}
		}

		public void removeOutputs(Jar content) {
			content.removePrefix(output);
			content.remove("build.properties");
		}

	}

	public EclipseBuildProperties(Processor properties) throws IOException {
		this.properties = properties;
		List<String> jarsCompileOrder = Strings.split(properties.getProperty("jars.compile.order", "."));
		if (jarsCompileOrder.isEmpty()) {
			properties.error("the jars.compile.order=%s, is empty", jarsCompileOrder);
			return;
		}

		for (String libName : jarsCompileOrder) {
			if (isDefault(libName) && jarsCompileOrder.size() > 1) {
				properties.error(
						"the jars.compile.order=%s, when  multiple libraries are build they should all be named and not be named '.'",
						jarsCompileOrder);
			}

			libraries.add(new Library(libName, properties));
		}

		if (libraries.size() != 1)
			throw new UnsupportedOperationException("Only can import projects consisting of one plugin");
	}

	private boolean isDefault(String libName) {
		return ".".equals(libName);
	}

	public void rearrange(Project p, Jar jar) {

	}

	public List<Library> getLibraries() {
		return libraries;
	}

}
