package test.diff;

import java.io.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.service.diff.*;
import aQute.lib.osgi.*;

public class DiffTest extends TestCase {
	DiffPluginImpl differ = new DiffPluginImpl();


	
	public void testAwtGeom() throws Exception {
		Tree newer = differ.tree( new File("/Ws/osgi/branch/cnf/repo/ee.j2se/ee.j2se-1.5.0.jar"));
		Tree gp = newer.get("<api>").get("java.awt.geom").get("java.awt.geom.GeneralPath");
		assertNotNull(gp);
		show(gp,0);
	}
	
	
	public final class Final {
		public void foo() {}
	}
	public class II {
		final int x=3;
		public void foo() {}
	}
	public class I extends II {
		public I bar() { return null; }
		public void foo() { }
	}
	public void testInheritance() throws Exception {
		Builder b = new Builder();
		b.addClasspath( new File("bin"));
		b.setProperty(Constants.EXPORT_PACKAGE, "test.diff");
		b.build();
		Tree newer = differ.tree(b);
		Tree older = differ.tree(b);
		Diff diff = newer.diff(older);
		Diff p = diff.get("<api>").get("test.diff");
		show(p,0);
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
	public abstract class X implements Intf {
	
		public void foo() {}
	}
	
	interface A extends Comparable, Serializable {

	}

	public void testSimple() throws Exception {

		Tree newer = differ.tree(new Jar(new File("jar/osgi.core-4.3.0.jar")));
		Tree older = differ.tree(new Jar(new File("jar/osgi.core.jar"))); // 4.2
		Diff diff = newer.diff(older);

		show(diff, 0);		
	}


	private abstract class SBB {}
	
	public class CMP implements Comparable<Number> {

		public int compareTo(Number var0) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	public class SB extends SBB implements Appendable {

		public SB append(char var0) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public SB append(CharSequence var0) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public SB append(CharSequence var0, int var1, int var2) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	
	void show(Diff diff, int indent) {
//		if (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() == Delta.IGNORED)
//			return;

		 for (int i = 0; i < indent; i++)
			 System.out.print("   ");

		System.out.println(diff.toString());

//		if (diff.getDelta().isStructural())
//			return;

		for (Diff c : diff.getChildren()) {
			show(c, indent + 1);
		}
	}

	void show(Tree tree, int indent) {

		 for (int i = 0; i < indent; i++)
			 System.out.print("   ");

		System.out.println(tree.toString());


		for (Tree c : tree.getChildren()) {
			show(c, indent + 1);
		}
	}
}
