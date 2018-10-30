package biz.aQute.bnd.reporter.helpers;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.json.JSONCodec;
import biz.aQute.bnd.reporter.manifest.dto.ActivationPolicyDTO;
import biz.aQute.bnd.reporter.manifest.dto.BundleSymbolicNameDTO;
import biz.aQute.bnd.reporter.manifest.dto.ContactAddressDTO;
import biz.aQute.bnd.reporter.manifest.dto.DeveloperDTO;
import biz.aQute.bnd.reporter.manifest.dto.DynamicImportPackageDTO;
import biz.aQute.bnd.reporter.manifest.dto.ExportPackageDTO;
import biz.aQute.bnd.reporter.manifest.dto.FragmentHostDTO;
import biz.aQute.bnd.reporter.manifest.dto.IconDTO;
import biz.aQute.bnd.reporter.manifest.dto.ImportPackageDTO;
import biz.aQute.bnd.reporter.manifest.dto.LicenseDTO;
import biz.aQute.bnd.reporter.manifest.dto.NativeCodeDTO;
import biz.aQute.bnd.reporter.manifest.dto.NativeCodeEntryDTO;
import biz.aQute.bnd.reporter.manifest.dto.ProvideCapabilityDTO;
import biz.aQute.bnd.reporter.manifest.dto.RequireBundleDTO;
import biz.aQute.bnd.reporter.manifest.dto.RequireCapabilityDTO;
import biz.aQute.bnd.reporter.manifest.dto.ScmDTO;
import biz.aQute.bnd.reporter.manifest.dto.TypedAttributeValueDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionInRangeDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionRangeDTO;
import junit.framework.TestCase;

@SuppressWarnings("boxing")
public class HeadersHelperTest extends TestCase {

  public void testBundleActivator() throws Exception {
    String expected = null;
    perform(Constants.BUNDLE_ACTIVATOR, "bundleActivator", "", expected);

    expected = "class.Name";
    perform(Constants.BUNDLE_ACTIVATOR, "bundleActivator", "class.Name", expected);
  }

  public void testBundleCategories() throws Exception {
    List<String> expected = null;
    perform(Constants.BUNDLE_CATEGORY, "bundleCategories", "", expected);

    expected = new LinkedList<>();
    expected.add("one");
    perform(Constants.BUNDLE_CATEGORY, "bundleCategories", "one", expected);

    expected.add("two");
    expected.add("one");
    expected.add("two");
    perform(Constants.BUNDLE_CATEGORY, "bundleCategories", "one, two, one, two", expected);
  }

  public void testBundleClasspaths() throws Exception {
    final List<String> expected = new LinkedList<>();
    expected.add(".");
    perform(Constants.BUNDLE_CLASSPATH, "bundleClasspaths", "", expected);

    expected.clear();
    expected.add("class.Name");
    perform(Constants.BUNDLE_CLASSPATH, "bundleClasspaths", "class.Name", expected);

    expected.add("class.Name1");
    expected.add("class.Name2");
    perform(Constants.BUNDLE_CLASSPATH, "bundleClasspaths", "class.Name;class.Name1,class.Name2",
        expected);
  }

  public void testBundleContactAddress() throws Exception {
    ContactAddressDTO expected = null;
    perform(Constants.BUNDLE_CONTACTADDRESS, "bundleContactAddress", "", expected);

    expected = new ContactAddressDTO();
    expected.address = "25 Argile Street Ottawa";
    expected.type = "postal";
    perform(Constants.BUNDLE_CONTACTADDRESS, "bundleContactAddress", "25 Argile Street Ottawa",
        expected);

    expected = new ContactAddressDTO();
    expected.address = "http://google.com";
    expected.type = "url";
    perform(Constants.BUNDLE_CONTACTADDRESS, "bundleContactAddress", "http://google.com", expected);

    expected = new ContactAddressDTO();
    expected.address = "cde@cde.cde";
    expected.type = "email";
    perform(Constants.BUNDLE_CONTACTADDRESS, "bundleContactAddress", "cde@cde.cde", expected);
  }

  public void testBundleCopyright() throws Exception {
    String expected = null;
    perform(Constants.BUNDLE_COPYRIGHT, "bundleCopyright", "", expected);

    expected = "My Copyright";
    perform(Constants.BUNDLE_COPYRIGHT, "bundleCopyright", "My Copyright", expected);
  }

