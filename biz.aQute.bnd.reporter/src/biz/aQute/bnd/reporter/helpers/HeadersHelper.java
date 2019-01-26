package biz.aQute.bnd.reporter.helpers;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.service.reporter.Reporter;
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
import biz.aQute.bnd.reporter.manifest.dto.OSGiHeadersDTO;
import biz.aQute.bnd.reporter.manifest.dto.ProvideCapabilityDTO;
import biz.aQute.bnd.reporter.manifest.dto.RequireBundleDTO;
import biz.aQute.bnd.reporter.manifest.dto.RequireCapabilityDTO;
import biz.aQute.bnd.reporter.manifest.dto.ScmDTO;
import biz.aQute.bnd.reporter.manifest.dto.TypedAttributeValueDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionInRangeDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionRangeDTO;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * Helper to extract (localized) and convert manifest headers into DTO. The extraction process
 * computes default values when possible.
 */
final public class HeadersHelper {

  private final ManifestHelper _manifest;
  private final Jar _jar;
  private final Reporter _reporter;

  protected HeadersHelper(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
    _manifest = manifest;
    _jar = jar;
    _reporter = reporter;
  }


  /**
   * Extract and convert the manifest headers from the jar in arguments into DTO. The extraction
   * process computes default values when possible.
   * 
   * @param jar the jar which contains the manifest headers to extract, must not {@code null}
   * @param locale the locale used to localize the extraction, must not be {@code null}
   * @param reporter the reporter to report error, must not be {@code null}
   * @return a DTO or {@code null} if the Jar does not contain a manifest
   */
  static public OSGiHeadersDTO extract(final Jar jar, final Locale locale,
      final Reporter reporter) {
    Objects.requireNonNull(jar, "jar");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(reporter, "reporter");

    final ManifestHelper manifest = ManifestHelper.createIfPresent(jar, locale);
    if (manifest != null) {
      final HeadersHelper helper = new HeadersHelper(manifest, jar, reporter);
      final OSGiHeadersDTO headers = new OSGiHeadersDTO();

      headers.bundleName = helper.extractBundleName();
      headers.bundleDescription = helper.extractBundleDescription();
      headers.bundleVersion = helper.extractBundleVersion();
      headers.bundleCategories = helper.extractBundleCategory();
      headers.bundleIcons = helper.extractBundleIcon();
      headers.bundleDocURL = helper.extractBundleDocURL();
      headers.bundleUpdateLocation = helper.extractBundleUpdateLocation();
      headers.bundleLocalization = helper.extractBundleLocalization();
      headers.bundleLicenses = helper.extractBundleLicense();
      headers.bundleDevelopers = helper.extractBundleDevelopers();
      headers.bundleSCM = helper.extractBundleSCM();
      headers.bundleCopyright = helper.extractBundleCopyright();
      headers.bundleVendor = helper.extractBundleVendor();
      headers.bundleContactAddress = helper.extractBundleContactAddress();
      headers.bundleSymbolicName = helper.extractBundleSymbolicName();
      headers.importPackages = helper.extractImportPackage();
      headers.dynamicImportPackages = helper.extractDynamicImportPackage();
      headers.exportPackages = helper.extractExportPackage();
      headers.provideCapabilities = helper.extractProvideCapability();
      headers.requireCapabilities = helper.extractRequireCapability();
      headers.requireBundles = helper.extractRequireBundle();
      headers.bundleRequiredExecutionEnvironments =
          helper.extractBundleRequiredExecutionEnvironment();
      headers.bundleActivationPolicy = helper.extractBundleActivationPolicy();
      headers.fragmentHost = helper.extractFragmentHost();
      headers.bundleActivator = helper.extractBundleActivator();
      headers.bundleClassPaths = helper.extractBundleClassPath();
      headers.bundleNativeCode = helper.extractBundleNativeCode();
      headers.bundleManifestVersion = helper.extractBundleManifestVersion();

      return headers;
    } else {
      return null;
    }
  }

