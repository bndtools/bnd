package test.diff;

import java.io.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.diff.*;
import aQute.lib.io.*;

public class DiffTest extends TestCase {
	static DiffPluginImpl	differ	= new DiffPluginImpl();

	
	/**
	 * Test API differences. We have a package in the /demo workspace project and we have
	 * the same package in our test.api package. If you make changes, copy the demo.jar
	 * in the testresources directory.
	 * 
	 */
	
	public void testAPI() throws Exception {
		Jar older = new Jar(new File("testresources/demo.jar"));
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setExportPackage("test.api");
		b.build();
		assertTrue(b.check());
		
		Jar newer = b.getJar();
		Tree newerTree = differ.tree(newer).get("<api>").get("test.api").get("test.api.Interf");
		Tree olderTree = differ.tree(older).get("<api>").get("test.api").get("test.api.Interf");
		Diff diff = newerTree.diff(olderTree);
		
		show(diff, 2);
		assertEquals( Delta.MAJOR , diff.getDelta() );
		assertEquals( Delta.MAJOR , diff.get("foo()").getDelta() );
		assertEquals( Delta.UNCHANGED , diff.get("foo()").get("abstract").getDelta() );
		assertEquals( Delta.ADDED , diff.get("foo()").get("java.util.Collection<Ljava.lang.Integer;>").getDelta() );
		assertEquals( Delta.REMOVED , diff.get("foo()").get("java.util.Collection<Ljava.lang.String;>").getDelta() );
		assertEquals( Delta.UNCHANGED , diff.get("fooInt()").getDelta() );
		assertEquals( Delta.UNCHANGED , diff.get("fooString()").getDelta() );
		b.close();
	}
	
	
	
	
	public void testInheritanceII() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty(Constants.EXPORT_PACKAGE, "test.diff.inherit");
		b.build();
		Tree newer = differ.tree(b);
		System.out.println(newer.get("<api>"));
		Tree older = differ.tree(b);
		assertTrue(newer.diff(older).getDelta() == Delta.UNCHANGED);
	}

	/**
	 * The diff command reports changes on (at least) the guava bundles when
	 * they have not changed, even when I feed it the same file at both ends.
	 * Even stranger is the fact that, though always about the same class, the
	 * actual diff is not consistent. Tested with several versions of bnd
	 * (including master HEAD) as well as several versions of the guava bundles
	 * from maven central. Reproduced by @bnd .
	 * 
	 * <pre>
	 * $ java -jar biz.aQute.bnd.jar diff guava-14.0.1.jar guava-14.0.1.jar
	 * MINOR      PACKAGE    com.google.common.collect
	 *  MINOR      CLASS      com.google.common.collect.ContiguousSet
	 *   MINOR      METHOD     tailSet(java.lang.Object,boolean)
	 *    ADDED      RETURN     com.google.common.collect.ImmutableCollection
	 *    ADDED      RETURN     com.google.common.collect.ImmutableSet
	 *    ADDED      RETURN     com.google.common.collect.ImmutableSortedSet
	 *    ADDED      RETURN
	 * com.google.common.collect.ImmutableSortedSetFauxverideShim
	 *    ADDED      RETURN     com.google.common.collect.SortedIterable
	 *    ADDED      RETURN     java.io.Serializable
	 *    ADDED      RETURN     java.lang.Iterable
	 *    ADDED      RETURN     java.lang.Iterable
	 *    ADDED      RETURN     java.lang.Iterable
	 *    ADDED      RETURN     java.util.Collection
	 *    ADDED      RETURN     java.util.Collection
	 *    ADDED      RETURN     java.util.Set
	 * </pre>
	 * 
	 * @throws Exception
	 */

	public void testGuavaDiff() throws Exception {
		File guava = IO.getFile("testresources/guava-14.0.1.jar");
		Tree one = differ.tree(guava);
		System.out.println(one.get("<api>"));
		Tree two = differ.tree(guava);
		Diff diff = one.diff(two);
		assertTrue(diff.getDelta() == Delta.UNCHANGED);
	}

	public static void testAwtGeom() throws Exception {
		Tree newer = differ.tree(new File("../cnf/repo/ee.j2se/ee.j2se-1.5.0.jar"));
		Tree gp = newer.get("<api>").get("java.awt.geom").get("java.awt.geom.GeneralPath");
		assertNotNull(gp);
		show(gp, 0);
	}

	public static final class Final {
		public Final foo() {
			return null;
		}
	}

	public static class II {
		final int	x	= 3;

		public II foo() {
			return null;
		}
	}

	public static class I extends II {
		public static I bar() {
			return null;
		}

		@Override
		public I foo() {
			return null;
		}
	}

	public static void testInheritance() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty(Constants.EXPORT_PACKAGE, "test.diff");
		b.build();
		Tree newer = differ.tree(b);
		Tree older = differ.tree(b);
		System.out.println(newer);
		System.out.println(older);

		Diff diff = newer.diff(older);
		Diff p = diff.get("<api>").get("test.diff");
		assertTrue(p.getDelta() == Delta.UNCHANGED);
		show(p, 0);
		Diff c = p.get("test.diff.DiffTest$I");
		assertNotNull(c.get("hashCode()"));
		assertNotNull(c.get("finalize()"));
		assertNotNull(c.get("foo()"));

		Diff cc = p.get("test.diff.DiffTest$Final");
		assertNotNull(cc.get("foo()"));
		b.close();
	}

	public static interface Intf {
		void foo();
	}

	public static abstract class X implements Intf {

		@Override
		public void foo() {}
	}

	static interface A extends Comparable<Object>, Serializable {

	}

	public static void testSimple() throws Exception {

		DiffPluginImpl differ = new DiffPluginImpl();
		differ.setIgnore("Bundle-Copyright,Bundle-Description,Bundle-License,Bundle-Name,bundle-manifestversion,Export-Package,Import-Package,Bundle-Vendor,Bundle-Version");
		Tree newer = differ.tree(new Jar(new File("jar/osgi.core-4.3.0.jar")));
		Tree older = differ.tree(new Jar(new File("jar/osgi.core.jar"))); // 4.2
		Diff diff = newer.get("<manifest>").diff(older.get("<manifest>"));

		show(diff, 0);
		assertEquals(Delta.UNCHANGED, diff.getDelta());
	}

	static abstract class SBB {}

	public static class CMP implements Comparable<Number> {

		@Override
		public int compareTo(Number var0) {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	static class SB extends SBB implements Appendable {

		@Override
		public SB append(char var0) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SB append(CharSequence var0) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SB append(CharSequence var0, int var1, int var2) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
	}

	static void show(Diff diff, int indent) {
		// if (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() ==
		// Delta.IGNORED)
		// return;

		for (int i = 0; i < indent; i++)
			System.err.print("   ");

		System.err.println(diff.toString());

		// if (diff.getDelta().isStructural())
		// return;

		for (Diff c : diff.getChildren()) {
			show(c, indent + 1);
		}
	}

	static void show(Tree tree, int indent) {

		for (int i = 0; i < indent; i++)
			System.err.print("   ");

		System.err.println(tree.toString());

		for (Tree c : tree.getChildren()) {
			show(c, indent + 1);
		}
	}
}
