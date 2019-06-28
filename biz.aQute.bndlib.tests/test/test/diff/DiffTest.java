package test.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class DiffTest extends TestCase {
	static DiffPluginImpl differ = new DiffPluginImpl();

	public void testBaselineDiffs() throws Exception {

		Tree newerTree = make(IO.getFile("testresources/baseline/test1.jar"));
		Tree olderTree = make(IO.getFile("testresources/baseline/test2.jar"));
		Diff diff = newerTree.diff(olderTree);
		show(diff, 2);

		assertEquals(Delta.UNCHANGED, diff.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("<init>()")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("putAll(java.util.List)")
			.getDelta());
	}

	private Tree make(File file) throws Exception {
		Builder b = new Builder();
		b.addClasspath(file);
		b.setProperty("Export-Package", "*");
		b.build();
		assertTrue(b.check());
		return differ.tree(b)
			.get("<api>")
			.get("a")
			.get("a.clazzA");
	}

	/**
	 * Test API differences. We have a package in the /demo workspace project
	 * and we have the same package in our test.api package.
	 */

	public void testAPI() throws Exception {
		Jar older = new Jar(IO.getFile("../demo/generated/demo.jar"));
		Builder b = new Builder();
		b.addClasspath(IO.getFile("bin_test"));
		b.setExportPackage("test.api");
		b.build();
		assertTrue(b.check());

		Jar newer = b.getJar();
		Tree newerTree = differ.tree(newer)
			.get("<api>")
			.get("test.api");
		Tree olderTree = differ.tree(older)
			.get("<api>")
			.get("test.api");
		Diff treeDiff = newerTree.diff(olderTree);

		show(treeDiff, 2);

		assertEquals(Delta.MAJOR, treeDiff.getDelta());

		Diff diff = treeDiff.get("test.api.Interf");
		assertEquals(Delta.MAJOR, diff.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("abstract")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("foo()")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("foo()")
			.get("abstract")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("foo()")
			.get("java.util.Collection")
			.getDelta());
		assertEquals(Delta.MAJOR, diff.get("foo(java.lang.Object)")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("foo(java.lang.Object)")
			.get("abstract")
			.getDelta());
		assertEquals(Delta.REMOVED, diff.get("foo(java.lang.Object)")
			.get("java.lang.Object")
			.getDelta());
		assertEquals(Delta.ADDED, diff.get("foo(java.lang.Object)")
			.get("java.util.List")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("fooInt()")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("fooInt()")
			.get("int")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("fooString()")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("fooString()")
			.get("java.lang.String")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("foo(java.util.List)")
			.getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("foo(java.util.List)")
			.get("void")
			.getDelta());

		diff = treeDiff.get("test.api.A");
		assertEquals(Delta.MINOR, diff.getDelta());
		assertEquals(Delta.ADDED, diff.get("n")
			.getDelta());
		assertEquals(Delta.ADDED, diff.get("n")
			.get("static")
			.getDelta());
		assertEquals(Delta.ADDED, diff.get("n")
			.get("int")
			.getDelta());

		diff = treeDiff.get("test.api.C");
		assertEquals(Delta.MAJOR, diff.getDelta());
		assertEquals(Delta.MAJOR, diff.get("s")
			.getDelta());
		assertEquals(Delta.ADDED, diff.get("s")
			.get("int")
			.getDelta());
		assertEquals(Delta.REMOVED, diff.get("s")
			.get("java.lang.String")
			.getDelta());

		b.close();
	}

	public void testAPIStaticSuperClassChange() throws Exception {
		Jar older = new Jar(IO.getFile("../demo/generated/demo.jar"));
		Builder b = new Builder();
		b.addClasspath(IO.getFile("bin_test"));
		b.setExportPackage("test.api");
		b.build();
		assertTrue(b.check());
		Jar newer = b.getJar();

		Processor processor = new Processor();

		DiffPluginImpl differ = new DiffPluginImpl();
		Baseline baseline = new Baseline(processor, differ);

		Info info = baseline.baseline(newer, older, null)
			.iterator()
			.next();

		Diff field = info.packageDiff.get("test.api.B");
		show(field, 2);
		assertEquals(Delta.UNCHANGED, field.getDelta());

		b.close();
	}

	public void testBaselineOverride() throws Exception {
		try (Jar older = new Jar(IO.getFile("../demo/generated/demo.jar")); Builder b = new Builder()) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setExportPackage("test.api");
			b.build();
			assertTrue(b.check());
			Jar newer = b.getJar();

			Processor processor = new Processor();

			DiffPluginImpl differ = new DiffPluginImpl();
			Baseline baseline = new Baseline(processor, differ);

			Info info = baseline.baseline(newer, older, null)
				.iterator()
				.next();

			Diff diff = info.packageDiff;
			assertThat(diff.getDelta()).isEqualTo(Delta.MAJOR);
			diff = info.packageDiff.get("test.api.A");
			assertThat(diff.getDelta()).isEqualTo(Delta.MINOR);
			diff = info.packageDiff.get("test.api.B");
			assertThat(diff.getDelta()).isEqualTo(Delta.UNCHANGED);
			diff = info.packageDiff.get("test.api.C");
			assertThat(diff.getDelta()).isEqualTo(Delta.MAJOR);
			diff = info.packageDiff.get("test.api.Interf");
			assertThat(diff.getDelta()).isEqualTo(Delta.MAJOR);
			assertThat(info.mismatch).isFalse();
			show(info.packageDiff, 2);
		}
	}

	public void testInheritanceII() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("bin_test"));
		b.setProperty(Constants.EXPORT_PACKAGE, "test.diff.inherit");
		b.build();
		Tree newer = differ.tree(b);
		System.out.println(newer.get("<api>"));
		Tree older = differ.tree(b);
		assertTrue(newer.diff(older)
			.getDelta() == Delta.UNCHANGED);
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
	 *  $ java -jar
	 * biz.aQute.bnd.jar diff guava-14.0.1.jar guava-14.0.1.jar MINOR PACKAGE
	 * com.google.common.collect MINOR CLASS
	 * com.google.common.collect.ContiguousSet MINOR METHOD
	 * tailSet(java.lang.Object,boolean) ADDED RETURN
	 * com.google.common.collect.ImmutableCollection ADDED RETURN
	 * com.google.common.collect.ImmutableSet ADDED RETURN
	 * com.google.common.collect.ImmutableSortedSet ADDED RETURN
	 * com.google.common.collect.ImmutableSortedSetFauxverideShim ADDED RETURN
	 * com.google.common.collect.SortedIterable ADDED RETURN
	 * java.io.Serializable ADDED RETURN java.lang.Iterable ADDED RETURN
	 * java.lang.Iterable ADDED RETURN java.lang.Iterable ADDED RETURN
	 * java.util.Collection ADDED RETURN java.util.Collection ADDED RETURN
	 * java.util.Set
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

	/**
	 * Test the scenario where nested annotations can generate false positive in
	 * diffs
	 * <p>
	 * The trigger is a class-level annotations of the form
	 *
	 * <pre>
	 * {@literal @}Properties(value = { {@literal @}Property(name = "some.key",
	 * value = "some.value") })
	 * </pre>
	 *
	 * @throws Exception
	 */
	public void testNestedExportedAnnotations() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test/"));
		b.setExportPackage("test.annotations.diff.payload");
		Jar build = b.build();
		Tree one = differ.tree(build);
		System.out.println(one);
		Tree two = differ.tree(build);
		Diff diff = one.diff(two);
		assertTrue(diff.getDelta() == Delta.UNCHANGED);
		b.close();
	}

	/**
	 * Test the scenario where nested annotations can generate false positive in
	 * diffs
	 * <p>
	 * The trigger is a class-level annotations of the form
	 *
	 * <pre>
	 * {@literal @}Properties(value = { {@literal @}Property(name = "some.key",
	 * value = "some.value") })
	 * </pre>
	 *
	 * @throws Exception
	 */
	public void testNestedExportedAnnotations2() throws Exception {

		File input = IO.getFile("testresources/exported-annotations.jar");
		Tree one = differ.tree(input);
		Tree two = differ.tree(input);
		Diff diff = one.diff(two);
		assertTrue(diff.getDelta() == Delta.UNCHANGED);
	}

	public void testAwtGeom() throws Exception {
		Tree newer = differ.tree(IO.getFile("jar/ee.j2se-1.6.0.jar"));
		Tree gp = newer.get("<api>")
			.get("java.awt.geom")
			.get("java.awt.geom.GeneralPath");
		assertNotNull(gp);
		show(gp, 0);
	}

	public static final class Final {
		public Final foo() {
			return null;
		}
	}

	public static class II {
		final int x = 3;

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
		b.addClasspath(IO.getFile("bin_test"));
		b.setProperty(Constants.EXPORT_PACKAGE, "test.diff");
		b.build();
		Tree newer = differ.tree(b);
		Tree older = differ.tree(b);
		System.out.println(newer);
		System.out.println(older);

		Diff diff = newer.diff(older);
		Diff p = diff.get("<api>")
			.get("test.diff");
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

	public interface Intf {
		void foo();
	}

	public static abstract class X implements Intf {

		@Override
		public void foo() {}
	}

	interface A extends Comparable<Object>, Serializable {

	}

	public static void testSimple() throws Exception {

		DiffPluginImpl differ = new DiffPluginImpl();
		differ.setIgnore(
			"Bundle-Copyright,Bundle-Description,Bundle-License,Bundle-Name,bundle-manifestversion,Export-Package,Import-Package,Bundle-Vendor,Bundle-Version");
		Tree newer = differ.tree(new Jar(IO.getFile("jar/osgi.core-4.3.0.jar")));
		Tree older = differ.tree(new Jar(IO.getFile("jar/osgi.core.jar"))); // 4.2
		Diff diff = newer.get("<manifest>")
			.diff(older.get("<manifest>"));

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