  public void testBundleDescription() throws Exception {
    String expected = null;
    perform(Constants.BUNDLE_DESCRIPTION, "bundleDescription", "", expected);

    expected = "My Description";
    perform(Constants.BUNDLE_DESCRIPTION, "bundleDescription", "My Description", expected);
  }

  public void testBundleDevelopers() throws Exception {
    DeveloperDTO e = null;
    List<DeveloperDTO> expected = null;
    perform(Constants.BUNDLE_DEVELOPERS, "bundleDevelopers", "", expected);

    expected = new LinkedList<>();
    e = new DeveloperDTO();
    e.identifier = "cde@cde.cde";
    expected.add(e);
    perform(Constants.BUNDLE_DEVELOPERS, "bundleDevelopers", "cde@cde.cde", expected);

    expected.clear();
    e = new DeveloperDTO();
    e.identifier = "cde";
    e.email = "cde@cde.cde";
    e.name = "Clement Delgrange";
    e.organization = "cdecode";
    e.organizationUrl = "cdecode.org";
    e.timezone = 1;
    e.roles.add("one");
    expected.add(e);
    perform(Constants.BUNDLE_DEVELOPERS, "bundleDevelopers",
        "cde;email='cde@cde.cde';name='Clement Delgrange';organization='cdecode';"
            + "organizationUrl='cdecode.org';timezone=1;roles='one'",
        expected);
    e.roles.add("two");
    e = new DeveloperDTO();
    e.identifier = "cde@two.com";
    e.name = "Clement Delgrange";
    e.organization = "cdecode";
    e.organizationUrl = "cdecode.org";
    e.timezone = 1;
    e.roles.add("one");
    expected.add(e);
    perform(Constants.BUNDLE_DEVELOPERS, "bundleDevelopers",
        "cde;email='cde@cde.cde';name='Clement Delgrange';organization='cdecode'"
            + ";organizationUrl='cdecode.org';timezone=1;roles='one, two',cde@two.com"
            + ";name='Clement Delgrange';organization='cdecode';organizationUrl='cdecode.org';"
            + "timezone=1;roles='one'",
        expected);
  }

  public void testBundleDocUrl() throws Exception {
    String expected = null;
    perform(Constants.BUNDLE_DOCURL, "bundleDocUrl", "", expected);
    expected = "www.google.com";
    perform(Constants.BUNDLE_DOCURL, "bundleDocUrl", "www.google.com", expected);
  }

  public void testDynamicImportPackages() throws Exception {
    DynamicImportPackageDTO i;
    List<DynamicImportPackageDTO> expected = null;
    perform(Constants.DYNAMICIMPORT_PACKAGE, "dynamicImportPackages", "", expected);

    expected = new LinkedList<>();
    i = new DynamicImportPackageDTO();
    i.packageName = "com.test.*";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 0;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 0;
    i.version.floor.minor = 0;
    i.version.floor.micro = 0;
    expected.add(i);
    perform(Constants.DYNAMICIMPORT_PACKAGE, "dynamicImportPackages", "com.test.*", expected);

    expected.clear();
    i = new DynamicImportPackageDTO();
    i.packageName = "com.test.*";
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 1;
    i.version.floor.minor = 2;
    i.version.floor.micro = 0;
    i.bundleSymbolicName = "symbolic.test";
    i.arbitraryAttributes.put("condi1", "test");
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 1;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    expected.add(i);
    i = new DynamicImportPackageDTO();
    i.packageName = "com.test2";
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 1;
    i.version.floor.minor = 2;
    i.version.floor.micro = 0;
    i.bundleSymbolicName = "symbolic.test";
    i.arbitraryAttributes.put("condi1", "test");
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 1;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    expected.add(i);
    i = new DynamicImportPackageDTO();
    i.packageName = "com.test3";
    i.arbitraryAttributes.put("condi2", "test2");
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 1;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.bundleVersion.ceiling = new VersionInRangeDTO();
    i.bundleVersion.ceiling.include = false;
    i.bundleVersion.ceiling.major = 1;
    i.bundleVersion.ceiling.minor = 1;
    i.bundleVersion.ceiling.micro = 0;
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 0;
    i.version.floor.minor = 0;
    i.version.floor.micro = 0;
    expected.add(i);
    perform(Constants.DYNAMICIMPORT_PACKAGE, "dynamicImportPackages",
        "com.test.*;com.test2;version='1.2.0';bundle-symbolic-name='symbolic.test';condi1=test;"
            + "directivetoignor:=test;bundle-version='1',com.test3;condi2=test2;bundle-version='[1.0,1.1)'",
        expected);
  }