  private VersionRangeDTO toOsgiRange(final String value) {
    if (value != null && VersionRange.isOSGiVersionRange(value)) {
      return toRange(VersionRange.parseOSGiVersionRange(value));
    } else {
      return null;
    }
  }

  private VersionRangeDTO toRange(final VersionRange range) {
    final VersionRangeDTO result = new VersionRangeDTO();

    result.floor = new VersionInRangeDTO();
    result.floor.include = range.includeLow();
    result.floor.major = range.getLow().getMajor();
    result.floor.minor = Integer.valueOf(range.getLow().getMinor());
    result.floor.micro = Integer.valueOf(range.getLow().getMicro());
    if (range.getLow().getQualifier() != null && !range.getLow().getQualifier().isEmpty()) {
      result.floor.qualifier = range.getLow().getQualifier();
    }

    if (!range.isSingleVersion()) {
      result.ceiling = new VersionInRangeDTO();
      result.ceiling.include = range.includeHigh();
      result.ceiling.major = range.getHigh().getMajor();
      result.ceiling.minor = Integer.valueOf(range.getHigh().getMinor());
      result.ceiling.micro = Integer.valueOf(range.getHigh().getMicro());
      if (range.getHigh().getQualifier() != null && !range.getHigh().getQualifier().isEmpty()) {
        result.ceiling.qualifier = range.getHigh().getQualifier();
      }
    }

    return result;
  }

  private VersionDTO toVersion(final String value) {
    if (value != null && Version.isVersion(value)) {
      final Version version = Version.parseVersion(value);
      final VersionDTO result = new VersionDTO();

      result.major = version.getMajor();
      result.minor = Integer.valueOf(version.getMinor());
      result.micro = Integer.valueOf(version.getMicro());
      if (version.getQualifier() != null && !version.getQualifier().isEmpty()) {
        result.qualifier = version.getQualifier();
      }

      return result;
    } else {
      return null;
    }
  }

  private VersionRangeDTO getDefaultRange() {
    final VersionRangeDTO range = new VersionRangeDTO();

    range.floor = new VersionInRangeDTO();
    range.floor.major = 0;
    range.floor.minor = Integer.valueOf(0);
    range.floor.micro = Integer.valueOf(0);
    range.floor.include = true;

    return range;
  }

  private VersionDTO getDefaultVersion() {
    final VersionDTO version = new VersionDTO();

    version.major = 0;
    version.minor = Integer.valueOf(0);
    version.micro = Integer.valueOf(0);

    return version;
  }

  private String removeSpecial(final String key) {
    String result = key;
    if (key != null) {
      while (!result.isEmpty() && !Character.isLetterOrDigit(result.charAt(0))) {
        result = result.substring(1, result.length());
      }
    }
    return result;
  }

  private String cleanKey(final String key) {
    String result = key;
    if (key != null) {
      while (result.endsWith("~")) {
        result = result.substring(0, result.length() - 1);
      }
    }
    return result;
  }

  private List<String> cleanKey(final Set<String> keys) {
    final List<String> result = new LinkedList<>();
    if (keys != null) {
      for (final String key : keys) {
        result.add(cleanKey(key));
      }
    }
    return result;
  }

  @SuppressWarnings("unused")
  private boolean isUrl(final String value) {
    try {
      new URL(value);
      return true;
    } catch (final MalformedURLException e) {
      return false;
    }
  }

  private boolean isEmail(final String value) {
    if (!value.contains(" ") && value.matches(".+@.+")) {
      return true;
    } else {
      return false;
    }
  }


  /*
   * The following methods are protected to be used in unit tests. For method to be tested name it
   * extract<the header name without "-">.
   */

