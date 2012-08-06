package test;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.service.log.*;
import org.osgi.service.packageadmin.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.make.component.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class AnnotationsTest extends TestCase {

	@Component(name = "mycomp", enabled = true, factory = "abc", immediate = false, provide = LogService.class, servicefactory = true,
			properties = {" aprop = a prop ", "    aprop2    =    really dumb value   ! "})
	static class MyComponent implements Serializable {
		private static final long	serialVersionUID	= 1L;
		LogService					log;

		@Activate
		protected void activatex() {}

		@Deactivate
		protected void deactivatex() {}

		@Modified
		protected void modifiedx() {}

		@Reference(type = '~', target = "(abc=3)")
		protected void setLog(LogService log) {
			this.log = log;
		}

		@Reference(type = '1')
		protected void setPackageAdmin(@SuppressWarnings("unused") PackageAdmin pa) {}

		protected void unsetLog(@SuppressWarnings("unused") LogService log) {
			this.log = null;
		}
	}

	public void testComponentReader() throws Exception {
		Analyzer analyzer = new Analyzer();
		File f = new File("bin/test/AnnotationsTest$MyComponent.class");
		Clazz c = new Clazz(analyzer, "test.AnnotationsTest.MyComponent", new FileResource(f));
		Map<String,String> map = ComponentAnnotationReader.getDefinition(c);
		System.err.println(map);
		assertEquals("mycomp", map.get("name:"));
		assertEquals("true", map.get("servicefactory:"));
		assertEquals("activatex", map.get("activate:"));
		assertEquals("deactivatex", map.get("deactivate:"));
		assertEquals("modifiedx", map.get("modified:"));
		assertEquals("org.osgi.service.log.LogService(abc=3)~", map.get("log/setLog"));
		assertEquals("org.osgi.service.packageadmin.PackageAdmin", map.get("packageAdmin/setPackageAdmin"));
		assertEquals("aprop= a prop ,aprop2=    really dumb value   ! ", map.get("properties:"));
	}

	public void testSimple() throws Exception {
		Analyzer analyzer = new Analyzer();
		Clazz clazz = new Clazz(analyzer, "", null);
		ClassDataCollector cd = new ClassDataCollector() {
			@Override
			public void addReference(TypeRef token) {}

			@Override
			public void annotation(Annotation annotation) {
				System.err.println("Annotation " + annotation);
			}

			@Override
			public void classBegin(int access, TypeRef name) {
				System.err.println("Class " + name);
			}

			@Override
			public void classEnd() {
				System.err.println("Class end ");
			}

			@Override
			public void extendsClass(TypeRef name) {
				System.err.println("extends " + name);
			}

			@Override
			public void implementsInterfaces(TypeRef[] name) {
				System.err.println("implements " + Arrays.toString(name));

			}

			@Override
			public void parameter(int p) {
				System.err.println("parameter " + p);
			}

		};

		clazz.parseClassFile(getClass().getResourceAsStream("Target.class"), cd);
	}
}

@Component
class Target implements Serializable {
	private static final long	serialVersionUID	= 1L;

	@Activate
	void activate() {

	}

	@Deactivate
	void deactivate() {

	}

	@Modified
	void modified() {

	}

	@Reference
	void setLog(@SuppressWarnings("unused") LogService log) {

	}

	void unsetLog(@SuppressWarnings("unused") LogService log) {

	}
}