  public void testExportPackages() throws Exception {
    ExportPackageDTO e;
    List<ExportPackageDTO> expected = null;
    perform(Constants.EXPORT_PACKAGE, "exportPackages", "", expected);

    expected = new LinkedList<>();
    e = new ExportPackageDTO();
    e.packageName = "com.test";
    e.version = new VersionDTO();
    e.version.major = 0;
    e.version.minor = 0;
    e.version.micro = 0;
    expected.add(e);
    perform(Constants.EXPORT_PACKAGE, "exportPackages", "com.test", expected);

    expected.clear();
    e = new ExportPackageDTO();
    e.packageName = "com.test";
    e.version = new VersionDTO();
    e.version.major = 1;
    e.version.minor = 2;
    e.version.micro = 0;
    e.mandatories.add("condi1");
    e.mandatories.add("condi2");
    e.includes.add("com.t");
    e.includes.add("org.t");
    e.excludes.add("com.tt");
    e.excludes.add("org.tt");
    e.uses.add("com.test");
    e.uses.add("org.test");
    e.arbitraryAttributes.put("condi1", "test");
    expected.add(e);
    e = new ExportPackageDTO();
    e.packageName = "com.test2";
    e.version = new VersionDTO();
    e.version.major = 1;
    e.version.minor = 2;
    e.version.micro = 0;
    e.mandatories.add("condi1");
    e.mandatories.add("condi2");
    e.includes.add("com.t");
    e.includes.add("org.t");
    e.excludes.add("com.tt");
    e.excludes.add("org.tt");
    e.uses.add("com.test");
    e.uses.add("org.test");
    e.arbitraryAttributes.put("condi1", "test");
    expected.add(e);
    e = new ExportPackageDTO();
    e.packageName = "com.test3";
    e.version = new VersionDTO();
    e.version.major = 0;
    e.version.minor = 0;
    e.version.micro = 0;
    e.arbitraryAttributes.put("condi2", "test2");
    expected.add(e);
    perform(Constants.EXPORT_PACKAGE, "exportPackages",
        "com.test;com.test2;version='1.2.0';mandatory:='condi1, condi2';include:='com.t, org.t';"
            + "exclude:='com.tt, org.tt';uses:='com.test, org.test';condi1=test;directivetoignor:=test,com.test3;condi2=test2",
        expected);
  }

	public void testFragmentHost() throws Exception {
    FragmentHostDTO expected = null;
    perform(Constants.FRAGMENT_HOST, "fragmentHost", "", expected);

    expected = new FragmentHostDTO();
    expected.bundleSymbolicName = "symbolic.name";
    expected.bundleVersion = new VersionRangeDTO();
    expected.bundleVersion.floor = new VersionInRangeDTO();
    expected.bundleVersion.floor.include = true;
    expected.bundleVersion.floor.major = 0;
    expected.bundleVersion.floor.minor = 0;
    expected.bundleVersion.floor.micro = 0;
    expected.extension = "framework";
    perform(Constants.FRAGMENT_HOST, "fragmentHost", "symbolic.name", expected);

    expected = new FragmentHostDTO();
    expected.bundleSymbolicName = "symbolic.name";
    expected.bundleVersion = new VersionRangeDTO();
    expected.bundleVersion.floor = new VersionInRangeDTO();
    expected.bundleVersion.floor.include = true;
    expected.bundleVersion.floor.major = 1;
    expected.bundleVersion.floor.minor = 2;
    expected.bundleVersion.floor.micro = 0;
    expected.bundleVersion.ceiling = new VersionInRangeDTO();
    expected.bundleVersion.ceiling.include = false;
    expected.bundleVersion.ceiling.major = 2;
    expected.bundleVersion.ceiling.minor = 0;
    expected.bundleVersion.ceiling.micro = 0;
    expected.extension = "bootclasspath";
    perform(Constants.FRAGMENT_HOST, "fragmentHost",
        "symbolic.name;extension:='bootclasspath';bundle-version='[1.2,2.0)'", expected);

    expected = new FragmentHostDTO();
    expected.bundleSymbolicName = "symbolic.name";
    expected.bundleVersion = new VersionRangeDTO();
    expected.bundleVersion.floor = new VersionInRangeDTO();
    expected.bundleVersion.floor.include = true;
    expected.bundleVersion.floor.major = 0;
    expected.bundleVersion.floor.minor = 0;
    expected.bundleVersion.floor.micro = 0;
    expected.extension = "bootclasspath";
    expected.arbitraryAttributes.put("otherattri", "cool");
    perform(Constants.FRAGMENT_HOST, "fragmentHost",
        "symbolic.name;extension:='bootclasspath';otherattri='cool';notallow:=direct", expected);
  }

