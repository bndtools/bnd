package test;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

public class ResourcesTest extends TestCase {

	/**
	 * If a name starts with a - sign then it is ok if it does not exist. The -
	 * sign must be skipped obviously.
	 * 
	 * @throws Exception
	 */
	public static void testAbsentIsOk() throws Exception {
		{
			Builder b = new Builder();
			b.setProperty("Include-Resource", "TargetFolder=-test/ws/p2/Resources");
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
			b.setProperty("Include-Resource", "-test/ws/p2/Resources");
			b.setProperty("-resourceonly", "true");
			Jar jar = b.build();
			assertTrue(b.check());
			Resource r = jar.getResource("resource1.res");
			assertNotNull(r);
		}

	}

	public static void testNegativeFilter() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=test/ws/p2/Resources;filter:=!*.txt");
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
		p.setProperty("Include-Resource", "/=src/test/activator");
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
		b.setProperty("Include-Resource", "TargetFolder=test/ws/p2/Resources");
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
		p.put("Include-Resource", "TargetFolder=test/ws/p2/Resources");
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
		b.setProperty("Include-Resource", "TargetFolder=test/ws/p2/Resources;filter:=re*.txt");
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
		b.setProperty("Include-Resource", "test/ws/p2/Resources;filter:=re*.txt");
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
		b.setProperty("Include-Resource", "TargetFolder=test/ws/p2/Resources;filter:=re*.txt;recursive:=false");
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
		b.setProperty("Include-Resource", "TargetFolder=test/ws/p2/Resources;filter:=re*.txt;flatten:=true");
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
		p.setProperty("Include-Resource", "  ");
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertEquals(0, jar.getResources().size());
		assertTrue(bmaker.check("The JAR is empty"));
	}

	public static void testLiteral() throws Exception {
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "text;literal=TEXT;extra='hello/world;charset=UTF-8'");
		bmaker.setProperties(p);
		bmaker.setClasspath(new String[] {
			"src"
		});
		Jar jar = bmaker.build();
		Resource resource = jar.getResource("text");
		assertNotNull(resource);
		byte buffer[] = new byte[1000];
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
		p.setProperty("Include-Resource", "ondemand.jar");
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
		b.setProperty("Include-Resource", "hello/world/<<EMPTY>>;literal=''");
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
		new File("test/ws/p2/Resources/empty").mkdirs();
		Builder b = new Builder();
		b.setProperty("Include-Resource", "TargetFolder=test/ws/p2/Resources");
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
