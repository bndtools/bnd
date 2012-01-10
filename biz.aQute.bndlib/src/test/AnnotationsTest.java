package test;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.service.log.*;
import org.osgi.service.packageadmin.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.make.component.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.TypeRef;

public class AnnotationsTest extends TestCase {

	@Component(name = "mycomp", enabled = true, factory = "abc", immediate = false, provide = LogService.class, servicefactory = true) static class MyComponent
			implements Serializable {
		private static final long	serialVersionUID	= 1L;
		LogService					log;

		@Activate protected void activatex() {
		}

		@Deactivate protected void deactivatex() {
		}

		@Modified protected void modifiedx() {
		}

		@Reference(type = '~', target = "(abc=3)") protected void setLog(LogService log) {
			this.log = log;
		}

		@Reference(type = '1') protected void setPackageAdmin(PackageAdmin pa) {
		}

		protected void unsetLog(LogService log) {
			this.log = null;
		}
	}

	public void testComponentReader() throws Exception {
		Analyzer analyzer = new Analyzer();
		File f = new File("bin/test/AnnotationsTest$MyComponent.class");
		Clazz c = new Clazz(analyzer, "test.AnnotationsTest.MyComponent", new FileResource(f));
		Map<String, String> map = ComponentAnnotationReader.getDefinition(c);
		System.out.println(map);
		assertEquals("mycomp", map.get("name:"));
		assertEquals("true", map.get("servicefactory:"));
		assertEquals("activatex", map.get("activate:"));
		assertEquals("deactivatex", map.get("deactivate:"));
		assertEquals("modifiedx", map.get("modified:"));
		assertEquals("org.osgi.service.log.LogService(abc=3)~", map.get("log/setLog"));
		assertEquals("org.osgi.service.packageadmin.PackageAdmin",
				map.get("packageAdmin/setPackageAdmin"));
	}

	public void testSimple() throws Exception {
		Analyzer analyzer = new Analyzer();
		Clazz clazz = new Clazz(analyzer, "", null);
		ClassDataCollector cd = new ClassDataCollector() {
			public void addReference(TypeRef token) {
			}

			public void annotation(Annotation annotation) {
				System.out.println("Annotation " + annotation);
			}

			public void classBegin(int access, TypeRef name) {
				System.out.println("Class " + name);
			}

			public void classEnd() {
				System.out.println("Class end ");
			}

			public void extendsClass(TypeRef name) {
				System.out.println("extends " + name);
			}

			public void implementsInterfaces(TypeRef[] name) {
				System.out.println("implements " + Arrays.toString(name));

			}

			public void parameter(int p) {
				System.out.println("parameter " + p);
			}

		};

		clazz.parseClassFile(getClass().getResourceAsStream("Target.class"), cd);
	}
}

@Component class Target implements Serializable {
	private static final long	serialVersionUID	= 1L;

	@Activate void activate() {

	}

	@Deactivate void deactivate() {

	}

	@Modified void modified() {

	}

	@Reference void setLog(LogService log) {

	}

	void unsetLog(LogService log) {

	}
}