  protected String extractBundleActivator() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_ACTIVATOR, false);
    if (!header.isEmpty()) {
      return cleanKey(header.keySet().iterator().next());
    } else {
      return null;
    }
  }

  protected List<String> extractBundleCategory() {
    final List<String> categories = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_CATEGORY, false);
    for (final String category : cleanKey(header.keySet())) {
      categories.add(category);
    }

    return !categories.isEmpty() ? categories : null;
  }

  protected List<String> extractBundleClassPath() {
    final List<String> classpaths = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_CLASSPATH, false);
    for (final String classpath : cleanKey(header.keySet())) {
      classpaths.add(classpath);
    }

    if (classpaths.isEmpty()) {
      classpaths.add(".");
    }

    return classpaths;
  }

  protected ContactAddressDTO extractBundleContactAddress() {
    final String contact = _manifest.getHeaderAsString(Constants.BUNDLE_CONTACTADDRESS);
    if (!contact.isEmpty()) {
      final ContactAddressDTO adress = new ContactAddressDTO();
      adress.address = contact;
      if (isUrl(contact)) {
        adress.type = "url";
      } else if (isEmail(contact)) {
        adress.type = "email";
      } else {
        adress.type = "postal";
      }
      return adress;
    } else {
      return null;
    }
  }

  protected String extractBundleCopyright() {
    final String copyright = _manifest.getHeaderAsString(Constants.BUNDLE_COPYRIGHT);

    return !copyright.isEmpty() ? copyright : null;
  }

  protected String extractBundleDescription() {
    final String description = _manifest.getHeaderAsString(Constants.BUNDLE_DESCRIPTION);

    return !description.isEmpty() ? description : null;
  }

  protected List<DeveloperDTO> extractBundleDevelopers() {
    final List<DeveloperDTO> developers = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_DEVELOPERS, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final DeveloperDTO developer = new DeveloperDTO();

      developer.identifier = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("email")) {
        developer.email = entry.getValue().get("email");
      }

      if (entry.getValue().containsKey("name")) {
        developer.name = entry.getValue().get("name");
      }

      if (entry.getValue().containsKey("organization")) {
        developer.organization = entry.getValue().get("organization");
      }

      if (entry.getValue().containsKey("organizationUrl")) {
        developer.organizationUrl = entry.getValue().get("organizationUrl");
      }

      if (entry.getValue().containsKey("timezone")) {
        developer.timezone = Integer.valueOf(entry.getValue().get("timezone"));
      }

      if (entry.getValue().containsKey("roles")) {
        for (final String role : entry.getValue().get("roles").split(",")) {
          developer.roles.add(role.trim());
        }
      }
      developers.add(developer);
    }

    return !developers.isEmpty() ? developers : null;
  }

  protected String extractBundleDocURL() {
    final String docUrl = _manifest.getHeaderAsString(Constants.BUNDLE_DOCURL);

    return !docUrl.isEmpty() ? docUrl : null;
  }

  protected List<DynamicImportPackageDTO> extractDynamicImportPackage() {
    final List<DynamicImportPackageDTO> imports = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.DYNAMICIMPORT_PACKAGE, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final DynamicImportPackageDTO myImport = new DynamicImportPackageDTO();

      myImport.packageName = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("bundle-symbolic-name")) {
        myImport.bundleSymbolicName = entry.getValue().get("bundle-symbolic-name");
      }

      myImport.version = toOsgiRange(entry.getValue().get("version", ""));
      if (myImport.version == null) {
        myImport.version = getDefaultRange();
      }

      myImport.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));
      if (myImport.bundleVersion == null) {
        myImport.bundleVersion = getDefaultRange();
      }

      final Attrs attribute = new Attrs(entry.getValue());
      attribute.remove("bundle-symbolic-name");
      attribute.remove("version");
      attribute.remove("bundle-version");

      for (final Entry<String, String> a : attribute.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          myImport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
        }
      }
      imports.add(myImport);
    }

    return !imports.isEmpty() ? imports : null;
  }

  protected List<ExportPackageDTO> extractExportPackage() {
    final List<ExportPackageDTO> exports = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.EXPORT_PACKAGE, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final ExportPackageDTO myExport = new ExportPackageDTO();

      myExport.packageName = cleanKey(entry.getKey());

      myExport.version = toVersion(entry.getValue().get("version", ""));
      if (myExport.version == null) {
        myExport.version = getDefaultVersion();
      }

      if (entry.getValue().containsKey("exclude:")) {
        for (final String c : entry.getValue().get("exclude:").split(",")) {
          myExport.excludes.add(c.trim());
        }
      }

      if (entry.getValue().containsKey("include:")) {
        for (final String c : entry.getValue().get("include:").split(",")) {
          myExport.includes.add(c.trim());
        }
      }

      if (entry.getValue().containsKey("mandatory:")) {
        for (final String c : entry.getValue().get("mandatory:").split(",")) {
          myExport.mandatories.add(c.trim());
        }
      }

      if (entry.getValue().containsKey("uses:")) {
        for (final String c : entry.getValue().get("uses:").split(",")) {
          myExport.uses.add(c.trim());
        }
      }

      final Attrs attribute = new Attrs(entry.getValue());
      attribute.remove("version");
      attribute.remove("exclude:");
      attribute.remove("include:");
      attribute.remove("mandatory:");
      attribute.remove("uses:");

      for (final Entry<String, String> a : attribute.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          myExport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
        }
      }
      exports.add(myExport);
    }

    return !exports.isEmpty() ? exports : null;
  }

  protected FragmentHostDTO extractFragmentHost() {
    final Parameters header = _manifest.getHeader(Constants.FRAGMENT_HOST, false);
    if (!header.isEmpty()) {
      final Attrs attibutes = header.values().iterator().next();
      final FragmentHostDTO frag = new FragmentHostDTO();

      frag.bundleSymbolicName = header.keySet().iterator().next();

      if (attibutes.containsKey("extension:")) {
        frag.extension = attibutes.get("extension:");
      } else {
        frag.extension = "framework";
      }

      frag.bundleVersion = toOsgiRange(attibutes.get("bundle-version", ""));
      if (frag.bundleVersion == null) {
        frag.bundleVersion = getDefaultRange();
      }

      attibutes.remove("bundle-version");
      attibutes.remove("extension:");

      for (final Entry<String, String> a : attibutes.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          frag.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());

        }
      }
      return frag;
    } else {
      return null;
    }
  }

  protected List<IconDTO> extractBundleIcon() {
    final List<IconDTO> icons = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_ICON, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final IconDTO icon = new IconDTO();

      icon.url = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("size")) {
        icon.size = Integer.valueOf(entry.getValue().get("size"));
      }
      icons.add(icon);
    }

    return !icons.isEmpty() ? icons : null;
  }

  protected List<ImportPackageDTO> extractImportPackage() {
    final List<ImportPackageDTO> imports = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.IMPORT_PACKAGE, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final ImportPackageDTO myImport = new ImportPackageDTO();

      myImport.packageName = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("resolution:")) {
        myImport.resolution = entry.getValue().get("resolution:");
      } else {
        myImport.resolution = "mandatory";
      }

      if (entry.getValue().containsKey("bundle-symbolic-name")) {
        myImport.bundleSymbolicName = entry.getValue().get("bundle-symbolic-name");
      }

      myImport.version = toOsgiRange(entry.getValue().get("version", ""));
      if (myImport.version == null) {
        myImport.version = getDefaultRange();
      }

      myImport.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));
      if (myImport.bundleVersion == null) {
        myImport.bundleVersion = getDefaultRange();
      }

      final Attrs attribute = new Attrs(entry.getValue());
      attribute.remove("bundle-symbolic-name");
      attribute.remove("version");
      attribute.remove("bundle-version");
      attribute.remove("resolution:");

      for (final Entry<String, String> a : attribute.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          myImport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
        }
      }
      imports.add(myImport);
    }

    return !imports.isEmpty() ? imports : null;
  }

  protected ActivationPolicyDTO extractBundleActivationPolicy() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_ACTIVATIONPOLICY, false);
    if (!header.isEmpty()) {
      final Attrs attributes = header.values().iterator().next();
      final ActivationPolicyDTO act = new ActivationPolicyDTO();

      act.policy = "lazy";

      if (attributes.containsKey("exclude:")) {
        for (final String a : attributes.get("exclude:").split(",")) {
          act.excludes.add(a.trim());
        }
      }

      if (attributes.containsKey("include:")) {
        for (final String a : attributes.get("include:").split(",")) {
          act.includes.add(a.trim());
        }
      } else {
        for (final String a : _jar.getPackages()) {
          act.includes.add(a);
        }
      }
      return act;
    } else {
      return null;
    }
  }

  protected List<LicenseDTO> extractBundleLicense() {
    final List<LicenseDTO> licences = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_LICENSE, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final LicenseDTO licence = new LicenseDTO();

      licence.name = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("description")) {
        licence.description = entry.getValue().get("description");
      }

      if (entry.getValue().containsKey("link")) {
        licence.link = entry.getValue().get("link");
      }
      licences.add(licence);
    }

    return !licences.isEmpty() ? licences : null;
  }

  protected Integer extractBundleManifestVersion() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_MANIFESTVERSION, false);
    if (!header.isEmpty()) {
      return Integer.valueOf(cleanKey(header.keySet().iterator().next()));
    } else {
      return Integer.valueOf(1);
    }
  }

  protected String extractBundleName() {
    final String name = _manifest.getHeaderAsString(Constants.BUNDLE_NAME);

    return !name.isEmpty() ? name : null;
  }

  protected NativeCodeDTO extractBundleNativeCode() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_NATIVECODE, true);
    if (!header.isEmpty()) {
      final Map<Attrs, NativeCodeEntryDTO> storedAttr = new HashMap<>();
      final NativeCodeDTO nativeCode = new NativeCodeDTO();

      for (final Entry<String, Attrs> entry : header.entrySet()) {
        if (entry.getKey().equals("*")) {
          nativeCode.optional = true;
        } else {
          NativeCodeEntryDTO nEntry = storedAttr.get(entry.getValue());
          if (nEntry == null) {
            nEntry = new NativeCodeEntryDTO();
            storedAttr.put(entry.getValue(), nEntry);

            String key = "osname";
            while (entry.getValue().get(key) != null) {
              nEntry.osnames.add(entry.getValue().get(key));
              key = key + "~";
            }

            key = "language";
            while (entry.getValue().get(key) != null) {
              nEntry.languages.add(entry.getValue().get(key));
              key = key + "~";
            }

            key = "processor";
            while (entry.getValue().get(key) != null) {
              nEntry.processors.add(entry.getValue().get(key));
              key = key + "~";
            }

            key = "selection-filter";
            while (entry.getValue().get(key) != null) {
              nEntry.selectionFilters.add(entry.getValue().get(key));
              key = key + "~";
            }

            key = "osversion";
            while (entry.getValue().get(key) != null) {
              final VersionRangeDTO r = toOsgiRange(entry.getValue().get(key, ""));
              if (r != null) {
                nEntry.osversions.add(r);
              }
              key = key + "~";
            }

            nativeCode.entries.add(nEntry);
          }
          nEntry.paths.add(cleanKey(entry.getKey()));
        }
      }
      return nativeCode;
    } else {
      return null;
    }
  }

  protected List<ProvideCapabilityDTO> extractProvideCapability() {
    final List<ProvideCapabilityDTO> capas = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.PROVIDE_CAPABILITY, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final ProvideCapabilityDTO capa = new ProvideCapabilityDTO();

      capa.namespace = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("uses:")) {
        for (final String c : entry.getValue().get("uses:").split(",")) {
          capa.uses.add(c.trim());
        }
      }

      if (entry.getValue().containsKey("effective:")) {
        capa.effective = entry.getValue().get("effective:");
      }

      final Attrs attribute = new Attrs(entry.getValue());
      attribute.remove("uses:");
      attribute.remove("effective:");

      for (final Entry<String, String> a : attribute.entrySet()) {
        if (a.getKey().endsWith(":")) {
          capa.arbitraryDirectives
              .put(removeSpecial(a.getKey().substring(0, a.getKey().length() - 1)), a.getValue());
        }
      }

      for (final Entry<String, String> a : attribute.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          final Object val = attribute.getTyped(a.getKey());
          final TypedAttributeValueDTO ta = new TypedAttributeValueDTO();

          if (attribute.getType(a.getKey()) == Type.DOUBLE) {
            ta.values.add(val.toString());
            ta.type = "Double";
          } else if (attribute.getType(a.getKey()) == Type.LONG) {
            ta.values.add(val.toString());
            ta.type = "Long";
          } else if (attribute.getType(a.getKey()) == Type.STRING) {
            ta.values.add(val.toString());
            ta.type = "String";
          } else if (attribute.getType(a.getKey()) == Type.VERSION) {
            ta.values.add(val.toString());
            ta.type = "Version";
          } else if (attribute.getType(a.getKey()) == Type.DOUBLES) {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "Double";
            ta.multiValue = true;
          } else if (attribute.getType(a.getKey()) == Type.LONGS) {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "Long";
            ta.multiValue = true;
          } else if (attribute.getType(a.getKey()) == Type.STRINGS) {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "String";
            ta.multiValue = true;
          } else {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "Version";
            ta.multiValue = true;
          }

          capa.typedAttributes.put(a.getKey(), ta);
        }
      }
      capas.add(capa);
    }

    return !capas.isEmpty() ? capas : null;
  }

  protected List<RequireBundleDTO> extractRequireBundle() {
    final List<RequireBundleDTO> reqs = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.REQUIRE_BUNDLE, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final RequireBundleDTO req = new RequireBundleDTO();

      req.bundleSymbolicName = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("resolution:")) {
        req.resolution = entry.getValue().get("resolution:");
      }

      if (entry.getValue().containsKey("visibility:")) {
        req.visibility = entry.getValue().get("visibility:");
      }

      req.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));
      if (req.bundleVersion == null) {
        req.bundleVersion = getDefaultRange();
      }

      final Attrs attribute = new Attrs(entry.getValue());
      attribute.remove("bundle-version");
      attribute.remove("resolution:");
      attribute.remove("visibility:");

      for (final Entry<String, String> a : attribute.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          req.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
        }
      }
      reqs.add(req);
    }

    return !reqs.isEmpty() ? reqs : null;
  }

  protected List<RequireCapabilityDTO> extractRequireCapability() {
    final List<RequireCapabilityDTO> capas = new LinkedList<>();
    final Parameters header = _manifest.getHeader(Constants.REQUIRE_CAPABILITY, false);
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final RequireCapabilityDTO capa = new RequireCapabilityDTO();

      capa.namespace = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("filter:")) {
        capa.filter = entry.getValue().get("filter:");
      }

      if (entry.getValue().containsKey("resolution:")) {
        capa.resolution = entry.getValue().get("resolution:");
      }

      if (entry.getValue().containsKey("cardinality:")) {
        capa.cardinality = entry.getValue().get("cardinality:");
      }

      if (entry.getValue().containsKey("effective:")) {
        capa.effective = entry.getValue().get("effective:");
      }

      final Attrs attribute = new Attrs(entry.getValue());
      for (final Entry<String, String> a : attribute.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          final Object val = attribute.getTyped(a.getKey());
          final TypedAttributeValueDTO ta = new TypedAttributeValueDTO();

          if (attribute.getType(a.getKey()) == Type.DOUBLE) {
            ta.values.add(val.toString());
            ta.type = "Double";
          } else if (attribute.getType(a.getKey()) == Type.LONG) {
            ta.values.add(val.toString());
            ta.type = "Long";
          } else if (attribute.getType(a.getKey()) == Type.STRING) {
            ta.values.add(val.toString());
            ta.type = "String";
          } else if (attribute.getType(a.getKey()) == Type.VERSION) {
            ta.values.add(val.toString());
            ta.type = "Version";
          } else if (attribute.getType(a.getKey()) == Type.DOUBLES) {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "Double";
            ta.multiValue = true;
          } else if (attribute.getType(a.getKey()) == Type.LONGS) {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "Long";
            ta.multiValue = true;
          } else if (attribute.getType(a.getKey()) == Type.STRINGS) {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "String";
            ta.multiValue = true;
          } else {
            for (final Object v : (Collection<?>) val) {
              ta.values.add(v.toString());
            }
            ta.type = "Version";
            ta.multiValue = true;
          }

          capa.typedAttributes.put(a.getKey(), ta);
        }
      }
      capas.add(capa);
    }

    return !capas.isEmpty() ? capas : null;
  }

  protected List<String> extractBundleRequiredExecutionEnvironment() {
    final List<String> execs = new LinkedList<>();
    final Parameters header =
        _manifest.getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, true);
    for (final String e : cleanKey(header.keySet())) {
      execs.add(e);
    }

    return !execs.isEmpty() ? execs : null;
  }

  protected ScmDTO extractBundleSCM() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_SCM, false);
    final String scm = header.toString();
    if (scm.length() > 0) {
      final ScmDTO scmDto = new ScmDTO();
      final Attrs attrs = OSGiHeader.parseProperties(scm);

      for (final String key : attrs.keySet()) {
        if (key.equals("url")) {
          scmDto.url = attrs.get(key);
        } else if (key.equals("connection")) {
          scmDto.connection = attrs.get(key);
        } else if (key.equals("developerConnection")) {
          scmDto.developerConnection = attrs.get(key);
        } else if (key.equals("tag")) {
          scmDto.tag = attrs.get(key);
        }
      }
      return scmDto;
    } else {
      return null;
    }
  }

  protected BundleSymbolicNameDTO extractBundleSymbolicName() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_SYMBOLICNAME, false);
    if (!header.isEmpty()) {
      final Attrs attributes = header.values().iterator().next();
      final BundleSymbolicNameDTO bsn = new BundleSymbolicNameDTO();

      bsn.symbolicName = cleanKey(header.keySet().iterator().next());

      if (attributes.containsKey("mandatory:")) {
        for (final String c : attributes.get("mandatory:").split(",")) {
          bsn.mandatories.add(c.trim());
        }
      }

      if (attributes.containsKey("fragment-attachment:")) {
        bsn.fragmentAttachment = attributes.get("fragment-attachment:");
      }

      if (attributes.containsKey("singleton:")) {
        bsn.singleton = Boolean.parseBoolean(attributes.get("singleton:"));
      }

      attributes.remove("fragment-attachment:");
      attributes.remove("mandatory:");
      attributes.remove("singleton:");

      for (final Entry<String, String> a : attributes.entrySet()) {
        if (!a.getKey().endsWith(":")) {
          bsn.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
        }
      }
      return bsn;
    } else {
      final BundleSymbolicNameDTO bsn = new BundleSymbolicNameDTO();
      bsn.symbolicName = "!! MISSING !!";
      _reporter.warning("the bundle does not declare a symbolic name");
      return bsn;
    }
  }

  protected String extractBundleVendor() {
    final String vendor = _manifest.getHeaderAsString(Constants.BUNDLE_VENDOR);

    return !vendor.isEmpty() ? vendor : null;
  }

  protected String extractBundleUpdateLocation() {
    final String updateLocation = _manifest.getHeaderAsString(Constants.BUNDLE_UPDATELOCATION);

    return !updateLocation.isEmpty() ? updateLocation : null;
  }

  protected String extractBundleLocalization() {
    final String localization = _manifest.getHeaderAsString(Constants.BUNDLE_LOCALIZATION);

    return !localization.isEmpty() ? localization : "OSGI-INF/l10n/bundle";
  }

  protected VersionDTO extractBundleVersion() {
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_VERSION, false);
    if (!header.isEmpty()) {
      final VersionDTO version = toVersion(cleanKey(header.keySet().iterator().next()));
      if (version == null) {
        return getDefaultVersion();
      } else {
        return version;
      }
    } else {
      return getDefaultVersion();
    }
  }
}