  public void testBundleIcons() throws Exception {
    IconDTO i;
    List<IconDTO> expected = null;
    perform(Constants.BUNDLE_ICON, "bundleIcons", "", expected);

    expected = new LinkedList<>();
    i = new IconDTO();
    i.url = "./path";
    expected.add(i);
    perform(Constants.BUNDLE_ICON, "bundleIcons", "./path", expected);

    i.size = 34;
    i = new IconDTO();
    i.url = "./path2";
    i.size = 34;
    expected.add(i);
    perform(Constants.BUNDLE_ICON, "bundleIcons", "./path; ./path2;size=34", expected);
  }

  public void testImportPackages() throws Exception {
    ImportPackageDTO i;
    List<ImportPackageDTO> expected = null;

    perform(Constants.IMPORT_PACKAGE, "importPackages", "", expected);

    expected = new LinkedList<>();
    i = new ImportPackageDTO();
    i.packageName = "com.test";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 0;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 0;
    i.version.floor.minor = 0;
    i.version.floor.micro = 0;
    expected.add(i);
    perform(Constants.IMPORT_PACKAGE, "importPackages", "com.test", expected);

    expected = new LinkedList<>();
    i = new ImportPackageDTO();
    i.packageName = "com.test.*";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 0;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 1;
    i.version.floor.minor = 2;
    i.version.floor.micro = 0;
    i.version.floor.qualifier = "QUAL";
    i.version.ceiling = new VersionInRangeDTO();
    i.version.ceiling.include = false;
    i.version.ceiling.major = 2;
    i.version.ceiling.minor = 0;
    i.version.ceiling.micro = 0;
    i.version.ceiling.qualifier = "QUAL";
    i.bundleSymbolicName = "symbolic.test";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 1;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.arbitraryAttributes.put("condi1", "test");
    i.resolution = "optional";
    expected.add(i);
    i = new ImportPackageDTO();
    i.packageName = "com.test2";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 0;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 1;
    i.version.floor.minor = 2;
    i.version.floor.micro = 0;
    i.version.floor.qualifier = "QUAL";
    i.version.ceiling = new VersionInRangeDTO();
    i.version.ceiling.include = false;
    i.version.ceiling.major = 2;
    i.version.ceiling.minor = 0;
    i.version.ceiling.micro = 0;
    i.version.ceiling.qualifier = "QUAL";
    i.bundleSymbolicName = "symbolic.test";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 1;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.arbitraryAttributes.put("condi1", "test");
    i.resolution = "optional";
    expected.add(i);
    i = new ImportPackageDTO();
    i.packageName = "com.test3";
    i.bundleVersion = new VersionRangeDTO();
    i.bundleVersion.floor = new VersionInRangeDTO();
    i.bundleVersion.floor.include = true;
    i.bundleVersion.floor.major = 0;
    i.bundleVersion.floor.minor = 0;
    i.bundleVersion.floor.micro = 0;
    i.version = new VersionRangeDTO();
    i.version.floor = new VersionInRangeDTO();
    i.version.floor.include = true;
    i.version.floor.major = 0;
    i.version.floor.minor = 0;
    i.version.floor.micro = 0;
    i.arbitraryAttributes.put("condi2", "test2");
    expected.add(i);
    perform(Constants.IMPORT_PACKAGE, "importPackages",
        "com.test.*;com.test2;version='[1.2.0.QUAL,2.0.0.QUAL)';resolution:='optional';"
            + "bundle-symbolic-name='symbolic.test';condi1=test;directivetoignor:=test;bundle-version='1',com.test3;condi2=test2",
        expected);
  }

