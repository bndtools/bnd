package biz.aQute.bnd.reporter.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

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
import biz.aQute.bnd.reporter.manifest.dto.ProvideCapabilityDTO;
import biz.aQute.bnd.reporter.manifest.dto.RequireBundleDTO;
import biz.aQute.bnd.reporter.manifest.dto.RequireCapabilityDTO;
import biz.aQute.bnd.reporter.manifest.dto.ScmDTO;
import biz.aQute.bnd.reporter.manifest.dto.TypedAttributeValueDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionInRangeDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionRangeDTO;

/**
 * Helper to extract (localized) and convert manifest headers into DTO. The extraction process
 * computes default values when possible.
 */
public class HeadersHelper {

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
   * @return a map of DTO headers
   */
  static public Map<String, Object> extract(final Jar jar, final Locale locale,
      final Reporter reporter) {
    Objects.requireNonNull(jar, "jar");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(reporter, "reporter");

    final Map<String, Object> headers = new LinkedHashMap<>();

    final ManifestHelper manifest = ManifestHelper.get(jar, locale);
    if (manifest != null) {
      final HeadersHelper e = new HeadersHelper(manifest, jar, reporter);
      for (final Method m : e.getClass().getDeclaredMethods()) {
        if (m.getName().startsWith("_")) {
          Object dto;
          try {
            dto = m.invoke(e);
            if (dto != null) {
              headers.put(m.getName().substring(1), dto);
            }
          } catch (IllegalAccessException | IllegalArgumentException
              | InvocationTargetException exception) {
            reporter.exception(exception, "bnd bug while converting manfinest to DTO");
          }
        }
      }
    }
    return headers;
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

  protected Object _bundleActivator() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_ACTIVATOR, false);
    if (!header.isEmpty()) {
      result = cleanKey(header.keySet().iterator().next());
    }
    return result;
  }

  protected Object _bundleCategories() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_CATEGORY, false);
    final List<String> categories = new LinkedList<>();
    for (final String category : cleanKey(header.keySet())) {
      categories.add(category);
    }
    if (!categories.isEmpty()) {
      result = categories;
    }
    return result;
  }

  protected Object _bundleClasspaths() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_CLASSPATH, false);

    final List<String> classpaths = new LinkedList<>();
    for (final String classpath : cleanKey(header.keySet())) {
      classpaths.add(classpath);
    }
    if (classpaths.isEmpty()) {
      classpaths.add(".");
    }
    result = classpaths;
    return result;
  }

  protected Object _bundleContactAddress() {
    Object result = null;

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
      result = adress;
    }

    return result;
  }

  protected Object _bundleCopyright() {
    Object result = null;
    final String copyright = _manifest.getHeaderAsString(Constants.BUNDLE_COPYRIGHT);
    if (!copyright.isEmpty()) {
      result = copyright;
    }
    return result;
  }

  protected Object _bundleDescription() {
    Object result = null;
    final String description = _manifest.getHeaderAsString(Constants.BUNDLE_DESCRIPTION);
    if (!description.isEmpty()) {
      result = description;
    }
    return result;
  }

  protected Object _bundleDevelopers() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_DEVELOPERS, false);
    final List<DeveloperDTO> developers = new LinkedList<>();
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
    if (!developers.isEmpty()) {
      result = developers;
    }
    return result;
  }

  protected Object _bundleDocUrl() {
    Object result = null;
    final String docUrl = _manifest.getHeaderAsString(Constants.BUNDLE_DOCURL);
    if (!docUrl.isEmpty()) {
      result = docUrl;
    }
    return result;
  }

  protected Object _dynamicImportPackages() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.DYNAMICIMPORT_PACKAGE, false);
    final List<DynamicImportPackageDTO> imports = new LinkedList<>();
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
    if (!imports.isEmpty()) {
      result = imports;
    }
    return result;
  }

  protected Object _exportPackages() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.EXPORT_PACKAGE, false);
    final List<ExportPackageDTO> exports = new LinkedList<>();
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
    if (!exports.isEmpty()) {
      result = exports;
    }
    return result;
  }

  protected Object _fragmentHost() {
    Object result = null;
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
      result = frag;
    }
    return result;
  }

  protected Object _bundleIcons() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_ICON, false);
    final List<IconDTO> icons = new LinkedList<>();
    for (final Entry<String, Attrs> entry : header.entrySet()) {
      final IconDTO icon = new IconDTO();

      icon.url = cleanKey(entry.getKey());

      if (entry.getValue().containsKey("size")) {
        icon.size = Integer.valueOf(entry.getValue().get("size"));
      }
      icons.add(icon);
    }
    if (!icons.isEmpty()) {
      result = icons;
    }
    return result;
  }

  protected Object _importPackages() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.IMPORT_PACKAGE, false);
    final List<ImportPackageDTO> imports = new LinkedList<>();
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
    if (!imports.isEmpty()) {
      result = imports;
    }
    return result;
  }

  protected Object _bundleActivationPolicy() {
    Object result = null;
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
      result = act;
    }
    return result;
  }

  protected Object _bundleLicenses() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_LICENSE, false);
    final List<LicenseDTO> licences = new LinkedList<>();
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
    if (!licences.isEmpty()) {
      result = licences;
    }
    return result;
  }

  protected Object _bundleManifestVersion() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_MANIFESTVERSION, false);
    if (!header.isEmpty()) {
      result = Integer.valueOf(cleanKey(header.keySet().iterator().next()));
    } else {
			result = Integer.valueOf(1);
    }
    return result;
  }

  protected Object _bundleName() {
    Object result = null;
    final String name = _manifest.getHeaderAsString(Constants.BUNDLE_NAME);
    if (!name.isEmpty()) {
      result = name;
    }
    return result;
  }

  protected Object _bundleNativeCode() {
    Object result = null;
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
      result = nativeCode;
    }
    return result;
  }

  protected Object _provideCapabilities() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.PROVIDE_CAPABILITY, false);
    final List<ProvideCapabilityDTO> capas = new LinkedList<>();
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
    if (!capas.isEmpty()) {
      result = capas;
    }
    return result;
  }

  protected Object _requireBundles() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.REQUIRE_BUNDLE, false);
    final List<RequireBundleDTO> reqs = new LinkedList<>();
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
    if (!reqs.isEmpty()) {
      result = reqs;
    }
    return result;
  }

  protected Object _requireCapabilities() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.REQUIRE_CAPABILITY, false);
    final List<RequireCapabilityDTO> capas = new LinkedList<>();
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
    if (!capas.isEmpty()) {
      result = capas;
    }
    return result;
  }

  protected Object _bundleRequiredExecutionEnvironments() {
    Object result = null;
    final Parameters header =
        _manifest.getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, true);
    final List<String> execs = new LinkedList<>();
    for (final String e : cleanKey(header.keySet())) {
      execs.add(e);
    }
    if (!execs.isEmpty()) {
      result = execs;
    }
    return result;
  }

  protected Object _bundleScm() {
    Object result = null;
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
      result = scmDto;
    }
    return result;
  }

  protected Object _bundleSymbolicName() {
    Object result = null;
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
      result = bsn;
    } else {
      final BundleSymbolicNameDTO bsn = new BundleSymbolicNameDTO();
      bsn.symbolicName = "!! MISSING !!";
      _reporter.warning("the bundle does not declare a symbolic name");
      result = bsn;
    }
    return result;
  }

  protected Object _bundleVendor() {
    Object result = null;
    final String vendor = _manifest.getHeaderAsString(Constants.BUNDLE_VENDOR);
    if (!vendor.isEmpty()) {
      result = vendor;
    }
    return result;
  }

  protected Object _bundleVersion() {
    Object result = null;
    final Parameters header = _manifest.getHeader(Constants.BUNDLE_VERSION, false);
    if (!header.isEmpty()) {
      final VersionDTO version = toVersion(cleanKey(header.keySet().iterator().next()));
      if (version == null) {
        result = getDefaultVersion();
      } else {
        result = version;
      }
    } else {
      result = getDefaultVersion();
    }
    return result;
  }
}
