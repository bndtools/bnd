package biz.aQute.bnd.reporter.helpers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
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
import biz.aQute.bnd.reporter.manifest.dto.VersionRangeDTO;

/**
 * Helper to convert manifest headers into DTO.
 */
public class HeadersHelper {

	static public String convertBundleActivator(final Parameters header) {
		if (!header.isEmpty()) {
			return cleanKey(header.keySet()
				.iterator()
				.next());
		} else {
			return null;
		}
	}

	static public List<String> convertBundleCategories(final Parameters header) {
		final List<String> categories = new LinkedList<>();

		for (final String category : cleanKey(header.keySet())) {
			categories.add(category);
		}

		return !categories.isEmpty() ? categories : null;
	}

	static public List<String> convertBundleClassPaths(final Parameters header) {
		final List<String> classpaths = new LinkedList<>();

		for (final String classpath : cleanKey(header.keySet())) {
			classpaths.add(classpath);
		}

		if (classpaths.isEmpty()) {
			classpaths.add(".");
		}

		return classpaths;
	}

	static public ContactAddressDTO convertBundleContactAddress(final Parameters header) {
		final String contact = header.toString();
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

	static public String convertBundleCopyright(final Parameters header) {
		final String copyright = header.toString();

		return !copyright.isEmpty() ? copyright : null;
	}

	static public String convertBundleDescription(final Parameters header) {
		final String description = header.toString();

		return !description.isEmpty() ? description : null;
	}

	static public List<DeveloperDTO> convertBundleDevelopers(final Parameters header) {
		final List<DeveloperDTO> developers = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final DeveloperDTO developer = new DeveloperDTO();

			developer.identifier = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("email")) {
				developer.email = entry.getValue()
					.get("email");
			}

			if (entry.getValue()
				.containsKey("name")) {
				developer.name = entry.getValue()
					.get("name");
			}

			if (entry.getValue()
				.containsKey("organization")) {
				developer.organization = entry.getValue()
					.get("organization");
			}

			if (entry.getValue()
				.containsKey("organizationUrl")) {
				developer.organizationUrl = entry.getValue()
					.get("organizationUrl");
			}

			if (entry.getValue()
				.containsKey("timezone")) {
				if (isInteger(entry.getValue()
					.get("timezone"))) {
					developer.timezone = Integer.valueOf(entry.getValue()
						.get("timezone"));
				} else {
					developer.timezone = Integer.valueOf((int) TimeUnit.HOURS.convert(TimeZone
						.getTimeZone(entry.getValue()
							.get("timezone"))
						.getRawOffset(), TimeUnit.MILLISECONDS));
				}
			}

			if (entry.getValue()
				.containsKey("roles")) {
				for (final String role : entry.getValue()
					.get("roles")
					.split(",")) {
					developer.roles.add(role.trim());
				}
			}
			developers.add(developer);
		}

