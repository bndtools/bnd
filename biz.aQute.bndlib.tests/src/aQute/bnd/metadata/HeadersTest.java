package aQute.bnd.metadata;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class HeadersTest extends TestCase {

	public void testHeaders() throws Exception {

		StringBuffer expected = new StringBuffer();
		List<String> headers = new LinkedList<>();

		headers.add("");
		headers.add("class.Name");
		perform(new ActivatorExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("one");
		headers.add("one, two");
		perform(new CategoryExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("class.Name");
		headers.add("class.Name;class.Name1,class.Name2");
		perform(new ClassPathExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("25 Argile Street Ottawa");
		headers.add("http://google.com");
		headers.add("cde@cde.cde");
		perform(new ContactAddressExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("My Copyright");
		perform(new CopyrightExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("My Description");
		perform(new DescriptionExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("cde@cde.cde");
		headers.add(
				"cde;email='cde@cde.cde';name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';timezone=1;roles='one'");
		headers.add(
				"cde;email='cde@cde.cde';name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';timezone=1;roles='one, tow',cde@two.com;name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';timezone=1;roles='one'");
		perform(new DeveloperExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("www.google.com");
		perform(new DocUrlExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("com.test.*");
		headers.add(
				"com.test.*;com.test2;version='1.2.0';bundle-symbolic-name='symbolic.test';condi1=test;directivetoignor:=test;bundle-version='1',com.test3;condi2=test2");
		perform(new DynamicImportExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("com.test");
		headers.add(
				"com.test;com.test2;version='1.2.0';mandatory:='condi1, condi2';include:='com.t, org.t';exclude:='com.tt, org.tt';uses:='com.test, org.test';condi1=test;directivetoignor:=test,com.test3;condi2=test2");
		perform(new ExportExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("symbolic.name");
		headers.add("symbolic.name;extension:='bootclasspath';bundle-version='[1.2,2.0)'");
		perform(new FragmentHostExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("./path");
		headers.add("./path, ./path2;size=34");
		perform(new IconExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("com.test");
		headers.add(
				"com.test.*;com.test2;version='[1.2.0,2.0)';resolution:='optional';bundle-symbolic-name='symbolic.test';condi1=test;directivetoignor:=test;bundle-version='1',com.test3;condi2=test2");
		perform(new ImportExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("lazy");
		headers.add("lazy;include:='test,test1';exclude:='test2, test3'");
		perform(new LazyActivationExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("Apache");
		headers.add("Apache;description='description';link='www.google.com',Apache2");
		perform(new LicenseExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("1.2.0.45689-SNAPSHOT");
		perform(new ManifestVersionExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("Test");
		perform(new NameExtractor(), headers, expected, false);
		headers.clear();


		headers.add("");
		headers.add("lib/http.dll; lib/zlib.dll;osname=Windows95;osname=Windows98;"
				+ "osname=WindowsNT;processor=x86;selection-filter = '(com.acme.windowing=win32)';"
				+ "language=en;language=se," + "lib/solaris/libhttp.so;osname=Solaris;osname=SunOS;processor=sparc,"
				+ "lib/linux/libhttp.so;osname=Linux;osversion='2';osversion='[1.0,3.0)';processor=mips;selection-filter='(com.acme.windowing=gtk)',*");
		perform(new NativeCodeExtractor(), headers, expected, true);
		headers.clear();

		headers.add("");
		headers.add("com.namespace");
		headers.add(
				"com.namespace;custom:=test;effective:='other';uses:='one,two';t=dde;tt:Double=5;ttt:Long=6;tttt:String=6;ttttt:Version=6;"
				+ "l:List='dd,e';ll:List<Double>='5,4';lll:List<Long>='6,7';llll:List<String>='6,dede';lllll:List<Version>='6,1.2'");
		perform(new ProvideCapabilityExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("bundle.symbolicName");
		headers.add("bundle.symbolicName;visibility:='reexport';resolution:=optional;bundle-version=1,com.test");
		perform(new RequireBundleExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("bundle.symbolicName");
		headers.add(
				"bundle.symbolicName;filter:=cool;cardinality:=multiple;effective:='other';resolution:=optional;att:Double=5,com.test");
		perform(new RequiredCapabilityExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("one");
		headers.add("one,two");
		perform(new RequiredExecutionEnvironmentExtractor(), headers, expected, false);
		headers.clear();

		headers.add("com.symbolic.name");
		headers.add(
				"com.symbolic.name;singleton:=true;fragment-attachment:=never;mandatory:='test, test2';condi=test;toignore:=test");
		perform(new SymbolicNameExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("My vendor");
		perform(new VendorExtractor(), headers, expected, false);
		headers.clear();

		headers.add("");
		headers.add("1.2");
		perform(new VersionExtractor(), headers, expected, false);

		StringBuffer e = new StringBuffer();

		for (String l : Files.readAllLines(Paths.get("testresources/metadata/resultHeaders.json"),
				StandardCharsets.UTF_8)) {

			e.append(l + "\n");
		}

		assertEquals(e.toString(), expected.toString());
	}

	public void perform(HeaderExtractor extractor, List<String> headers, StringBuffer expected, boolean multiAttr)
			throws Exception {

		Iterator<String> ith = headers.iterator();

		while (ith.hasNext()) {

			String h = ith.next();

			ManifestHeadersDTO dto = new ManifestHeadersDTO();
			Parameters p = new Parameters(multiAttr);
			OSGiHeader.parseHeader(h, null, p);

			if (!p.isEmpty()) {
				extractor.extract(dto, p, new HashMap<String,Parameters>(), new Jar("name"));
			} else {
				extractor.extract(dto, null, new HashMap<String,Parameters>(), new Jar("name"));
			}
			extractor.verify(dto);

			BundleMetadataDTO bundle = new BundleMetadataDTO();
			bundle.manifestHeaders = dto;

			ByteArrayOutputStream s = new ByteArrayOutputStream();
			MetadataJsonIO.toJson(bundle, s);

			expected.append("\nextractor: " + extractor.getClass().getSimpleName());
			expected.append("\nheader: " + h);
			expected.append("\nresult: \n" + new String(s.toByteArray()));
			expected.append("\n---\n");
		}
	}
}
