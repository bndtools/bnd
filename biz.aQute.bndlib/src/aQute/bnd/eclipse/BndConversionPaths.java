package aQute.bnd.eclipse;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.strings.Strings;

public class BndConversionPaths {

	final List<String>				directories;
	final HashMap<String, String>	mapper;
	final String					key;
	final int						length;

	public BndConversionPaths(Processor ws, String key, String ifNoDefaultPath, String defaultMapping) {
		this.key = key;
		String mapper = ws.getProperty(key + ".mapping", defaultMapping);
		this.mapper = new HashMap<String, String>();

		for (Map.Entry<String, String> e : OSGiHeader.parseProperties(mapper).entrySet()) {
			String k = ensureDirectory(e.getKey());
			String v = ensureDirectory(e.getValue());
			this.mapper.put(k, v);
		}

		directories = Strings.split(ws.getProperty(key, ifNoDefaultPath))
				.stream()
				.map(this::ensureDirectory)
				.collect(Collectors.toList());
		length = directories.size();
	}

	String map(String fromDir) {
		fromDir = ensureDirectory(fromDir);
		if (directories.contains(fromDir))
			return fromDir;

		String toDir = mapper.get(fromDir);
		if (toDir != null) {
			if (directories.contains(toDir))
				return toDir;
		}

		return null;
	}

	public String ensureDirectory(String s) {
		if (!s.endsWith("/"))
			return s + "/";
		else
			return s;
	}

	public boolean has(String sourceDir) {
		sourceDir = ensureDirectory(sourceDir);

		return directories.contains(sourceDir) || mapper.containsKey(sourceDir);
	}

	public void move(Jar content, String sourceDir) {
		String toDir = map(sourceDir);
		if (toDir == null)
			toDir = directories.get(0);

		content.move(sourceDir, toDir);
	}

	public void addDirectory(String sourceDir) {
		directories.add(sourceDir);
	}

	public void update(Formatter model) {
		try (Formatter sub = new Formatter()) {
			String del = "${^" + key + "}, ";
			for (int i = length; i < directories.size(); i++) {
				sub.format("%s%s", del, directories.get(i));
				del = ", ";
			}
			if (sub.toString().length() != 0)
				model.format(EclipseManifest.HEADER_FORMAT, key, sub.toString());
		}
	}

	public void remove(Jar content, String string) {
		Map<String, Resource> resources = content.getResources();
		for (String dir : directories) {
			String path = dir + string;
			if (resources.containsKey(path)) {
				content.remove(path);
			}
		}
	}

	public Set<String> getRelative(Set<String> paths) {
		Set<String> relative = new TreeSet<>();
		for (String dir : directories) {
			for (String path : paths) {
				if (path.startsWith(dir)) {
					relative.add(path.substring(dir.length()));
				}
			}
		}
		return relative;
	}

}