  public void testBundleActivationPolicy() throws Exception {
    ActivationPolicyDTO expected = null;
    perform(Constants.BUNDLE_ACTIVATIONPOLICY, "bundleActivationPolicy", "", expected);

    expected = new ActivationPolicyDTO();
    expected.policy = "lazy";
    expected.includes.add("com"); // we add this package in jar
    perform(Constants.BUNDLE_ACTIVATIONPOLICY, "bundleActivationPolicy", "lazy", expected);

    expected.excludes.add("test2");
    expected.excludes.add("test3");
    expected.includes.add("test");
    expected.includes.add("test1");
    expected.includes.remove("com");
    perform(Constants.BUNDLE_ACTIVATIONPOLICY, "bundleActivationPolicy",
        "lazy;include:='test,test1';exclude:='test2, test3'", expected);
  }

  public void testBundleLicenses() throws Exception {
    LicenseDTO l = new LicenseDTO();
    List<LicenseDTO> expected = null;
    perform(Constants.BUNDLE_LICENSE, "bundleLicenses", "", expected);

    expected = new LinkedList<>();
    l.name = "Apache";
    expected.add(l);
    perform(Constants.BUNDLE_LICENSE, "bundleLicenses", "Apache", expected);

    expected = new LinkedList<>();
    l.name = "Apache";
    l.description = "description";
    l.link = "www.google.com";
    expected.add(l);
    l = new LicenseDTO();
    l.name = "Apache2";
    expected.add(l);
    perform(Constants.BUNDLE_LICENSE, "bundleLicenses",
        "Apache;description='description';link='www.google.com',Apache2", expected);
  }

  public void testBundleManifestVersion() throws Exception {
    Integer expected = 1;
    perform(Constants.BUNDLE_MANIFESTVERSION, "bundleManifestVersion", "", expected);
    expected = 2;
    perform(Constants.BUNDLE_MANIFESTVERSION, "bundleManifestVersion", "2", expected);
  }

  public void testBundleName() throws Exception {
    String expected = null;
    perform(Constants.BUNDLE_NAME, "bundleName", "", expected);
    expected = "Test";
    perform(Constants.BUNDLE_NAME, "bundleName", "Test", expected);
  }

  public void testBundleNativeCode() throws Exception {
    VersionRangeDTO v;
    NativeCodeEntryDTO n;
    NativeCodeDTO expected = null;
    perform(Constants.BUNDLE_NATIVECODE, "bundleNativeCode", "", expected);

    expected = new NativeCodeDTO();
    n = new NativeCodeEntryDTO();
    n.paths.add("lib/http.dll");
    n.paths.add("lib/zlib.dll");
    n.osnames.add("Windows95");
    n.osnames.add("Windows98");
    n.osnames.add("WindowsNT");
    n.processors.add("x86");
    n.selectionFilters.add("(com.acme.windowing=win32)");
    n.languages.add("en");
    n.languages.add("se");
    expected.entries.add(n);
    n = new NativeCodeEntryDTO();
    n.paths.add("lib/solaris/libhttp.so");
    n.osnames.add("Solaris");
    n.osnames.add("SunOS");
    n.processors.add("sparc");
    expected.entries.add(n);
    n = new NativeCodeEntryDTO();
    n.paths.add("lib/linux/libhttp.so");
    n.osnames.add("Linux");
    n.processors.add("mips");
    n.selectionFilters.add("(com.acme.windowing=gtk)");
    v = new VersionRangeDTO();
    v.floor = new VersionInRangeDTO();
    v.floor.include = true;
    v.floor.major = 2;
    v.floor.minor = 0;
    v.floor.micro = 0;
    n.osversions.add(v);
    v = new VersionRangeDTO();
    v.floor = new VersionInRangeDTO();
    v.floor.include = true;
    v.floor.major = 1;
    v.floor.minor = 0;
    v.floor.micro = 0;
    v.ceiling = new VersionInRangeDTO();
    v.ceiling.include = false;
    v.ceiling.major = 3;
    v.ceiling.minor = 0;
    v.ceiling.micro = 0;
    n.osversions.add(v);
    expected.entries.add(n);
    expected.optional = true;
    perform(Constants.BUNDLE_NATIVECODE, "bundleNativeCode",
        "lib/http.dll; lib/zlib.dll;osname=Windows95;osname=Windows98;osversion='not';"
            + "osname=WindowsNT;processor=x86;selection-filter = '(com.acme.windowing=win32)';"
            + "language=en;language=se,"
            + "lib/solaris/libhttp.so;osname=Solaris;osname=SunOS;processor=sparc,"
            + "lib/linux/libhttp.so;osname=Linux;osversion='2';osversion='[1.0,3.0)';processor=mips;selection-filter='(com.acme.windowing=gtk)',*",
        expected);
  }

