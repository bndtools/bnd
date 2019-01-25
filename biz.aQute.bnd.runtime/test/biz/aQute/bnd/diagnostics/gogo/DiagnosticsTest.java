package biz.aQute.bnd.diagnostics.gogo;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import aQute.bnd.build.Workspace;
import aQute.bnd.remote.junit.JUnitFrameworkBuilder;
import aQute.lib.io.IO;

public class DiagnosticsTest {
	static Workspace				workspace;
	static JUnitFrameworkBuilder	builder;

	@SuppressWarnings("resource")
	@BeforeClass
	public static void before() throws Exception {
		Workspace.remoteWorkspaces = true;
		workspace = Workspace.findWorkspace(IO.work);
		builder = new JUnitFrameworkBuilder()
			.runfw("org.apache.felix.framework")
			.gogo()
			.bundles("biz.aQute.bnd.diagnostics.gogo");
	}

	@AfterClass
	public static void after() throws Exception {
		builder.close();
	}

	// @Test
	// public void testSimple() throws Exception {
	// try (JUnitFramework fw = builder.create()) {
	//
	// CommandProcessor cp = fw.getService(CommandProcessor.class);
	//
	// CommandSession s = cp.createSession(System.in, System.out, System.err);
	//
	// Object obj = s.execute("caps");
	// System.out.println(obj);
	// Thread.sleep(1000);
	// s.close();
	// }
	// }
	//
	// @Test
	// public void testSCR() throws Exception {
	// try (JUnitFramework fw = builder
	// .bundles(
	// "org.apache.felix.configadmin,org.apache.felix.scr")
	// .create()) {
	// CommandProcessor cp = fw.getService(CommandProcessor.class);
	// fw.reportServices(System.out);
	// CommandSession s = cp.createSession(System.in, System.out, System.err);
	//
	// Object obj = s.execute("ds");
	// System.out.println(obj);
	// Thread.sleep(1000);
	// }
	// }


	// JUnitFramework fw;
	// static ConsoleLogger log;
	//
	// @Before
	// public void load() throws Exception {
	// try {
	// fw = builder.run("org.apache.felix.framework");
	// log = new ConsoleLogger(fw.context);
	// fw.addBundle(" org.apache.felix.gogo.command," +
	// "org.apache.felix.gogo.runtime,"
	// + "org.apache.felix.scr;version=2.0.12" + "");
	// } catch (Exception e) {
	// e.printStackTrace();
	// throw e;
	// }
	//
	// }
	//
	// @After
	// public void close() throws Exception {
	// fw.close();
	// }
	//
	// @Test
	// public void testSimple() {
	// Bundle[] bundles = fw.context.getBundles();
	// assertThat(bundles).hasSize(4);
	//
	// ServiceReference<LogService> serviceReference =
	// fw.context.getServiceReference(LogService.class);
	// assertThat(serviceReference).isNotNull();
	// assertThat(serviceReference.getBundle()
	// .getBundleId()).isEqualTo(0L);
	// LogService service = fw.context.getService(serviceReference);
	// assertThat(service).isInstanceOf(ConsoleLogger.Facade.class);
	//
	// service.log(LogService.LOG_DEBUG, "Hello World");
	// }
	//
	// @Test
	// public void testBasicDiagnostics() {
	// Diagnostics d = new Diagnostics(fw.context);
	// List<Capability> caps = d.caps(-1, "*");
	// System.out.println("Caps" + caps);
	// }
	//
	// @Component
	// public static class TestSearchingComponent {
	//
	// @Reference
	// Foo foo;
	//
	// @Activate
	// void activate() {
	// System.out.println("activate TestSearchingComponent");
	// }
	//
	// @Deactivate
	// void deactivate() {
	// System.out.println("deactivate TestSearchingComponent");
	// }
	// }
	//
	// @Component
	// public static class TestFooComponent implements Foo {
	//
	// @Activate
	// void activate() {
	// System.out.println("activate TestFooComponent");
	// }
	//
	// @Deactivate
	// void deactivate() {
	// System.out.println("deactivate TestFooComponent");
	// }
	// }
	//
	// @Test
	// public void testWantedWithPrivateGetAndExportedRegister() throws
	// Exception {
	//
	// Diagnostics diagnostic = new Diagnostics(fw.context);
	//
	// //
	// // get service Foo, has private package with Foo
	// //
	// BundleBuilder aBuilder = fw.bundle();
	// aBuilder.addResource(TestSearchingComponent.class);
	// aBuilder.setPrivatePackage(Foo.class.getPackage()
	// .getName());
	// Bundle a = aBuilder.install();
	// a.start();
	//
	// List<Search> wanted = diagnostic.wanted(a.getBundleId(), Glob.ALL);
	//
	// boolean foundFooClassDiagnostic = false;
	// for (Search s : wanted) {
	// if (s.serviceName.equals(Foo.class.getName())) {
	// assertThat(s.mismatched).isEmpty();
	// assertThat(s.matched).isEmpty();
	// foundFooClassDiagnostic = true;
	// }
	// }
	//
	// assertThat(foundFooClassDiagnostic).isEqualTo(true);
	//
	// //
	// // register service Foo, exported package with Foo
	// //
	//
	// BundleBuilder bBuilder = fw.bundle();
	// bBuilder.addResource(TestFooComponent.class);
	// bBuilder.setExportPackage(Foo.class.getPackage()
	// .getName());
	// Bundle b = bBuilder.install();
	// b.start();
	//
	// wanted = diagnostic.wanted(a.getBundleId(), Glob.ALL);
	//
	// foundFooClassDiagnostic = false;
	// for (Search s : wanted) {
	// if (s.serviceName.equals(Foo.class.getName())) {
	// assertThat(s.mismatched).contains(b.getBundleId());
	// assertThat(s.matched).isEmpty();
	// foundFooClassDiagnostic = true;
	// }
	// }
	// assertThat(foundFooClassDiagnostic).isEqualTo(true);
	//
	// a.uninstall();
	// b.uninstall();
	// }
}
