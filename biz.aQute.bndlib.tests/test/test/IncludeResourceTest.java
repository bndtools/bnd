package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
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
	public void testIncludeResourceDirectoryToJar() throws Exception {
		Set<String> jar = testResources("foo.jar=@jar", 1);
		assertThat(jar).containsExactlyInAnyOrder("foo.jar");
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

	@Test
	public void testIncludeResourceDuplicatesDefaultOverwrite() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource("@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*");
			Jar jar = a.build();
			assertFalse(a.check());
			assertEquals(
				"includeresource.duplicates: Duplicate overwritten: META-INF/services/foo (Consider using the onduplicate: directive to handle duplicates.)",
				a.getWarnings()
					.get(0));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			// default should be "overwrite"
			assertEquals("b", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesDefaultOverwriteButNoWarningOnIdenticalFiles() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource("@jar/jarA.jar!/META-INF/services/*, @jar/jarA.jar!/META-INF/services/*");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			// default should be "overwrite"
			assertEquals("a", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesMerge() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:=MERGE");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("a\nb", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceMixedMetaInfDuplicatesMerge() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource("@jar/jarA.jar!/META-INF/*, @jar/jarB.jar!/META-INF/*;onduplicate:=MERGE");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resourceFoo = jar.getResource("META-INF/services/foo");
			assertEquals("a\nb", IO.collect(resourceFoo.openInputStream()));

			Resource resourceManifest = jar.getResource("META-INF/bar.txt");
			assertEquals("a", IO.collect(resourceManifest.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesMergeBlank() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			// dup_merge contains a blank value. should be ignored and use
			// default 'overwrite' behavior
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:=   ");
			Jar jar = a.build();
			assertFalse(a.check());
			assertEquals("No value after '=' sign for attribute onduplicate:", a.getErrors()
				.get(0));

		}
	}


	@Test
	public void testIncludeResourceDuplicatesError() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:=ERROR");
			Jar jar = a.build();
			assertFalse(a.check());
			assertEquals("includeresource.duplicates: duplicate found for path META-INF/services/foo", a.getErrors()
				.get(0));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("b", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesWarning() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:=WARN");
			Jar jar = a.build();
			assertFalse(a.check());
			assertEquals("includeresource.duplicates: duplicate found for path META-INF/services/foo", a.getWarnings()
				.get(0));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("b", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesOverwrite() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:=OVERWRITE");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("b", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesSkip() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:=SKIP");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("a", IO.collect(resource.openInputStream()));

		}
	}


	@Test
	public void testIncludeResourceLiteralDuplicatesMerge(@InjectTemporaryDirectory
	File tmp) throws Exception {

		try (Builder b = new Builder()) {
			b.setIncludeResource("/a/a.txt;literal='a', /a/a.txt;literal='b';onduplicate:=MERGE");
			b.build();
			assertTrue(b.check());

			b.getJar()
				.writeFolder(tmp);

			assertEquals("a", IO.collect(IO.getFile(tmp, "a/a.txt")));
		}
	}

	@Test
	public void testIncludeResourceLiteralMetaInfServicesDuplicatesMerge(@InjectTemporaryDirectory
	File tmp) throws Exception {

		try (Builder b = new Builder()) {
			b.setIncludeResource(
				"META-INF/services/a.txt;literal='a', META-INF/services/a.txt;literal='b';onduplicate:=MERGE");
			b.build();
			assertTrue(b.check());

			b.getJar()
				.writeFolder(tmp);

			assertEquals("a\nb", IO.collect(IO.getFile(tmp, "META-INF/services/a.txt")));
		}
	}

	@Test
	public void testIncludeResourceLiteralDuplicatesError(@InjectTemporaryDirectory
	File tmp) throws Exception {

		try (Builder b = new Builder()) {
			b.setIncludeResource("/a/a.txt;literal='a', /a/a.txt;literal='b';onduplicate:=ERROR");
			b.build();
			assertFalse(b.check());
			assertEquals("includeresource.duplicates: duplicate found for path /a/a.txt", b.getErrors()
				.get(0));

			b.getJar()
				.writeFolder(tmp);

			assertEquals("b", IO.collect(IO.getFile(tmp, "a/a.txt")));
		}
	}

	@Test
	public void testIncludeResourceDuplicatesMergeWithTag() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:='MERGE,metainfservices'");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("a\nb", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesMergeWithoutPlugin() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:='MERGE,nonexistingtag'");
			Jar jar = a.build();
			assertFalse(a.check());

			assertEquals("includeresource.duplicates: no plugins found for tags: [nonexistingtag]", a.getErrors()
				.get(0));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			// we expect nothing because there is no plugin with the tag
			// 'nonexistingtag'
			// which means we have nothing which can merge META-INF/services
			// files.
			// so we keep the existing file
			assertEquals("a", IO.collect(resource.openInputStream()));

		}

	}

	@Test
	public void testIncludeResourceDuplicatesTagWithoutStrategyEnumButExistingTag() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:='metainfservices'");
			Jar jar = a.build();
			assertTrue(a.check());

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("a\nb", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesTagWithoutStrategyEnumButNonExistingTag() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:='nonexistingtag'");
			Jar jar = a.build();
			assertFalse(a.check());

			assertEquals("includeresource.duplicates: no plugins found for tags: [nonexistingtag]", a.getErrors()
				.get(0));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			// we expect nothing because there is no plugin with the tag
			// 'nonexistingtag'
			// which means we have nothing which can merge META-INF/services
			// files.
			// so we keep the existing file
			assertEquals("a", IO.collect(resource.openInputStream()));

		}
	}

	@Test
	public void testIncludeResourceDuplicatesMergeWithTagAndWarn() throws Exception {

		try (Builder a = new Builder();) {
			a.addClasspath(new File("jar/jarA.jar"));
			a.addClasspath(a.getFile("jar/jarB.jar"));
			a.setIncludeResource(
				"@jar/jarA.jar!/META-INF/services/*, @jar/jarB.jar!/META-INF/services/*;onduplicate:='MERGE,metainfservices,WARN'");
			Jar jar = a.build();
			assertFalse(a.check());
			assertEquals("includeresource.duplicates: duplicate found for path META-INF/services/foo", a.getWarnings()
				.get(0));

			assertTrue(jar.getDirectories()
				.containsKey("META-INF/services"));

			Resource resource = jar.getResource("META-INF/services/foo");
			assertEquals("a\nb", IO.collect(resource.openInputStream()));

		}
	}

}
