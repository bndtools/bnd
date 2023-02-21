package aQute.bnd.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.exceptions.Exceptions;

/**
 * Multi Release jars are another error magnet extension to Java. Instead of
 * having the flat class space of Java and Javac, it allows developers to put
 * multiple versions of a class in versioned directory.
 * <p>
 *
 * @see "https://docs.oracle.com/en/java/javase/19/docs/specs/jar/jar.html#multi-release-jar-files"
 *      <p>
 */
public class MultiReleaseJars {
	final static Pattern VERSIONED_P = Pattern.compile("META-INF/versions/(?<release>\\d+)/(?<path>.*)");

	/**
	 * Check if a JAR is a multi release jar.
	 *
	 * @param jar the jar
	 * @return true if this is a multi release jar
	 */
	public static boolean isMulti(Jar jar) {
		try {
			Domain domain = Domain.domain(jar.getManifest());
			return Processor.isTrue(domain.get(Constants.MULTI_RElEASE));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * bnd, like javac and java, require a _flat_ namespace. Many invariants in
	 * the language just fail if this is not the case. In this case we take a
	 * Jar and create a new Jar that is a view on the old jar.
	 * <p>
	 * If this is not a Multi Release jar, return the original jar. Otherwise,
	 * return a flattened version for the given release. If the version is <9,
	 * flattening will happen for the highest version in the jar. The output jar
	 * will contain a flattened version for all release up to and including the
	 * given release. Versions of the same resource in a higher version will
	 * override lower versions.
	 * <p>
	 * For example if the jar contains release 8 in the root, versions 11,15,
	 * and 18 and the release is 15, the output will be the aggregate of 8,11,
	 * and 15. If both 8, 11, 15 have class Foo.class, then the 15 version will
	 * be in the output.
	 * <p>
	 * The flattening will use the same resources as the given Jar file.
	 * Therefore if this jar is closed, we will close the parent.
	 *
	 * @param jar the input jar
	 * @param release the highest to be included release number.
	 * @return the output jar or this jar.
	 */
	public static Jar view(Jar jar, int release) {
		if (!isMulti(jar))
			return jar;

		Jar out = new Jar(jar.getName()) {
			@Override
			public void close() {
				super.close();
				jar.close();
			}
		};

		if (release < 9) {
			release = Integer.MAX_VALUE;
		}

		List<String> names = new ArrayList<>(jar.getResources()
			.keySet());
		Collections.sort(names, (a,b)->{
			int n = a.startsWith("META-INF/versions/") ? 1 : 0;
			n += (b.startsWith("META-INF/versions/") ? 2 : 0);
			switch (n) {
				default :
				case 0 :
				case 3 :
					return a.compareTo(b);
				case 1 :
					return 1;
				case 2 :
					return -1;
			}
		});
		System.out.println(names);
		for (String name : names) {

			Resource resource = jar.getResource(name);

			Matcher m = VERSIONED_P.matcher(name);

			if (m.matches()) {
				int r = Integer.parseInt(m.group("release"));
				if (r > release)
					continue;
				out.putResource(m.group("path"), resource);
			} else
				out.putResource(name, resource);
		}
		return out;
	}

}