  public void testProvideCapabilities() throws Exception {
    TypedAttributeValueDTO t;
    ProvideCapabilityDTO c;
    List<ProvideCapabilityDTO> expected = null;

    perform(Constants.PROVIDE_CAPABILITY, "provideCapabilities", "", expected);

    expected = new LinkedList<>();
    c = new ProvideCapabilityDTO();
    c.namespace = "com.namespace";
    expected.add(c);
    perform(Constants.PROVIDE_CAPABILITY, "provideCapabilities", "com.namespace", expected);

    expected = new LinkedList<>();
    c = new ProvideCapabilityDTO();
    c.namespace = "com.namespace";
    c.arbitraryDirectives.put("custom", "test");
    c.effective = "other";
    c.uses.add("one");
    c.uses.add("two");
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("dde");
    c.typedAttributes.put("t", t);
    t = new TypedAttributeValueDTO();
    t.type = "Double";
    t.values.add("5.0");
    c.typedAttributes.put("tt", t);
    t = new TypedAttributeValueDTO();
    t.type = "Long";
    t.values.add("6");
    c.typedAttributes.put("ttt", t);
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("6");
    c.typedAttributes.put("tttt", t);
    t = new TypedAttributeValueDTO();
    t.type = "Version";
    t.values.add("6.0.0");
    c.typedAttributes.put("ttttt", t);
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("dd,e");
    c.typedAttributes.put("l:List", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "Double";
    t.values.add("5.0");
    t.values.add("4.0");
    c.typedAttributes.put("ll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "Long";
    t.values.add("6");
    t.values.add("7");
    c.typedAttributes.put("lll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "String";
    t.values.add("6");
    t.values.add("dede");
    c.typedAttributes.put("llll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "Version";
    t.values.add("6.0.0");
    t.values.add("1.2.0");
    c.typedAttributes.put("lllll", t);
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("not");
    c.typedAttributes.put("q:Strange", t);
    expected.add(c);
    perform(Constants.PROVIDE_CAPABILITY, "provideCapabilities",
        "com.namespace;custom:=test;effective:='other';uses:='one,two';t=dde;tt:Double=5;"
            + "ttt:Long=6;tttt:String=6;ttttt:Version=6;"
            + "l:List='dd,e';ll:List<Double>='5,4';lll:List<Long>='6,7';"
            + "llll:List<String>='6,dede';lllll:List<Version>='6,1.2';q:Strange=not",
        expected);
  }

  public void testRequireBundles() throws Exception {
    RequireBundleDTO b;
    List<RequireBundleDTO> expected = null;
    perform(Constants.REQUIRE_BUNDLE, "requireBundles", "", expected);

    expected = new LinkedList<>();
    b = new RequireBundleDTO();
    b.bundleSymbolicName = "bundle.symbolicName";
    b.bundleVersion = new VersionRangeDTO();
    b.bundleVersion.floor = new VersionInRangeDTO();
    b.bundleVersion.floor.major = 0;
    b.bundleVersion.floor.minor = 0;
    b.bundleVersion.floor.micro = 0;
    b.bundleVersion.floor.include = true;
    expected.add(b);
    perform(Constants.REQUIRE_BUNDLE, "requireBundles", "bundle.symbolicName", expected);

    expected = new LinkedList<>();
    b = new RequireBundleDTO();
    b.bundleSymbolicName = "bundle.symbolicName";
    b.bundleVersion = new VersionRangeDTO();
    b.bundleVersion.floor = new VersionInRangeDTO();
    b.bundleVersion.floor.major = 1;
    b.bundleVersion.floor.minor = 0;
    b.bundleVersion.floor.micro = 0;
    b.bundleVersion.floor.include = true;
    b.visibility = "reexport";
    b.resolution = "optional";
    expected.add(b);

    b = new RequireBundleDTO();
    b.bundleSymbolicName = "com.test";
    b.bundleVersion = new VersionRangeDTO();
    b.bundleVersion.floor = new VersionInRangeDTO();
    b.bundleVersion.floor.major = 0;
    b.bundleVersion.floor.minor = 0;
    b.bundleVersion.floor.micro = 0;
    b.bundleVersion.floor.include = true;
    b.arbitraryAttributes.put("abri", "v");
    expected.add(b);
    perform(Constants.REQUIRE_BUNDLE, "requireBundles",
        "bundle.symbolicName;visibility:='reexport';resolution:=optional;bundle-version=1,com.test;abri=v;notallow:=de",
        expected);
  }

  public void testRequireCapabilities() throws Exception {
    TypedAttributeValueDTO t;
    RequireCapabilityDTO c;
    List<RequireCapabilityDTO> expected = null;
    perform(Constants.REQUIRE_CAPABILITY, "requireCapabilities", "", expected);

    expected = new LinkedList<>();
    c = new RequireCapabilityDTO();
    c.namespace = "bundle.symbolicName";
    expected.add(c);
    perform(Constants.REQUIRE_CAPABILITY, "requireCapabilities", "bundle.symbolicName", expected);

    expected = new LinkedList<>();
    c = new RequireCapabilityDTO();
    c.namespace = "bundle.symbolicName";
    c.filter = "cool";
    c.cardinality = "multiple";
    c.effective = "other";
    c.resolution = "optional";
    t = new TypedAttributeValueDTO();
    t.type = "Double";
    t.values.add("5.0");
    c.typedAttributes.put("att", t);
    expected.add(c);
    c = new RequireCapabilityDTO();
    c.namespace = "com.test";
    expected.add(c);
    perform(Constants.REQUIRE_CAPABILITY, "requireCapabilities",
        "bundle.symbolicName;filter:=cool;cardinality:=multiple;effective:='other';resolution:=optional;att:Double=5,com.test",
        expected);

    expected = new LinkedList<>();
    c = new RequireCapabilityDTO();
    c.namespace = "com.namespace";
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("dde");
    c.typedAttributes.put("t", t);
    t = new TypedAttributeValueDTO();
    t.type = "Double";
    t.values.add("5.0");
    c.typedAttributes.put("tt", t);
    t = new TypedAttributeValueDTO();
    t.type = "Long";
    t.values.add("6");
    c.typedAttributes.put("ttt", t);
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("6");
    c.typedAttributes.put("tttt", t);
    t = new TypedAttributeValueDTO();
    t.type = "Version";
    t.values.add("6.0.0");
    c.typedAttributes.put("ttttt", t);
    t = new TypedAttributeValueDTO();
    t.type = "String";
    t.values.add("dd,e");
    c.typedAttributes.put("l:List", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "Double";
    t.values.add("5.0");
    t.values.add("4.0");
    c.typedAttributes.put("ll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "Long";
    t.values.add("6");
    t.values.add("7");
    c.typedAttributes.put("lll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "String";
    t.values.add("6");
    t.values.add("dede");
    c.typedAttributes.put("llll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = true;
    t.type = "Version";
    t.values.add("6.0.0");
    t.values.add("1.2.0");
    c.typedAttributes.put("lllll", t);
    t = new TypedAttributeValueDTO();
    t.multiValue = false;
    t.type = "String";
    t.values.add("not");
    c.typedAttributes.put("q:Strange", t);
    expected.add(c);
    perform(Constants.REQUIRE_CAPABILITY, "requireCapabilities",
        "com.namespace;custom:=test;t=dde;tt:Double=5;ttt:Long=6;tttt:String=6;ttttt:Version=6;"
            + "l:List='dd,e';ll:List<Double>='5,4';lll:List<Long>='6,7';llll:List<String>='6,dede';lllll:List<Version>='6,1.2';q:Strange=not",
        expected);
  }

  public void testBundleRequiredExecutionEnvironments() throws Exception {
    List<String> expected = null;
    perform(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "bundleRequiredExecutionEnvironments",
        "", expected);

    expected = new LinkedList<>();
    expected.add("one");
    perform(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "bundleRequiredExecutionEnvironments",
        "one", expected);

    expected.add("two");
    perform(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "bundleRequiredExecutionEnvironments",
        "one,two", expected);
  }

  public void testBundleSymbolicName() throws Exception {
    BundleSymbolicNameDTO expected = new BundleSymbolicNameDTO();
    expected.symbolicName = "!! MISSING !!";
    perform(Constants.BUNDLE_SYMBOLICNAME, "bundleSymbolicName", "", expected);

    expected = new BundleSymbolicNameDTO();
    expected.symbolicName = "com.symbolic.name";
    perform(Constants.BUNDLE_SYMBOLICNAME, "bundleSymbolicName", "com.symbolic.name", expected);

    expected = new BundleSymbolicNameDTO();
    expected.symbolicName = "com.symbolic.name";
    expected.singleton = true;
    expected.fragmentAttachment = "never";
    expected.mandatories.add("test");
    expected.mandatories.add("test2");
    expected.arbitraryAttributes.put("condi", "test");
    perform(Constants.BUNDLE_SYMBOLICNAME, "bundleSymbolicName",
        "com.symbolic.name;singleton:=true;fragment-attachment:=never;mandatory:='test, test2';condi=test;toignore:=test",
        expected);
  }

  public void testBundleVendor() throws Exception {
    String expected = null;
    perform(Constants.BUNDLE_VENDOR, "bundleVendor", "", expected);
    expected = "My vendor";
    perform(Constants.BUNDLE_VENDOR, "bundleVendor", "My vendor", expected);
  }

  public void testBundleScm() throws Exception {
    ScmDTO expected = null;
    perform(Constants.BUNDLE_SCM, "bundleScm", "", expected);

    expected = new ScmDTO();
    expected.connection = "test";
    expected.developerConnection = "test";
    expected.url = "test";
    expected.tag = "test";
    perform(Constants.BUNDLE_SCM, "bundleScm",
        "url=test,connection=test,developerConnection=test,tag=test,notallow=test", expected);
  }

  public void testBundleVersion() throws Exception {
    VersionDTO expected = new VersionDTO();
    expected.major = 0;
    expected.minor = 0;
    expected.micro = 0;
    perform(Constants.BUNDLE_VERSION, "bundleVersion", "", expected);

    expected = new VersionDTO();
    expected.major = 0;
    expected.minor = 0;
    expected.micro = 0;
    perform(Constants.BUNDLE_VERSION, "bundleVersion", "notver", expected);

    expected = new VersionDTO();
    expected.major = 1;
    expected.minor = 2;
    expected.micro = 0;
    perform(Constants.BUNDLE_VERSION, "bundleVersion", "1.2", expected);

    expected = new VersionDTO();
    expected.major = 1;
    expected.minor = 2;
    expected.micro = 0;
    expected.qualifier = "45689-SNAPSHOT";
    perform(Constants.BUNDLE_VERSION, "bundleVersion", "1.2.0.45689-SNAPSHOT", expected);

    expected = new VersionDTO();
    expected.major = 0;
    expected.minor = 0;
    expected.micro = 0;
    perform(Constants.BUNDLE_VERSION, "bundleVersion", "1.2-SNAPSHOT", expected);
  }

  public void testExtract() {
    final Jar jar = new Jar("jar");
    final Manifest manifest = new Manifest();
    jar.setManifest(manifest);
    final Processor p = new Processor();
    assertNotNull(HeadersHelper.extract(jar, Locale.forLanguageTag("und"), p));
    assertTrue(p.isOk());
  }

  public void perform(final String headerName, final String entryName, final String header,
      final Object expected) throws Exception {
    final Jar jar = new Jar("jar");
    final Map<String, Resource> dir = new HashMap<>();
    dir.put("com/test", new FileResource(new File("")));
    jar.addDirectory(dir, true);
    final Manifest manifest = new Manifest();
    jar.setManifest(manifest);
    manifest.getMainAttributes().putValue(headerName, header);
    final Processor p = new Processor();
    final HeadersHelper extractor =
        new HeadersHelper(ManifestHelper.get(jar, Locale.forLanguageTag("und")), jar, p);
    final Object actual = extractor.getClass().getDeclaredMethod("_" + entryName).invoke(extractor);
    assertTrue(p.isOk());
    final JSONCodec c = new JSONCodec();
    final StringWriter es = new StringWriter();
    final StringWriter as = new StringWriter();

    c.enc().to(es).put(expected);
    c.enc().to(as).put(actual);
    assertEquals(c.dec().from(es.toString()).get(), c.dec().from(as.toString()).get());
  }
}
