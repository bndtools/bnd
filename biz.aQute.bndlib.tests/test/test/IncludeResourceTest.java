package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

public class IncludeResourceTest {

	@Test
	public void testpreprocessing() throws IOException, Exception {
		String s = testPreprocessing("{foo.txt};literal='${sum;1,2,3}'", "foo.txt",
			"Preprocessing does not work for literals: foo.txt");
		s = testPreprocessing("foo.txt;literal='${sum;1,2,3}'", "foo.txt");
		assertEquals("6", s);

		s = testPreprocessing("{testresources/includeresource/root.txt}", "root.txt");
		assertEquals("6", s.trim());
	}

	/**
	 */
	@Test
	public void testIncludeResourceFromFileSystemDirectory() throws Exception {

		Set<String> filteredAuto = testResources("testresources/includeresource/;filter:=root.*;recurse:=false", 1);
		assertThat(filteredAuto).contains("root.txt");

		Set<String> flattened = testResources("testresources/includeresource/;flatten:=true", 4);
		assertThat(flattened).contains("root.txt", "a.txt", "b.txt", "c.txt");

		Set<String> filtered = testResources("testresources/includeresource/;filter:=c.*", 1);
		assertThat(filtered).contains("a/c/c.txt");

		Set<String> filteredAll = testResources("testresources/includeresource/;filter:=[abc].*", 3);
		assertThat(filteredAll).contains("a/a.txt", "b/b.txt", "a/c/c.txt");

		Set<String> filteredNot = testResources("testresources/includeresource/;filter:=![abc].*", 1);
		assertThat(filteredNot).contains("root.txt");

		Set<String> deflt = testResources("testresources/includeresource", 4);
		assertThat(deflt).contains("root.txt", "a/a.txt", "b/b.txt", "a/c/c.txt");

		Set<String> withSlash = testResources("testresources/includeresource/", 4);

		Set<String> rename = testResources("x=testresources/includeresource", 4);
		assertThat(rename).contains("x/root.txt", "x/a/a.txt", "x/b/b.txt", "x/a/c/c.txt");

		Set<String> renameWithSlash = testResources("x=testresources/includeresource/", 4);
		assertThat(rename).contains("x/root.txt", "x/a/a.txt", "x/b/b.txt", "x/a/c/c.txt");

		Set<String> renameWithTwoSlashes = testResources("x/=testresources/includeresource/", 4);
		assertThat(rename).contains("x/root.txt", "x/a/a.txt", "x/b/b.txt", "x/a/c/c.txt");

	}

	@Test
	public void testIncludeResourceFromFileSystemFile() throws Exception {

		Set<String> deflt = testResources("testresources/includeresource/root.txt", 1);
		assertThat(deflt).contains("root.txt");

		Set<String> withSlash = testResources("testresources/includeresource/a/c/c.txt", 1);
		assertThat(withSlash).contains("c.txt");

		Set<String> renameWithSlash = testResources("x/=testresources/includeresource/a/c/c.txt", 1);
		assertThat(renameWithSlash).contains("x/c.txt");

	}

	@Test
	public void testIncludeResourceFromZip() throws Exception {

		Set<String> deflt = testResources("@jar/osgi.jar", 528);
		Set<String> noRecurse = testResources("@jar/osgi.jar!/*", 528);
		Set<String> include = testResources("@jar/osgi.jar!/org/osgi/framework/*", 31);

		Set<String> exclude = testResources("@jar/osgi.jar!/!org/osgi/framework/*", 528 - 31);

		Set<String> includeOneFile = testResources("@jar/osgi.jar!/LICENSE", 1);
		Set<String> includeOneFile2 = testResources("'@jar/osgi.jar!/LICENSE|about.html'", 2);
		Set<String> excludeOneFile = testResources("@jar/osgi.jar!/!LICENSE", 528 - 1);

		Set<String> or = testResources("@jar/osgi.jar!/LICENSE|about.html", 2);
		Set<String> orDirectory = testResources("@jar/osgi.jar!/LICENSE|about.html|org/*", 261);
		Set<String> notOrDirectory = testResources("@jar/osgi.jar!/!LICENSE|about.html|org/*", 528 - 261);

		Set<String> framework = testResources("@jar/osgi.jar!/*/framework/*", 58);
		Set<String> frameworkSource = testResources("@jar/osgi.jar!/*/framework/*.java", 25);

		Set<String> parentheses = testResources("@jar/osgi.jar!/!(LICENSE|about.html|org/*)", 528 - 261);
		Set<String> parenthesesNoWildcard = testResources("@jar/osgi.jar!/(LICENSE)", 1);

		Set<String> curliesOptional = testResources("'@jar/osgi.jar!/(LICENSE){0,1}'", 1);
		Set<String> curliesOnce = testResources("'@jar/osgi.jar!/(LICENSE){1}'", 1);
		Set<String> curliesTwice = testResources("'@jar/osgi.jar!/(*framework){2}*.java:i'", 3);
		System.out.println(curliesTwice);
	}

	@Test
	public void testIncludeResourceFromUrl() throws Exception {
		URI uri = IO.getFile("jar/osgi.jar")
			.toURI();
		Set<String> deflt = testResources("@" + uri, 528);
		Set<String> includeOneFile = testResources("@" + uri + "!/LICENSE", 1);
		Set<String> excludeOneFile = testResources("@" + uri + "!/!LICENSE", 528 - 1);
	}

	@Test
	public void testIncludeResourceFromClasspathJars() throws Exception {
		Set<String> deflt = testResources("@osgi.jar", 528);
		Set<String> both = testResources("'@{osgi,easymock}.jar'", 528 + 59);
		Set<String> both2 = testResources("'@(osgi|easymock).jar'", 528 + 59);
		Set<String> both3 = testResources("'@(?:osgi|easymock).jar'", 528 + 59);
	}

	@Test
	public void testLiteral() throws Exception {
		Set<String> deflt = testResources("a.txt;literal='AAA'", 1);

	}

	private Set<String> testResources(String ir, int count, String... checks) throws Exception {

		try (Builder bmaker = new Builder()) {
			bmaker.addClasspath(bmaker.getFile("jar/osgi.jar"));
			bmaker.addClasspath(bmaker.getFile("jar/easymock.jar"));
			Properties p = new Properties();
			p.put("-includeresource", ir);
			bmaker.setProperties(p);
			Jar jar = bmaker.build();
			assertTrue(bmaker.check(checks));
			if (count > 0)
				assertEquals(count, jar.getResources()
					.keySet()
					.stream()
					.filter(n -> !n.endsWith(Constants.EMPTY_HEADER))
					.count());

			Set<String> keySet = new HashSet<>(jar.getResources()
				.keySet());
			// System.out.println(keySet);
			return keySet;
		}
	}

	private String testPreprocessing(String ir, String resource, String... checks) throws IOException, Exception {
		try (Builder bmaker = new Builder()) {
			bmaker.setProperty("-resourcesonly", "true");
			bmaker.addClasspath(bmaker.getFile("jar/osgi.jar"));
			bmaker.addClasspath(bmaker.getFile("jar/easymock.jar"));
			Properties p = new Properties();
			p.put("-includeresource", ir);
			bmaker.setProperties(p);
			Jar jar = bmaker.build();
			assertTrue(bmaker.check(checks));
			Resource resource2 = jar.getResource(resource);
			if (resource2 == null)
				return null;

			return IO.collect(resource2.openInputStream());
		}

	}
}
