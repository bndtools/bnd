package test;

import junit.framework.TestCase;

public class RepoBuilderTest extends TestCase {

	public static void testA() {}

	// public void testSimple() {
	// assertBundles("osgi", RepoBuilder.STRATEGY_LOWEST, new
	// String[]{"osgi-3.0.0.jar"});
	// assertBundles("osgi", RepoBuilder.STRATEGY_HIGHEST, new
	// String[]{"osgi-4.2.0.jar"});
	// assertBundles("osgi;version=4.1", RepoBuilder.STRATEGY_HIGHEST, new
	// String[]{"osgi-4.2.0.jar"});
	// assertBundles("osgi;version=4.1", RepoBuilder.STRATEGY_LOWEST, new
	// String[]{"osgi-4.1.0.jar"});
	// assertBundles("osgi;version=4.1.1", RepoBuilder.STRATEGY_LOWEST, new
	// String[]{"osgi-4.2.0.jar"});
	// assertBundles("osgi;version=\"(4.1,5)\"", RepoBuilder.STRATEGY_LOWEST,
	// new String[]{"osgi-4.2.0.jar"});
	// assertBundles("osgi;version=\"[4.1,5)\"", RepoBuilder.STRATEGY_LOWEST,
	// new String[]{"osgi-4.1.0.jar"});
	// assertBundles("osgi;version=\"[4.1,5)\"", RepoBuilder.STRATEGY_HIGHEST,
	// new String[]{"osgi-4.2.0.jar"});
	// assertBundles("osgi;version=\"[3,4)\"", RepoBuilder.STRATEGY_HIGHEST, new
	// String[]{"osgi-3.0.0.jar"});
	// //assertBundles("osgi,org.osgi.impl.service.log,org.osgi.impl.service.cm",
	// RepoBuilder.STRATEGY_LOWEST, new String[]{"osgi-3.0.0.jar",
	// "org.osgi.impl.service.cm-2.1.1.jar"});
	// }
	//
	//
	// void assertBundles(String bundles, int strategy, String endsWith[]) {
	// RepoBuilder r = new RepoBuilder();
	// Properties p = new Properties();
	// p.setProperty("-plugin",
	// "aQute.lib.deployer.FileRepo;location=test/test/repo");
	// p.setProperty("-bundles", bundles );
	// r.setProperties(p);
	// List<Container> b = r.getBundles(strategy, r.getProperty("-bundles"));
	// List<File> l = new ArrayList<File>();
	// for ( Container c : b )
	// l.add(c.getFile());
	// check(r);
	// assertEquals( endsWith.length, b.size());
	// for ( int i =0; i<endsWith.length; i++ ) {
	// assertTrue(endsWith[i] + " : " + l.get(i),
	// l.get(i).toString().endsWith(endsWith[i]));
	// }
	// System.err.println(b);
	// }
	//
	// void check(Analyzer r) {
	// System.err.println(r.getErrors());
	// System.err.println(r.getWarnings());
	// assertEquals(0, r.getWarnings().size());
	// assertEquals(0, r.getErrors().size());
	// }
}
