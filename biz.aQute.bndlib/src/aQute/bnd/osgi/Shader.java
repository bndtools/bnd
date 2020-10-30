package aQute.bnd.osgi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.lib.exceptions.Exceptions;
import aQute.libg.cryptography.SHA1;

/**
 * Shade a JAR. This is renaming packages that match the specification. The
 * default rename is a JAR wide random prefix but the specification is a
 * selector and the 'prefix' attribute is the prefix if set. Shading will rename
 * the path of any resource in the matching packages. For any classes inside the
 * JAR, they will be processed to use the renamed classes.
 * <p>
 * The spec is a normal selector where the key is a binary path.
 *
 * <pre>
 * 	-shade = !org.osgi.framewor.util.*, org.osgi.framework.*;prefix=foo
 * </pre>
 */
public class Shader {
	static Random random = new Random();

	/**
	 * Shade the jar. This can modify the jar. This will rename all classes
	 * according to the parameters. If a bundle id is given, then the default
	 * package rename is to a string that is repeatable between builds. It will
	 * use a unique string for a renamed package that is based on the bsn,
	 * version-qualifier, and the package name.
	 *
	 * @param jar the jar to rename the classes in.
	 * @param parameters the specification
	 * @param id null or a bundle id
	 */
	public static Map<String, String> shade(Jar jar, Parameters parameters, BundleId id) throws Exception {

		Map<String, String> hashes = new HashMap<>();

		Instructions shading = buildInstructions(parameters);

		// for repeatability we need a seed that is the same for the same bundle
		// + version. If no id is given, we calculate a random one.

		String rseed;

		if (id == null) {
			int rnum = random.nextInt();
			int abs = Math.abs(rnum);
			rseed = Integer.toString(abs);
		} else {
			rseed = Integer.toString(id.getBsn()
				.hashCode()
				^ id.getShortVersion() // no date
					.hashCode());
		}

		Function<String, String> mapper = mapper(shading, rseed, hashes);

		for (Map.Entry<String, Resource> e : jar.getResources()
			.entrySet()) {
			doEntry(jar, mapper, e);
		}

		renameEntries(jar, mapper);
		return hashes;
	}

	private static Instructions buildInstructions(Parameters parameters) {
		Instructions shading = new Instructions();
		for (Entry<String, Attrs> entry : parameters.entrySet()) {
			String path = entry.getKey()
				.replace('.', '/');
			shading.put(new Instruction(path), entry.getValue());
		}
		return shading;
	}

	private static Function<String, String> mapper(Instructions shading, String random, Map<String, String> hashes) {
		Function<String, String> mapper = original -> {

			if (original.startsWith("OSGI-OPT/src/")) {
				String src = map(shading, random, hashes, original.substring(13));
				if (src == null)
					return null;
				return "OSGI-OPT/src/".concat(src);
			}

			return map(shading, random, hashes, original);
		};
		return mapper;
	}

	private static String map(Instructions shading, String random, Map<String, String> hashes, String original) {
		int n = original.lastIndexOf('/');
		if (n < 0)
			return original;
		String cname = original.substring(n + 1);
		String pname = original.substring(0, n + 1);

		String result;

		Instruction matcher = shading.matcher(pname);
		if (matcher != null) {
			if (matcher.isNegated())
				return original;

			String prefix = shading.get(matcher)
				.get(Constants.SHADE_PREFIX_DIRECTIVE);

			if (prefix != null) {
				prefix = prefix.replace('.', '/');
				if (!prefix.endsWith("/")) {
					prefix = prefix + "/";
				}

				result = prefix + original;
			} else {
				prefix = hashes.computeIfAbsent(pname, k -> prefix(random, pname));
				result = prefix + cname;
			}
		} else
			result = null;

		return result;
	}

	private static String prefix(String bsn, String pname) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("x")
				.append(SHA1.digest(bsn.concat(pname)
					.getBytes(StandardCharsets.UTF_8))
					.asHex())
				.setLength(9);

			return sb.append('/')
				.toString();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static void doEntry(Jar jar, Function<String, String> mapper, Map.Entry<String, Resource> e)
		throws IOException, Exception {
		String path = e.getKey();
		Resource resource = e.getValue();

		if (path.endsWith(".class")) {
			doClass(jar, mapper, path, resource);
		}
	}

	private static void doClass(Jar jar, Function<String, String> mapper, String path, Resource resource)
		throws IOException, Exception {
		ClassFile cf = ClassFile.parseInputStream(resource.openInputStream());
		Optional<ClassFile> renamed = cf.rename(mapper);
		if (renamed.isPresent()) {
			resource = new EmbeddedResource(renamed.get()
				.toBytes(), resource.lastModified());
			jar.putResource(path, resource);
		}
	}

	private static void renameEntries(Jar jar, Function<String, String> mapper) {
		new HashSet<>(jar.getResources()
			.keySet()).forEach(path -> {
				String newPath = mapper.apply(path);
				if (newPath != null)
					jar.rename(path, newPath);
			});

	}

}
