package test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;

@SuppressWarnings("resource")
public class ResourcesTest {
	static final int BUFFER_SIZE = IOConstants.PAGE_SIZE * 1;
	private Path		tmp;

	@BeforeEach
	public void before(TestInfo info) throws Exception {
		Method testMethod = info.getTestMethod()
			.get();
		tmp = Paths.get("generated/tmp/test", getClass().getName(), testMethod.getName())
			.toAbsolutePath();
		IO.delete(tmp);
		IO.mkdirs(tmp);
	}

	/**
	 * Command facility in Include-Resource
	 */

	@Test
	@DisabledOnOs(WINDOWS)
	public void testCommand() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "xkeystore; requires='testresources/keystore';cmd='echo ${@requires}', ");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		assertTrue(b.check());
		Resource r = jar.getResource("xkeystore");
		assertNotNull(r);
		String s = IO.collect(r.openInputStream());
		assertEquals("testresources/keystore\n", s);
	}

	/**
	 * Test the Include-Resource facility to generate resources on the fly. This
	 * is a a case where multiple resources and up in a single combined
	 * resource.
	 */

	@Test
	@DisabledOnOs(WINDOWS)
	public void testOnTheFlyMerge() throws Exception {
		Builder b = new Builder();
		b.setIncludeResource("count;for='1,2,3';cmd='echo YES_${@}'");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		assertTrue(b.check());
		Resource r = jar.getResource("count");
		assertNotNull(r);

		String s = IO.collect(r.openInputStream());
		assertEquals("YES_1\nYES_2\nYES_3\n", s);
		b.close();
	}

	/**
	 * Test the Include-Resource facility to generate resources on the fly. This
	 * is a simple case of one resource.
	 */

	@Test
	@DisabledOnOs(WINDOWS)
	public void testOnTheFlySingle() throws Exception {
		Builder b = new Builder();
		b.setIncludeResource("testresources/ls;cmd='ls /etc | grep hosts'");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		assertTrue(b.check());
		Resource r = jar.getResource("testresources/ls");
		assertNotNull(r);
		String s = IO.collect(r.openInputStream());
		assertTrue(s.contains("hosts"));
	}

	/**
	 * Test the Include-Resource facility to generate resources on the fly. This
	 * is a simple case of one resource with an error.
	 */

	@Test
	@DisabledOnOs(WINDOWS)
	public void testOnTheFlySingleError() throws Exception {
		Builder b = new Builder();
		b.setIncludeResource("testresources/x;cmd='I do not exist!!!!!!!!!!!'");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		assertTrue(b.check("Cmd 'I do not exist!!!!!!!!!!!' failed"));
		b.close();
	}

	/**
	 * Test the Include-Resource facility to generate resources on the fly. This
	 * is a a case where multiple resources and up in a single combined
	 * resource.
	 */

	@Test
	@DisabledOnOs(WINDOWS)
	public void testOnTheFlyMultiple() throws Exception {
		Builder b = new Builder();
		b.setIncludeResource("count/${@};for='1,2,3';cmd='echo YES_${@}'");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		assertTrue(b.check());
		assertNotNull(jar.getResource("count/1"));
		Resource r = jar.getResource("count/2");
		assertNotNull(jar.getResource("count/3"));

		String s = IO.collect(r.openInputStream());
		assertEquals("YES_2\n", s);
	}

	/**
	 * If a name starts with a - sign then it is ok if it does not exist. The -
	 * sign must be skipped obviously.
	 *
	 * @throws Exception
	 */
	@Test
	public void testAbsentIsOk() throws Exception {
		{
			Builder b = new Builder();
			b.setProperty("Include-Resource", "TargetFolder=-testresources/ws/p2/Resources");
			b.setProperty("-resourceonly", "true");
			Jar jar = b.build();
			assertTrue(b.check());
			Resource r = jar.getResource("TargetFolder/resource1.res");
			assertNotNull(r);
			r = jar.getResource("TargetFolder/resource2.res");
			assertNotNull(r);
			r = jar.getResource("TargetFolder/resource5.asc");
			assertNotNull(r);
		}

		{
			Builder b = new Builder();
			b.setProperty("Include-Resource", "TargetFolder=-doesnotexist, text;literal='Hello'");
			b.setProperty("-resourceonly", "true");
			b.build();
			assertTrue(b.check());
		}

		{
			Builder b = new Builder();
			b.setProperty("Include-Resource", "-doesnotexist, text;literal='Hello'");
			b.setProperty("-resourceonly", "true");
			b.build();
			assertTrue(b.check());
		}

		{
			Builder b = new Builder();
			b.setProperty("Include-Resource", "-testresources/ws/p2/Resources");
			b.setProperty("-resourceonly", "true");
			Jar jar = b.build();
			assertTrue(b.check());
			Resource r = jar.getResource("resource1.res");
			assertNotNull(r);
		}

	}

	@Test
	public void testNegativeFilter() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=testresources/ws/p2/Resources;filter:=!*.txt");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource1.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource2.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource5.asc");
		assertNotNull(r);
	}

	@Test
	public void testCopyToRoot() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "/=test/test/activator");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		for (String s : jar.getResources()
			.keySet())
			System.err.println(s);
		assertNotNull(jar.getResource("Activator.java"));
		assertEquals(0, bmaker.getErrors()
			.size());
		assertEquals(0, bmaker.getWarnings()
			.size());
	}

	@Test
	public void testIncludeResourceDirectivesDefault() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=testresources/ws/p2/Resources");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource6.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource7.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/stuff/resource9.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/text.txt");
		assertNotNull(r);

	}

	@Test
	public void testIncludeResourceDoNotCopy() throws Exception {
		Builder b = new Builder();

		// Use Properties file otherwise -donotcopy is not picked up
		Properties p = new Properties();
		p.put("-donotcopy", "CVS|.svn|stuff");
		p.put("Include-Resource", "TargetFolder=testresources/ws/p2/Resources");
		p.put("-resourceonly", "true");
		b.setProperties(p);

		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource6.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource7.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/stuff/resource9.res");
		assertNull(r);
		r = jar.getResource("TargetFolder/text.txt");
		assertNotNull(r);

	}

	@Test
	public void testIncludeResourceDoNotCopyPath() throws Exception {
		Builder b = new Builder();

		// Use Properties file otherwise -donotcopy is not picked up
		Properties p = new Properties();
		p.put("-donotcopy", ".*/more/.*");
		p.put("Include-Resource", "TargetFolder=testresources/ws/p2/Resources");
		p.put("-resourceonly", "true");
		p.put("-upto", "3.0");
		b.setProperties(p);

		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource6.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource7.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/stuff/resource9.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/text.txt");
		assertNotNull(r);

	}

	@Test
	public void testIncludeResourceDoNotCopyPath_Since_3_1() throws Exception {
		Builder b = new Builder();

		// Use Properties file otherwise -donotcopy is not picked up
		Properties p = new Properties();
		p.put("-donotcopy", ".*/more/.*");
		p.put("Include-Resource", "TargetFolder=testresources/ws/p2/Resources");
		p.put("-resourceonly", "true");
		b.setProperties(p);

		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource6.txt");
		assertNull(r);
		r = jar.getResource("TargetFolder/more/resource7.txt");
		assertNull(r);
		r = jar.getResource("TargetFolder/stuff/resource9.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/text.txt");
		assertNotNull(r);

	}

	@Test
	public void testIncludeResourceDirectivesFilterRecursive() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=testresources/ws/p2/Resources;filter:=re*.txt");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource6.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource7.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/text.txt");
		assertNull(r);

	}

	@Test
	public void testIncludeResourceDirectivesFilterRecursive2() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "testresources/ws/p2/Resources;filter:=re*.txt");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("resource3.txt");
		assertNotNull(r);
		r = jar.getResource("resource4.txt");
		assertNotNull(r);
		r = jar.getResource("more/resource6.txt");
		assertNotNull(r);
		r = jar.getResource("more/resource7.txt");
		assertNotNull(r);
		r = jar.getResource("text.txt");
		assertNull(r);

	}

	@Test
	public void testIncludeResourceDirectivesFilterNonRecursive() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource",
			"TargetFolder=testresources/ws/p2/Resources;filter:=re*.txt;recursive:=false");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/more/resource6.txt");
		assertNull(r);
		r = jar.getResource("TargetFolder/more/resource7.txt");
		assertNull(r);
	}

	@Test
	public void testIncludeResourceDirectivesFilterRecursiveFlatten() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=testresources/ws/p2/Resources;filter:=re*.txt;flatten:=true");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();

		Resource r = jar.getResource("TargetFolder/resource3.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource4.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource6.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource7.txt");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource1.res");
		assertNull(r);

	}

	@Test
	public void testEmpty() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "  ");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertEquals(0, jar.getResources()
			.size());
		assertTrue(bmaker.check("The JAR is empty"));
	}

	@Test
	public void testLiteral() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "text;literal=TEXT;extra='hello/world;charset=UTF-8'");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"test"
		});
		Jar jar = bmaker.build();
		Resource resource = jar.getResource("text");
		assertNotNull(resource);
		byte buffer[] = new byte[BUFFER_SIZE];
		int size = resource.openInputStream()
			.read(buffer);
		String s = new String(buffer, 0, size);
		assertEquals("TEXT", s);
		assertEquals("hello/world;charset=UTF-8", resource.getExtra());
		report(bmaker);

	}

	/**
	 * Check if we can create a jar on demand through the make facility.
	 *
	 * @throws Exception
	 */
	@Test
	public void testOnDemandResource() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
		p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
		p.setProperty("Include-Resource", "ondemand.jar");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"bin_test"
		});
		Jar jar = bmaker.build();
		Resource resource = jar.getResource("ondemand.jar");
		assertNotNull(resource);
		assertTrue(bmaker.check());
		assertTrue(resource instanceof JarResource);
		report(bmaker);

	}

	@Test
	public void testEmptyDirs() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "hello/world/<<EMPTY>>;literal=''");
		Jar jar = b.build();
		Map<String, Map<String, Resource>> directories = jar.getDirectories();
		assertTrue(directories.containsKey("hello/world"));
		// report(b); //error due to empty literal
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		jar.write(baos);
		byte[] contents = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(contents);
		ZipInputStream zis = new ZipInputStream(bais);
		boolean hasDir = false;
		boolean hasContent = false;
		ZipEntry ze = zis.getNextEntry();
		while (null != ze) {
			if (ze.getName()
				.equals("hello/world/") && ze.isDirectory())
				hasDir = true;
			if (ze.getName()
				.startsWith("hello/world/")
				&& ze.getName()
					.length() > "hello/world/".length())
				hasContent = true;
			ze = zis.getNextEntry();
		}
		assertTrue(hasDir);
		assertFalse(hasContent);
	}

	@Test
	public void testEmptyDirs2() throws Exception {
		File tstDir = IO.getFile("testresources/ws/p2/Resources/empty");
		tstDir.mkdirs();
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=testresources/ws/p2/Resources");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/empty/<<EMPTY>>");
		assertNotNull(r);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		jar.write(baos);
		byte[] contents = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(contents);
		ZipInputStream zis = new ZipInputStream(bais);
		boolean hasDir = false;
		boolean hasContent = false;
		ZipEntry ze = zis.getNextEntry();
		while (null != ze) {
			if (ze.getName()
				.equals("TargetFolder/empty/") && ze.isDirectory())
				hasDir = true;
			if (ze.getName()
				.startsWith("TargetFolder/empty/")
				&& ze.getName()
					.length() > "TargetFolder/empty/".length())
				hasContent = true;
			ze = zis.getNextEntry();
		}
		assertTrue(hasDir);
		assertFalse(hasContent);
		IO.delete(tstDir);
	}

	@Test
	public void testURLResourceJarLocking() throws Exception {
		File f = tmp.resolve("locking.jar")
			.toFile();
		try (Builder b = new Builder()) {
			b.setProperty("-includeresource", "TargetFolder=testresources/ws/p2/Resources");
			b.setProperty("-resourceonly", "true");
			Jar jar = b.build();

			f.getParentFile()
				.mkdirs();
			jar.write(f);
		}
		URL url = new URL("jar:" + f.toURI() + "!/TargetFolder/resource3.txt");

		try (Resource resource = Resource.fromURL(url)) {
			assertEquals("Resource3", IO.collect(resource.buffer(), UTF_8));
		}

		Files.delete(f.toPath());
	}

	static void report(Processor processor) {
		System.err.println();
		for (int i = 0; i < processor.getErrors()
			.size(); i++)
			System.err.println(processor.getErrors()
				.get(i));
		for (int i = 0; i < processor.getWarnings()
			.size(); i++)
			System.err.println(processor.getWarnings()
				.get(i));
		assertEquals(0, processor.getErrors()
			.size());
		assertEquals(0, processor.getWarnings()
			.size());
	}

}