		return !developers.isEmpty() ? developers : null;
	}

	static public String convertBundleDocURL(final Parameters header) {
		final String docUrl = header.toString();

		return !docUrl.isEmpty() ? docUrl : null;
	}

	static public List<DynamicImportPackageDTO> convertDynamicImportPackages(final Parameters header) {
		final List<DynamicImportPackageDTO> imports = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final DynamicImportPackageDTO myImport = new DynamicImportPackageDTO();

			myImport.packageName = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("bundle-symbolic-name")) {
				myImport.bundleSymbolicName = entry.getValue()
					.get("bundle-symbolic-name");
			}

			myImport.version = VersionHelper.toRange(entry.getValue()
				.get("version", ""));
			if (myImport.version == null) {
				myImport.version = VersionHelper.createDefaultRange();
			}

			myImport.bundleVersion = VersionHelper.toRange(entry.getValue()
				.get("bundle-version", ""));
			if (myImport.bundleVersion == null) {
				myImport.bundleVersion = VersionHelper.createDefaultRange();
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("bundle-symbolic-name");
			attribute.remove("version");
			attribute.remove("bundle-version");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey()
					.endsWith(":")) {
					myImport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			imports.add(myImport);
		}

		return !imports.isEmpty() ? imports : null;
	}

	static public List<ExportPackageDTO> convertExportPackages(final Parameters header) {
		final List<ExportPackageDTO> exports = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final ExportPackageDTO myExport = new ExportPackageDTO();

			myExport.packageName = cleanKey(entry.getKey());

			myExport.version = VersionHelper.toVersion(entry.getValue()
				.get("version", ""));
			if (myExport.version == null) {
				myExport.version = VersionHelper.createDefaultVersion();
			}

			if (entry.getValue()
				.containsKey("exclude:")) {
				for (final String c : entry.getValue()
					.get("exclude:")
					.split(",")) {
					myExport.excludes.add(c.trim());
				}
			}

			if (entry.getValue()
				.containsKey("include:")) {
				for (final String c : entry.getValue()
					.get("include:")
					.split(",")) {
					myExport.includes.add(c.trim());
				}
			}

			if (entry.getValue()
				.containsKey("mandatory:")) {
				for (final String c : entry.getValue()
					.get("mandatory:")
					.split(",")) {
					myExport.mandatories.add(c.trim());
				}
			}

			if (entry.getValue()
				.containsKey("uses:")) {
				for (final String c : entry.getValue()
					.get("uses:")
					.split(",")) {
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
				if (!a.getKey()
					.endsWith(":")) {
					myExport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			exports.add(myExport);
		}

		return !exports.isEmpty() ? exports : null;
	}

	static public FragmentHostDTO convertFragmentHost(final Parameters header) {
		if (!header.isEmpty()) {
			final Attrs attibutes = header.values()
				.iterator()
				.next();
			final FragmentHostDTO frag = new FragmentHostDTO();

			frag.bundleSymbolicName = header.keySet()
				.iterator()
				.next();

			if (attibutes.containsKey("extension:")) {
				frag.extension = attibutes.get("extension:");
			} else {
				frag.extension = "framework";
			}

			frag.bundleVersion = VersionHelper.toRange(attibutes.get("bundle-version", ""));
			if (frag.bundleVersion == null) {
				frag.bundleVersion = VersionHelper.createDefaultRange();
			}

			attibutes.remove("bundle-version");
			attibutes.remove("extension:");

			for (final Entry<String, String> a : attibutes.entrySet()) {
				if (!a.getKey()
					.endsWith(":")) {
					frag.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());

				}
			}
			return frag;
		} else {
			return null;
		}
	}

	static public List<IconDTO> convertBundleIcons(final Parameters header) {
		final List<IconDTO> icons = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final IconDTO icon = new IconDTO();

			icon.url = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("size")) {
				icon.size = Integer.valueOf(entry.getValue()
					.get("size"));
			}
			icons.add(icon);
		}

		return !icons.isEmpty() ? icons : null;
	}

	static public List<ImportPackageDTO> convertImportPackages(final Parameters header) {
		final List<ImportPackageDTO> imports = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final ImportPackageDTO myImport = new ImportPackageDTO();

			myImport.packageName = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("resolution:")) {
				myImport.resolution = entry.getValue()
					.get("resolution:");
			} else {
				myImport.resolution = "mandatory";
			}

			if (entry.getValue()
				.containsKey("bundle-symbolic-name")) {
				myImport.bundleSymbolicName = entry.getValue()
					.get("bundle-symbolic-name");
			}

			myImport.version = VersionHelper.toRange(entry.getValue()
				.get("version", ""));
			if (myImport.version == null) {
				myImport.version = VersionHelper.createDefaultRange();
			}

			myImport.bundleVersion = VersionHelper.toRange(entry.getValue()
				.get("bundle-version", ""));
			if (myImport.bundleVersion == null) {
				myImport.bundleVersion = VersionHelper.createDefaultRange();
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("bundle-symbolic-name");
			attribute.remove("version");
			attribute.remove("bundle-version");
			attribute.remove("resolution:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey()
					.endsWith(":")) {
					myImport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			imports.add(myImport);
		}

		return !imports.isEmpty() ? imports : null;
	}

	static public ActivationPolicyDTO convertBundleActivationPolicy(final Parameters header,
		final List<String> defaultPackages) {
		if (!header.isEmpty()) {
			final Attrs attributes = header.values()
				.iterator()
				.next();
			final ActivationPolicyDTO act = new ActivationPolicyDTO();

			act.policy = "lazy";

			if (attributes.containsKey("exclude:")) {
				for (final String a : attributes.get("exclude:")
					.split(",")) {
					act.excludes.add(a.trim());
				}
			}

			if (attributes.containsKey("include:")) {
				for (final String a : attributes.get("include:")
					.split(",")) {
					act.includes.add(a.trim());
				}
			} else {
				act.includes.addAll(defaultPackages);
			}
			return act;
		} else {
			return null;
		}
	}

	static public List<LicenseDTO> convertBundleLicenses(final Parameters header) {
		final List<LicenseDTO> licences = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final LicenseDTO licence = new LicenseDTO();

			licence.name = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("description")) {
				licence.description = entry.getValue()
					.get("description");
			}

			if (entry.getValue()
				.containsKey("link")) {
				licence.link = entry.getValue()
					.get("link");
			}
			licences.add(licence);
		}

		return !licences.isEmpty() ? licences : null;
	}

	static public Integer convertBundleManifestVersion(final Parameters header) {
		if (!header.isEmpty()) {
			return Integer.valueOf(cleanKey(header.keySet()
				.iterator()
				.next()));
		} else {
			return Integer.valueOf(1);
		}
	}

	static public String convertBundleName(final Parameters header) {
		final String name = header.toString();

		return !name.isEmpty() ? name : null;
	}

	static public NativeCodeDTO convertBundleNativeCode(final Parameters header) {
		if (!header.isEmpty()) {
			final Map<Attrs, NativeCodeEntryDTO> storedAttr = new HashMap<>();
			final NativeCodeDTO nativeCode = new NativeCodeDTO();

			for (final Entry<String, Attrs> entry : header.entrySet()) {
				if (entry.getKey()
					.equals("*")) {
					nativeCode.optional = true;
				} else {
					NativeCodeEntryDTO nEntry = storedAttr.get(entry.getValue());
					if (nEntry == null) {
						nEntry = new NativeCodeEntryDTO();
						storedAttr.put(entry.getValue(), nEntry);

						String key = "osname";
						while (entry.getValue()
							.get(key) != null) {
							nEntry.osnames.add(entry.getValue()
								.get(key));
							key = key + "~";
						}

						key = "language";
						while (entry.getValue()
							.get(key) != null) {
							nEntry.languages.add(entry.getValue()
								.get(key));
							key = key + "~";
						}

						key = "processor";
						while (entry.getValue()
							.get(key) != null) {
							nEntry.processors.add(entry.getValue()
								.get(key));
							key = key + "~";
						}

						key = "selection-filter";
						while (entry.getValue()
							.get(key) != null) {
							nEntry.selectionFilters.add(entry.getValue()
								.get(key));
							key = key + "~";
						}

						key = "osversion";
						while (entry.getValue()
							.get(key) != null) {
							final VersionRangeDTO r = VersionHelper.toRange(entry.getValue()
								.get(key, ""));
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

	static public List<ProvideCapabilityDTO> convertProvideCapabilities(final Parameters header) {
		final List<ProvideCapabilityDTO> capas = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final ProvideCapabilityDTO capa = new ProvideCapabilityDTO();

			capa.namespace = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("uses:")) {
				for (final String c : entry.getValue()
					.get("uses:")
					.split(",")) {
					capa.uses.add(c.trim());
				}
			}

			if (entry.getValue()
				.containsKey("effective:")) {
				capa.effective = entry.getValue()
					.get("effective:");
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("uses:");
			attribute.remove("effective:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (a.getKey()
					.endsWith(":")) {
					capa.arbitraryDirectives.put(removeSpecial(a.getKey()
						.substring(0, a.getKey()
							.length() - 1)),
						a.getValue());
				}
			}

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey()
					.endsWith(":")) {
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

	static public List<RequireBundleDTO> convertRequireBundles(final Parameters header) {
		final List<RequireBundleDTO> reqs = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final RequireBundleDTO req = new RequireBundleDTO();

			req.bundleSymbolicName = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("resolution:")) {
				req.resolution = entry.getValue()
					.get("resolution:");
			}

			if (entry.getValue()
				.containsKey("visibility:")) {
				req.visibility = entry.getValue()
					.get("visibility:");
			}

			req.bundleVersion = VersionHelper.toRange(entry.getValue()
				.get("bundle-version", ""));
			if (req.bundleVersion == null) {
				req.bundleVersion = VersionHelper.createDefaultRange();
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("bundle-version");
			attribute.remove("resolution:");
			attribute.remove("visibility:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey()
					.endsWith(":")) {
					req.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			reqs.add(req);
		}

		return !reqs.isEmpty() ? reqs : null;
	}

	static public List<RequireCapabilityDTO> convertRequireCapabilities(final Parameters header) {
		final List<RequireCapabilityDTO> capas = new LinkedList<>();

		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final RequireCapabilityDTO capa = new RequireCapabilityDTO();

			capa.namespace = cleanKey(entry.getKey());

			if (entry.getValue()
				.containsKey("filter:")) {
				capa.filter = entry.getValue()
					.get("filter:");
			}

			if (entry.getValue()
				.containsKey("resolution:")) {
				capa.resolution = entry.getValue()
					.get("resolution:");
			}

			if (entry.getValue()
				.containsKey("cardinality:")) {
				capa.cardinality = entry.getValue()
					.get("cardinality:");
			}

			if (entry.getValue()
				.containsKey("effective:")) {
				capa.effective = entry.getValue()
					.get("effective:");
			}

			final Attrs attribute = new Attrs(entry.getValue());
			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey()
					.endsWith(":")) {
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

	static public List<String> convertBundleRequiredExecutionEnvironments(final Parameters header) {
		final List<String> execs = new LinkedList<>();

		for (final String e : cleanKey(header.keySet())) {
			execs.add(e);
		}

		return !execs.isEmpty() ? execs : null;
	}

	static public ScmDTO convertBundleSCM(final Parameters header) {
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

	static public BundleSymbolicNameDTO convertBundleSymbolicName(final Parameters header) {
		if (!header.isEmpty()) {
			final Attrs attributes = header.values()
				.iterator()
				.next();
			final BundleSymbolicNameDTO bsn = new BundleSymbolicNameDTO();

			bsn.symbolicName = cleanKey(header.keySet()
				.iterator()
				.next());

			if (attributes.containsKey("mandatory:")) {
				for (final String c : attributes.get("mandatory:")
					.split(",")) {
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
				if (!a.getKey()
					.endsWith(":")) {
					bsn.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			return bsn;
		} else {
			return null;
		}
	}

	static public String convertBundleVendor(final Parameters header) {
		final String vendor = header.toString();

		return !vendor.isEmpty() ? vendor : null;
	}

	static public String convertBundleUpdateLocation(final Parameters header) {
		final String updateLocation = header.toString();

		return !updateLocation.isEmpty() ? updateLocation : null;
	}

	static public String convertBundleLocalization(final Parameters header) {
		final String localization = header.toString();

		return !localization.isEmpty() ? localization : "OSGI-INF/l10n/bundle";
	}

	static public VersionDTO convertBundleVersion(final Parameters header) {
		if (!header.isEmpty()) {
			final VersionDTO version = VersionHelper.toVersion(cleanKey(header.keySet()
				.iterator()
				.next()));
			if (version == null) {
				return VersionHelper.createDefaultVersion();
			} else {
				return version;
			}
		} else {
			return VersionHelper.createDefaultVersion();
		}
	}

	static private String removeSpecial(final String key) {
		String result = key;
		if (key != null) {
			while (!result.isEmpty() && !Character.isLetterOrDigit(result.charAt(0))) {
				result = result.substring(1, result.length());
			}
		}
		return result;
	}

	@SuppressWarnings("unused")
	static private boolean isUrl(final String value) {
		try {
			new URL(value);
			return true;
		} catch (final MalformedURLException e) {
			return false;
		}
	}

	static private boolean isEmail(final String value) {
		if (!value.contains(" ") && value.matches(".+@.+")) {
			return true;
		} else {
			return false;
		}
	}

	static private String cleanKey(final String key) {
		return Processor.removeDuplicateMarker(key);
	}

	static private List<String> cleanKey(final Set<String> keys) {
		final List<String> result = new LinkedList<>();
		if (keys != null) {
			for (final String key : keys) {
				result.add(cleanKey(key));
			}
		}
		return result;
	}

	static private boolean isInteger(final String s) {
		try {
			Integer.parseInt(s);
		} catch (@SuppressWarnings("unused")
		final NumberFormatException e) {
			return false;
		}
		return true;
	}
}
