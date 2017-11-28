package biz.aQute.bnd.reporter.plugins.headers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.JsonReportSerializerPlugin;
import junit.framework.TestCase;

public class HeadersTest extends TestCase {

	public void testHeaders() throws Exception {

		final StringBuffer expected = new StringBuffer();
		final List<String> headers = new LinkedList<>();

		headers.add("");
		headers.add("class.Name");
		perform(Constants.BUNDLE_ACTIVATOR, new ActivatorExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("one");
		headers.add("one, two, one, two");
		perform(Constants.BUNDLE_CATEGORY, new CategoryExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("class.Name");
		headers.add("class.Name;class.Name1,class.Name2");
		perform(Constants.BUNDLE_CLASSPATH, new ClasspathExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("25 Argile Street Ottawa");
		headers.add("http://google.com");
		headers.add("cde@cde.cde");
		perform(Constants.BUNDLE_CONTACTADDRESS, new ContactAddressExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("My Copyright");
		perform(Constants.BUNDLE_COPYRIGHT, new CopyrightExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("My Description");
		perform(Constants.BUNDLE_DESCRIPTION, new DescriptionExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("cde@cde.cde");
		headers.add(
				"cde;email='cde@cde.cde';name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';timezone=1;roles='one'");
		headers.add(
				"cde;email='cde@cde.cde';name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';timezone=1;roles='one, tow',cde@two.com;name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';timezone=1;roles='one'");
		perform(Constants.BUNDLE_DEVELOPERS, new DeveloperExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("www.google.com");
		perform(Constants.BUNDLE_DOCURL, new DocUrlExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("com.test.*");
		headers.add(
				"com.test.*;com.test2;version='1.2.0';bundle-symbolic-name='symbolic.test';condi1=test;directivetoignor:=test;bundle-version='1',com.test3;condi2=test2");
		perform(Constants.DYNAMICIMPORT_PACKAGE, new DynamicImportExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("com.test");
		headers.add(
				"com.test;com.test2;version='1.2.0';mandatory:='condi1, condi2';include:='com.t, org.t';exclude:='com.tt, org.tt';uses:='com.test, org.test';condi1=test;directivetoignor:=test,com.test3;condi2=test2");
		perform(Constants.EXPORT_PACKAGE, new ExportExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("symbolic.name");
		headers.add("symbolic.name;extension:='bootclasspath';bundle-version='[1.2,2.0)'");
		headers.add("symbolic.name;extension:='bootclasspath';otherattri='cool';notallow:=direct");
		perform(Constants.FRAGMENT_HOST, new FragmentHostExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("./path");
		headers.add("./path, ./path2;size=34");
		perform(Constants.BUNDLE_ICON, new IconExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("com.test");
		headers.add(
				"com.test.*;com.test2;version='[1.2.0.QUAL,2.0.0.QUAL)';resolution:='optional';bundle-symbolic-name='symbolic.test';condi1=test;directivetoignor:=test;bundle-version='1',com.test3;condi2=test2");
		perform(Constants.IMPORT_PACKAGE, new ImportExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("lazy");
		headers.add("lazy;include:='test,test1';exclude:='test2, test3'");
		perform(Constants.BUNDLE_ACTIVATIONPOLICY, new LazyActivationExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("Apache");
		headers.add("Apache;description='description';link='www.google.com',Apache2");
		perform(Constants.BUNDLE_LICENSE, new LicenseExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("1");
		perform(Constants.BUNDLE_MANIFESTVERSION, new ManifestVersionExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("Test");
		perform(Constants.BUNDLE_NAME, new NameExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("lib/http.dll; lib/zlib.dll;osname=Windows95;osname=Windows98;osversion='not';"
				+ "osname=WindowsNT;processor=x86;selection-filter = '(com.acme.windowing=win32)';"
				+ "language=en;language=se," + "lib/solaris/libhttp.so;osname=Solaris;osname=SunOS;processor=sparc,"
				+ "lib/linux/libhttp.so;osname=Linux;osversion='2';osversion='[1.0,3.0)';processor=mips;selection-filter='(com.acme.windowing=gtk)',*");
		perform(Constants.BUNDLE_NATIVECODE, new NativeCodeExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("com.namespace");
		headers.add(
				"com.namespace;custom:=test;effective:='other';uses:='one,two';t=dde;tt:Double=5;ttt:Long=6;tttt:String=6;ttttt:Version=6;"
						+ "l:List='dd,e';ll:List<Double>='5,4';lll:List<Long>='6,7';llll:List<String>='6,dede';lllll:List<Version>='6,1.2';q:Strange=not");
		perform(Constants.PROVIDE_CAPABILITY, new ProvideCapabilityExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("bundle.symbolicName");
		headers.add(
				"bundle.symbolicName;visibility:='reexport';resolution:=optional;bundle-version=1,com.test;abri=v;notallow:=de");
		perform(Constants.REQUIRE_BUNDLE, new RequireBundleExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("bundle.symbolicName");
		headers.add(
				"bundle.symbolicName;filter:=cool;cardinality:=multiple;effective:='other';resolution:=optional;att:Double=5,com.test");
		headers.add("com.namespace;custom:=test;t=dde;tt:Double=5;ttt:Long=6;tttt:String=6;ttttt:Version=6;"
				+ "l:List='dd,e';ll:List<Double>='5,4';lll:List<Long>='6,7';llll:List<String>='6,dede';lllll:List<Version>='6,1.2';q:Strange=not");
		perform(Constants.REQUIRE_CAPABILITY, new RequiredCapabilityExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("one");
		headers.add("one,two");
		perform(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, new RequiredExecutionEnvironmentExtractor(), headers,
				expected);
		headers.clear();

		headers.add("");
		headers.add("com.symbolic.name");
		headers.add(
				"com.symbolic.name;singleton:=true;fragment-attachment:=never;mandatory:='test, test2';condi=test;toignore:=test");
		perform(Constants.BUNDLE_SYMBOLICNAME, new SymbolicNameExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("My vendor");
		perform(Constants.BUNDLE_VENDOR, new VendorExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("url=test,connection=test,developerConnection=test,tag=test,notallow=test");
		perform(Constants.BUNDLE_SCM, new SCMExtractor(), headers, expected);
		headers.clear();

		headers.add("");
		headers.add("notver");
		headers.add("1.2");
		headers.add("1.2.0.45689-SNAPSHOT");
		perform(Constants.BUNDLE_VERSION, new VersionExtractor(), headers, expected);

		final StringBuffer e = new StringBuffer();

		for (final String l : Files.readAllLines(Paths.get("testresources/headersResult.txt"),
				StandardCharsets.UTF_8)) {

			e.append(l + "\n");
		}

		assertEquals(expected.toString(), e.toString());
	}

	public void perform(final String headerName, final HeaderExtractor extractor, final List<String> headers,
			final StringBuffer expected) throws Exception {

		final Iterator<String> ith = headers.iterator();

		while (ith.hasNext()) {

			final String h = ith.next();
			final Jar jar = new Jar("jar");
			final Map<String, Resource> dir = new HashMap<>();
			dir.put("com/test", new FileResource(new File("")));
			jar.addDirectory(dir, true);
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes().putValue(headerName, h);
			final Processor p = new Processor();

			final Map<String, Object> result = new HashMap<>();
			result.put(extractor.getEntryName(), extractor.extract(ManifestHelper.get(jar, ""), jar, p));

			assertTrue(p.isOk());

			final ByteArrayOutputStream s = new ByteArrayOutputStream();
			new JsonReportSerializerPlugin().serialize(result, s);

			expected.append("\nextractor: " + extractor.getClass().getSimpleName());
			expected.append("\nheader: " + h);
			expected.append("\nresult: \n" + new String(s.toByteArray()));
			expected.append("\n---\n");
		}
	}
}
