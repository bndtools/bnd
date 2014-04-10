package test;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import junit.framework.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

@SuppressWarnings("resource")
public class ResourcesTest extends TestCase {
	static final int BUFFER_SIZE = IOConstants.PAGE_SIZE * 1;

	/**
	 * Command facility in Include-Resource
	 */
	
	public void testCommand() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "xkeystore; requires='testresources/keystore';cmd='file ${@requires}', ");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		assertTrue(b.check());
		Resource r = jar.getResource("xkeystore");
		assertNotNull(r);
		String s = IO.collect(r.openInputStream());
		assertEquals("testresources/keystore: Java KeyStore\n", s);
	}
	/**
	 * Test the Include-Resource facility to generate resources on the fly. This
	 * is a a case where multiple resources and up in a single combined
	 * resource.
	 */

	public static void testOnTheFlyMerge() throws Exception {
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

	public static void testOnTheFlySingle() throws Exception {
		// disable this test on windows
		if (!"/".equals(File.separator))
			return;

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

	public static void testOnTheFlySingleError() throws Exception {
		// disable this test on windows
		if (!"/".equals(File.separator))
			return;

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

	public static void testOnTheFlyMultiple() throws Exception {
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
	public static void testAbsentIsOk() throws Exception {
		{
			Builder b = new Builder();
			b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=-testresources/ws/p2/Resources");
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
			b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=-doesnotexist, text;literal='Hello'");
			b.setProperty("-resourceonly", "true");
			b.build();
			assertTrue(b.check());
		}

		{
			Builder b = new Builder();
			b.setProperty(Constants.INCLUDE_RESOURCE, "-doesnotexist, text;literal='Hello'");
			b.setProperty("-resourceonly", "true");
			b.build();
			assertTrue(b.check());
		}

		{
			Builder b = new Builder();
			b.setProperty(Constants.INCLUDE_RESOURCE, "-testresources/ws/p2/Resources");
			b.setProperty("-resourceonly", "true");
			Jar jar = b.build();
			assertTrue(b.check());
			Resource r = jar.getResource("resource1.res");
			assertNotNull(r);
		}

	}

	public static void testNegativeFilter() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources;filter:=!*.txt");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		Resource r = jar.getResource("TargetFolder/resource1.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource2.res");
		assertNotNull(r);
		r = jar.getResource("TargetFolder/resource5.asc");
		assertNotNull(r);
	}

	public static void testCopyToRoot() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty(Constants.INCLUDE_RESOURCE, "/=src/test/activator");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		for (String s : jar.getResources().keySet())
			System.err.println(s);
		assertNotNull(jar.getResource("Activator.java"));
		assertEquals(0, bmaker.getErrors().size());
		assertEquals(0, bmaker.getWarnings().size());
	}

	public static void testIncludeResourceDirectivesDefault() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources");
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

	public static void testIncludeResourceDoNotCopy() throws Exception {
		Builder b = new Builder();

		// Use Properties file otherwise -donotcopy is not picked up
		Properties p = new Properties();
		p.put("-donotcopy", "CVS|.svn|stuff");
		p.put(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources");
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

	public static void testIncludeResourceDirectivesFilterRecursive() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources;filter:=re*.txt");
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

	public static void testIncludeResourceDirectivesFilterRecursive2() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "testresources/ws/p2/Resources;filter:=re*.txt");
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

	public static void testIncludeResourceDirectivesFilterNonRecursive() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources;filter:=re*.txt;recursive:=false");
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

	public static void testIncludeResourceDirectivesFilterRecursiveFlatten() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources;filter:=re*.txt;flatten:=true");
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

	public static void testEmpty() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty(Constants.INCLUDE_RESOURCE, "  ");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertEquals(0, jar.getResources().size());
		assertTrue(bmaker.check("The JAR is empty"));
	}

	public static void testLiteral() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty(Constants.INCLUDE_RESOURCE, "text;literal=TEXT;extra='hello/world;charset=UTF-8'");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"src"
		});
		Jar jar = bmaker.build();
		Resource resource = jar.getResource("text");
		assertNotNull(resource);
		byte buffer[] = new byte[BUFFER_SIZE];
		int size = resource.openInputStream().read(buffer);
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
	public static void testOnDemandResource() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("-plugin", "aQute.bnd.make.MakeBnd, aQute.bnd.make.MakeCopy");
		p.setProperty("-make", "(*).jar;type=bnd;recipe=bnd/$1.bnd");
		p.setProperty(Constants.INCLUDE_RESOURCE, "ondemand.jar");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"bin"
		});
		Jar jar = bmaker.build();
		Resource resource = jar.getResource("ondemand.jar");
		assertNotNull(resource);
		assertTrue(bmaker.check());
		assertTrue(resource instanceof JarResource);
		report(bmaker);

	}

	public void testEmptyDirs() throws Exception {
		Builder b = new Builder();
		b.setProperty("-resourceonly", "true");
		b.setProperty(Constants.INCLUDE_RESOURCE, "hello/world/<<EMPTY>>;literal=''");
		Jar jar = b.build();
		Map<String, Map<String, Resource>> directories = jar.getDirectories();
		assertTrue(directories.containsKey("hello/world"));
//		report(b); //error due to empty literal
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		jar.write(baos);
		byte[] contents = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(contents);
		ZipInputStream zis = new ZipInputStream(bais);
		boolean hasDir = false;
		boolean hasContent = false;
		ZipEntry ze = zis.getNextEntry();
		while (null != ze) {
			if (ze.getName().equals("hello/world/") && ze.isDirectory())
				hasDir = true;
			if (ze.getName().startsWith("hello/world/") && ze.getName().length() > "hello/world/".length())
				hasContent = true;
			ze = zis.getNextEntry();
		}
		assertTrue(hasDir);
		assertFalse(hasContent);
	}

	public void testEmptyDirs2() throws Exception {
		File tstDir = new File("testresources/ws/p2/Resources/empty");
		tstDir.mkdirs();
		Builder b = new Builder();
		b.setProperty(Constants.INCLUDE_RESOURCE, "TargetFolder=testresources/ws/p2/Resources");
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
			if (ze.getName().equals("TargetFolder/empty/") && ze.isDirectory())
				hasDir = true;
			if (ze.getName().startsWith("TargetFolder/empty/") && ze.getName().length() > "TargetFolder/empty/".length())
				hasContent = true;
			ze = zis.getNextEntry();
		}
		assertTrue(hasDir);
		assertFalse(hasContent);
		IO.delete(tstDir);
	}


static void report(Processor processor) {
		System.err.println();
		for (int i = 0; i < processor.getErrors().size(); i++)
			System.err.println(processor.getErrors().get(i));
		for (int i = 0; i < processor.getWarnings().size(); i++)
			System.err.println(processor.getWarnings().get(i));
		assertEquals(0, processor.getErrors().size());
		assertEquals(0, processor.getWarnings().size());
	}
}
