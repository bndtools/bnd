package aQute.bnd.osgi;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.IO;

class ReleaseEntry {

	static final int											NO_RELEASE			= 0;
	static final Pattern										MULTI_RELEASE_PATH	= Pattern
		.compile("^META-INF/versions/(\\d+)/(.*)$", Pattern.CASE_INSENSITIVE);

	private final NavigableMap<String, Resource>				resources			= new TreeMap<>();
	private final NavigableMap<String, Map<String, Resource>>	directories			= new TreeMap<>();

	private Jar													jar;
	private Optional<Manifest>									manifest;
	private Optional<ModuleAttribute>							moduleAttribute;

	private int													release;

	ReleaseEntry(Jar jar, int release) {
		this.jar = jar;
		this.release = release;
	}

	boolean putResource(String path, Resource resource, boolean overwrite) {
		if (path.equals(jar.getManifestName())) {
			manifest = null;
		} else if (path.equals(Constants.MODULE_INFO_CLASS)) {
			moduleAttribute = null;
		}
		String dir = getParent(path);
		Map<String, Resource> s = directories.get(dir);
		if (s == null) {
			s = new TreeMap<>();
			directories.put(dir, s);
			// make ancestor directories
			for (int n; (n = dir.lastIndexOf('/')) > 0;) {
				dir = dir.substring(0, n);
				if (directories.containsKey(dir))
					break;
				directories.put(dir, null);
			}
		}
		boolean duplicate = s.containsKey(path);
		if (!duplicate || overwrite) {
			resources.put(path, resource);
			s.put(path, resource);
			jar.updateModified(resource.lastModified(), getFullPath(path));
		}
		return duplicate;
	}

	NavigableMap<String, Map<String, Resource>> getDirectories() {
		return directories;
	}

	Resource remove(String path) {
		Resource resource = resources.remove(path);
		if (resource != null) {
			String dir = getParent(path);
			Map<String, Resource> mdir = directories.get(dir);
			// must be != null
			mdir.remove(path);
		}
		return resource;
	}

	void removePrefix(String prefixLow) {
		String prefixHigh = prefixLow.concat("\uFFFF");
		resources.subMap(prefixLow, prefixHigh)
			.clear();
		if (prefixLow.endsWith("/")) {
			prefixLow = prefixLow.substring(0, prefixLow.length() - 1);
			prefixHigh = prefixLow.concat("\uFFFF");
		}
		directories.subMap(prefixLow, prefixHigh)
			.clear();
	}

	void removeSubDirs(String dir) {
		if (!dir.endsWith("/")) {
			dir = dir.concat("/");
		}
		List<String> subDirs = new ArrayList<>(directories.subMap(dir, dir.concat("\uFFFF"))
			.keySet());
		subDirs.forEach(subDir -> removePrefix(subDir.concat("/")));
	}

	boolean rename(String oldPath, String newPath) {
		Resource resource = remove(oldPath);
		if (resource == null) {
			return false;
		}
		return putResource(newPath, resource, true);
	}

	boolean isEmpty() {
		return resources.isEmpty();
	}

	boolean exists(String path) {
		return resources.containsKey(path);
	}

	Optional<Manifest> manifest() {
		Optional<Manifest> optional = manifest;
		if (optional != null) {
			return optional;
		}
		try {
			Resource manifestResource = getResource(jar.manifestName);
			if (manifestResource == null) {
				return manifest = Optional.empty();
			}
			try (InputStream in = manifestResource.openInputStream()) {
				return manifest = Optional.of(new Manifest(in));
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void setManifest(Manifest manifest) {
		this.manifest = Optional.ofNullable(manifest);
	}

	Optional<ModuleAttribute> moduleAttribute() {
		Optional<ModuleAttribute> optional = moduleAttribute;
		if (optional != null) {
			return optional;
		}
		Resource module_info_resource = getResource(Constants.MODULE_INFO_CLASS);
		if (module_info_resource == null) {
			return moduleAttribute = Optional.empty();
		}
		try {
			ClassFile module_info;
			ByteBuffer bb = module_info_resource.buffer();
			if (bb != null) {
				module_info = ClassFile.parseClassFile(ByteBufferDataInput.wrap(bb));
			} else {
				try (DataInputStream din = new DataInputStream(module_info_resource.openInputStream())) {
					module_info = ClassFile.parseClassFile(din);
				}
			}
			return moduleAttribute = Arrays.stream(module_info.attributes)
				.filter(ModuleAttribute.class::isInstance)
				.map(ModuleAttribute.class::cast)
				.findFirst();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	Resource getResource(String path) {
		return resources.get(path);
	}

	NavigableMap<String, Resource> getResources() {
		return resources;
	}

	String getFullPath(String basePath) {
		if (release == NO_RELEASE) {
			return basePath;
		}
		return String.format("META-INF/versions/%d/%s", release, basePath);
	}

	boolean hasDirectory(String path) {
		return directories.containsKey(path);
	}

	private String getParent(String path) {
		jar.check();
		int n = path.lastIndexOf('/');
		if (n < 0)
			return "";

		return path.substring(0, n);
	}

	public void close() {
		resources.values()
			.forEach(IO::close);
		resources.clear();
		directories.clear();
		manifest = null;
	}

}
