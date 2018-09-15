package test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Collection;

import aQute.bnd.make.calltree.CalltreeResource;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Resource;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class CalltreeTest extends TestCase {
	@SuppressWarnings("restriction")
	public static void testCalltree() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Private-Package", "test");
		b.build();
		Collection<Clazz> clazzes = b.getClassspace()
			.values();
		assertTrue(clazzes.size() > 10);

		CalltreeResource.writeCalltree(pw, clazzes);
		pw.close();
		System.err.println(sw.toString());
	}

	static class Implements implements Resource {

		@Override
		public String getExtra() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long lastModified() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public InputStream openInputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setExtra(String extra) {
			// TODO Auto-generated method stub

		}

		@Override
		public void write(OutputStream out) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public long size() throws Exception {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ByteBuffer buffer() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void close() throws IOException {}
	}

	// public void testCoverage() throws Exception {
	// Builder b = new Builder();
	// b.addClasspath(new File("bin_test"));
	// b.setProperty("Private-Package", "test");
	// b.build();
	// Collection<Clazz> testsuite = b.getClassspace().values();
	//
	// Builder c = new Builder();
	// c.addClasspath(new File("bin_test"));
	// c.setProperty("Private-Package", "aQute.bnd.osgi");
	// c.build();
	// Collection<Clazz> target = c.getClassspace().values();
	//
	// Map<Clazz.MethodDef, List<Clazz.MethodDef>> xref =
	// Coverage.getCrossRef(testsuite, target);
	// System.err.println(xref);
	//
	// List<Clazz.MethodDef> refs = xref.get(new Clazz.MethodDef(0,
	// Resource.class.getName(),
	// "write", "(Ljava/io/OutputStream;)V"));
	//
	// assertNotNull("The write(OutputStream) method is implemented by
	// Resource",
	// xref);
	//
	// assertTrue("We must have at least one reference", refs.size() > 0);
	// // MethodDef md = refs.get(0);
	// boolean found = false;
	// for (MethodDef md : refs) {
	// if (md.clazz.equals("test.CalltreeTest$Implements")) {
	// found = true;
	// assertEquals(md.name, "<implements>");
	// assertEquals(md.descriptor, "()V");
	// }
	// }
	// assertTrue( found);
	//
	// assertTrue(xref.containsKey(new Clazz.MethodDef(0,
	// Analyzer.class.getName(), "analyze",
	// "()V")));
	// assertTrue(xref.containsKey(new Clazz.MethodDef(0,
	// Builder.class.getName(), "build",
	// "()LaQute/lib/osgi/Jar;")));
	// assertTrue(xref
	// .get(
	// new Clazz.MethodDef(0, "aQute.bnd.osgi.Builder", "build",
	// "()LaQute/lib/osgi/Jar;")).contains(
	// new Clazz.MethodDef(0, "test.CalltreeTest", "testCoverage", "()V")));
	//
	// Tag tag = CoverageResource.toTag(xref);
	// PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.err));
	// tag.print(0, pw);
	// pw.close();
	// }
	//
	// public void testCatalog() throws Exception {
	// Builder b = new Builder();
	// b.addClasspath(IO.getFile("jar/osgi.jar"));
	// b.setProperty("Private-Package", "org.osgi.framework");
	// b.build();
	// Collection<Clazz> target = b.getClassspace().values();
	//
	// Map<MethodDef, List<MethodDef>> xref = Coverage.getCrossRef(new
	// ArrayList<Clazz>(), target);
	//
	// System.err.println(Processor.join(xref.keySet(), "\n"));
	//
	// assertTrue(xref.containsKey(new Clazz.MethodDef(0,
	// "org.osgi.framework.AdminPermission",
	// "<init>", "()V")));
	// assertTrue(xref.containsKey(new Clazz.MethodDef(0,
	// "org.osgi.framework.AdminPermission",
	// "<init>", "(Lorg/osgi/framework/Bundle;Ljava/lang/String;)V")));
	// assertTrue(xref.containsKey(new Clazz.MethodDef(0,
	// "org.osgi.framework.AdminPermission",
	// "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")));
	// assertFalse(xref.containsKey(new Clazz.MethodDef(0,
	// "org.osgi.framework.AdminPermission$1",
	// "run", "()Ljava/lang/Object;")));
	// assertFalse(xref.containsKey(new Clazz.MethodDef(0,
	// "org.osgi.framework.AdminPermission",
	// "createName", "(Lorg/osgi/framework/Bundle;)Ljava/lang/String;")));
	// }
	//
	// public void testResource() throws Exception {
	// Builder b = new Builder();
	// b.addClasspath(IO.getFile("jar/osgi.jar"));
	// b.setProperty("Private-Package", "org.osgi.framework");
	// b.build();
	// Collection<Clazz> target = b.getClassspace().values();
	//
	// Resource r = new CoverageResource(target, target);
	// InputStream in = r.openInputStream();
	// ByteArrayOutputStream out = new ByteArrayOutputStream();
	// byte[] buffer = new byte[1000];
	// int size = in.read(buffer);
	// while (size > 0) {
	// out.write(buffer, 0, size);
	// size = in.read(buffer);
	// }
	// out.close();
	// assertTrue(out.toByteArray().length > 1000);
	// }

}
