package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.ADDED;
import static aQute.bnd.service.diff.Delta.MAJOR;
import static aQute.bnd.service.diff.Delta.MICRO;
import static aQute.bnd.service.diff.Delta.MINOR;
import static aQute.bnd.service.diff.Delta.REMOVED;
import static aQute.bnd.service.diff.Delta.UNCHANGED;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Differ;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.bnd.unmodifiable.Sets;
import aQute.bnd.version.Version;
import aQute.libg.generics.Create;
import aQute.service.reporter.Reporter;;

/**
 * This class maintains
 */
public class Baseline {
	private final static Logger			logger			= LoggerFactory.getLogger(Baseline.class);
	private final static Set<String>	BASELINEIGNORE	= Sets.of("aQute.bnd.annotation.baseline.BaselineIgnore");

	public static class Info {
		public String				packageName;
		public Diff					packageDiff;
		public Collection<String>	providers;
		public Map<String, String>	attributes;
		public Version				newerVersion;
		public Version				olderVersion;
		public Version				suggestedVersion;
		public Version				suggestedIfProviders;
		public boolean				mismatch;
		public String				warning	= "";
		public String				reason;
	}

	public static class BundleInfo {
		public String	bsn;
		public Version	olderVersion;
		public Version	newerVersion;
		public Version	suggestedVersion;
		public boolean	mismatch;
		public String	reason;
	}

	final Differ		differ;
	final Reporter		bnd;
	final BundleInfo	binfo	= new BundleInfo();

	Diff				diff;
	Set<Info>			infos;
	String				bsn;
	Version				newerVersion;
	Version				olderVersion;
	Version				suggestedVersion;
	String				releaseRepository;
	boolean				includeZeroMajor;

	public Baseline(Reporter bnd, Differ differ) throws IOException {
		this.differ = differ;
		this.bnd = bnd;
	}

	/**
	 * This method compares a jar to a baseline jar and returns version
	 * suggestions if the baseline does not agree with the newer jar. The
	 * returned set contains all the exported packages.
	 *
	 * @param newer
	 * @param older
	 * @return null if ok, otherwise a set of suggested versions for all
	 *         packages (also the ones that were ok).
	 * @throws Exception
	 */
	public Set<Info> baseline(Jar newer, Jar older, Instructions packageFilters) throws Exception {
		Tree n = differ.tree(newer);
		Parameters nExports = getExports(newer);
		Tree o = differ.tree(older);
		Parameters oExports = getExports(older);
		if (packageFilters == null)
			packageFilters = new Instructions();

		return baseline(n, nExports, o, oExports, packageFilters);
	}

	public Set<Info> baseline(Tree n, Parameters nExports, Tree o, Parameters oExports, Instructions packageFilters)
		throws Exception {
		diff = n.diff(o);
		Diff apiDiff = diff.get("<api>");
		infos = Create.set();

		bsn = getBsn(n);

		newerVersion = getVersion(n);
		olderVersion = getVersion(o);

		// Parse includeZeroMajor configuration from packageFilters
		includeZeroMajor = getIncludeZeroMajor(packageFilters);

		final boolean binfoMismatch = mismatch(olderVersion, newerVersion);

		boolean firstRelease = false;
		if (o.get("<manifest>") == null) {
			firstRelease = true;
			if (newerVersion.equals(Version.emptyVersion)) {
				newerVersion = Version.ONE;
			}
		}
		Delta highestDelta = UNCHANGED;
		for (Diff pdiff : apiDiff.getChildren()) {
			if (pdiff.getType() != Type.PACKAGE) // Just packages
				continue;

			if (pdiff.getName()
				.startsWith("java."))
				continue;

			var matcher = packageFilters.matcher(pdiff.getName());
			if (!packageFilters.isEmpty() && (matcher == null || matcher.isNegated()))
				continue;

			var threshold = getThreshold(packageFilters, matcher);

			final Info info = new Info();
			infos.add(info);
			info.reason = getRootCauses(pdiff);
			info.packageDiff = pdiff;
			info.packageName = pdiff.getName();
			info.attributes = nExports.get(info.packageName);
			logger.debug("attrs for {} {}", info.packageName, info.attributes);

			info.newerVersion = getVersion(info.attributes);
			info.olderVersion = getVersion(oExports.get(info.packageName));
			Delta delta = pdiff.getDelta();
			if (delta == UNCHANGED) {
				info.suggestedVersion = info.olderVersion;
				// Fix previously released package containing version qualifier
				if (info.olderVersion.getQualifier() != null) {
					info.suggestedVersion = bump(MICRO, info.olderVersion, 1, 0);
					info.warning += "Found package version with qualifier. Bumping micro version";
				} else if (!info.newerVersion.equals(info.olderVersion)) {
					info.warning += "No difference but versions are not equal";
				}
			} else if (delta == REMOVED) {
				info.suggestedVersion = null;
			} else if (delta == ADDED) {
				info.suggestedVersion = info.newerVersion;
			} else { // We have an API change
				// Process @BaselineIgnore annotations
				delta = pdiff.getDelta(diff -> {
					switch (diff.getDelta()) {
						case UNCHANGED :
						case IGNORED :
							return false;
						default :
							break;
					}

					if (threshold != null && diff.getDelta().compareTo(threshold) < 0) {
						return true;
					}

					switch (diff.getType()) {
						case PACKAGE :
						case INTERFACE :
						case ANNOTATION :
						case CLASS :
						case ENUM :
						case FIELD :
						case METHOD :
							boolean ignore = diff.getChildren()
								.stream()
								.filter(child -> (child.getType() == Type.ANNOTATED)
									&& BASELINEIGNORE.contains(child.getName()))
								.flatMap(child -> child.getChildren()
									.stream())
								.filter(child -> child.getType() == Type.PROPERTY)
								.map(Diff::getName)
								.filter(property -> property.startsWith("value='"))
								.map(property -> property.substring(7, property.length() - 1))
								.anyMatch(version -> {
									try {
										return Version.valueOf(version)
											.compareTo(info.olderVersion) > 0;
									} catch (Exception e) {
										bnd.exception(e,
											"BaselineIgnore unable to compare specified version %s to baseline package version %s",
											version, info.olderVersion);
										return false;
									}
								});
							return ignore;
						default :
							return false;
					}
				});

				info.suggestedVersion = bump(delta, info.olderVersion, 1, 0);

				if (info.newerVersion.compareTo(info.suggestedVersion) < 0) {
					// our suggested version is greater than the new version!
					info.mismatch = mismatch(info.olderVersion, info.newerVersion);

					// We can fix some major problems by assuming
					// that an interface is a provider interface
					if (delta == MAJOR) {

						info.providers = Create.set();
						if (info.attributes != null)
							info.providers
								.addAll(Processor.split(info.attributes.get(Constants.PROVIDER_TYPE_DIRECTIVE)));

						// Calculate the new delta assuming we fix all the major
						// interfaces by making them providers
						Delta otherDelta = pdiff.getDelta(diff -> {
							if (diff.getType() == Type.INTERFACE && diff.getDelta() == MAJOR) {
								info.providers.add(Descriptors.getShortName(diff.getName()));
								return true;
							}
							return false;
						});

						if (otherDelta != MAJOR) {
							info.suggestedIfProviders = bump(MINOR, info.olderVersion, 1, 0);
						}
					}
				}
			}
			Delta content = switch (delta) {
				case IGNORED, UNCHANGED -> UNCHANGED;
				case ADDED -> MINOR;
				case CHANGED -> MICRO; // cannot happen
				case MICRO, MINOR, MAJOR -> delta;
				case REMOVED -> MAJOR;
				default -> MAJOR;
			};


			if (threshold != null && content.compareTo(threshold) < 0) {
				content = UNCHANGED;
			}

			if (content.compareTo(highestDelta) > 0) {
				highestDelta = content;
			}
		}
		// If this is a first release, or the base has a different symbolic
		// name, then the newer version must be ok. Otherwise the version bump
		// for bundles with the same symbolic name should be at least as big as
		// the biggest semantic change
		if (firstRelease || !bsn.equals(getBsn(o))) {
			suggestedVersion = newerVersion;
		} else {
			suggestedVersion = bumpBundle(highestDelta, olderVersion, 1, 0);
			if (suggestedVersion.compareTo(newerVersion) < 0)
				suggestedVersion = newerVersion;
		}

		binfo.bsn = bsn;
		binfo.suggestedVersion = suggestedVersion;
		binfo.olderVersion = olderVersion;
		binfo.newerVersion = newerVersion;

		if (newerVersion.getWithoutQualifier()
			.equals(olderVersion.getWithoutQualifier())) {
			// We have a special case, the current and repository revisions
			// have the same version, this happens after a release, only want
			// to generate an error when they really differ.

			if (getDiff().getDelta() == UNCHANGED)
				return infos;
		}

		// Ok, now our bundle version must be >= the suggestedVersion
		if (newerVersion.getWithoutQualifier()
			.compareTo(getSuggestedVersion()) < 0) {
			binfo.mismatch = binfoMismatch;
			binfo.reason = getRootCauses(apiDiff);
		}

		return infos;
	}

	private boolean getIncludeZeroMajor(Instructions packageFilters) {
		if (packageFilters == null || packageFilters.isEmpty())
			return false;
		// Check if any instruction has includezeromajor attribute set to true
		for (var entry : packageFilters.entrySet()) {
			var attrs = entry.getValue();
			if (attrs != null) {
				var value = attrs.get(Constants.DIFFPACKAGES_INCLUDE_ZERO_MAJOR);
				if (value != null && "true".equalsIgnoreCase(value)) {
					return true;
				}
			}
		}
		return false;
	}

	private Delta getThreshold(Instructions packageFilters, Instruction matcher) {
		if (matcher == null)
			return null;
		var attrs = packageFilters.get(matcher);
		assert attrs != null : "guaranteed by the matcher != null";
		var threshold = attrs.getOrDefault(Constants.DIFFPACKAGES_THRESHOLD, "MICRO")
			.toUpperCase();
		try {
			return Delta.valueOf(threshold);
		}
		catch (IllegalArgumentException e) {
			bnd.error("baseline.threshold baseline threshold specified as [%s] but does not correspond to a Delta enum - ignoring", threshold);
		}
		return null;
	}

	/**
	 * "Major version zero (0.y.z) is for initial development. Anything may
	 * change at any time. The public API should not be considered stable."
	 * <p>
	 * This method returns {@code true} if baselining should report mismatches
	 * for the given versions. By default, it returns {@code false} for versions
	 * with major version 0 (unless {@code includeZeroMajor} is enabled).
	 *
	 * @see <a href="https://semver.org/#spec-item-4">SemVer</a>
	 */
	private boolean mismatch(Version older, Version newer) {
		if (includeZeroMajor) {
			// When includeZeroMajor is enabled, only exclude versions where both are 0.0.x
			return !(older.getMajor() == 0 && older.getMinor() == 0 && newer.getMajor() == 0 && newer.getMinor() == 0);
		}
		// Default behavior: exclude all versions with major version 0
		return older.getMajor() > 0 && newer.getMajor() > 0;
	}

	private String getRootCauses(Diff apiDiff) {
		try (Formatter f = new Formatter()) {
			getRootCauses(f, apiDiff, "");
			return f.toString();
		}
	}

	/*
	 * Calculate the root causes for this diff. This means that we descent
	 * through diffs with the same level (MAJOR/MINOR/MICRO) until we find an
	 * add/remove. The first add/remove is seen as the root cause.
	 */
	private void getRootCauses(Formatter f, Diff diff, String path) {
		for (Diff child : diff.getChildren()) {
			String cpath = path + "/" + child.getName();
			if (child.getDelta() == diff.getDelta()) {
				getRootCauses(f, child, cpath);
			} else {
				if (child.getDelta() == ADDED) {
					f.format("+ %s\n", cpath);
				} else if (child.getDelta() == REMOVED) {
					f.format("- %s\n", cpath);
				}
			}
		}
	}

	/**
	 * Gets the generated diff
	 *
	 * @return the diff
	 */
	public Diff getDiff() {
		return diff;
	}

	public Set<Info> getPackageInfos() {
		if (infos == null)
			return Collections.emptySet();
		return infos;
	}

	public String getBsn() {
		return bsn;
	}

	public Version getSuggestedVersion() {
		return suggestedVersion;
	}

	public void setSuggestedVersion(Version suggestedVersion) {
		this.suggestedVersion = suggestedVersion;
	}

	public Version getNewerVersion() {
		return newerVersion;
	}

	public Version getOlderVersion() {
		return olderVersion;
	}

	public String getReleaseRepository() {
		return releaseRepository;
	}

	public void setReleaseRepository(String releaseRepository) {
		this.releaseRepository = releaseRepository;
	}

	private Version bump(Delta delta, Version last, int offset, int base) {
		return switch (delta) {
			case UNCHANGED, IGNORED -> last;
			case MINOR -> new Version(last.getMajor(), last.getMinor() + offset, base);
			case MAJOR -> new Version(last.getMajor() + 1, base, base);
			case ADDED -> last;
			default -> new Version(last.getMajor(), last.getMinor(), last.getMicro() + offset);
		};
	}

	private Version getVersion(Map<String, String> map) {
		if (map == null)
			return Version.LOWEST;

		return Version.parseVersion(map.get(Constants.VERSION_ATTRIBUTE));
	}

	private Parameters getExports(Jar jar) throws Exception {
		Manifest m = jar.getManifest();
		if (m == null)
			return new Parameters();

		return OSGiHeader.parseHeader(m.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE));
	}

	private Version getVersion(Tree top) {
		Tree manifest = top.get("<manifest>");
		if (manifest == null) {
			return Version.emptyVersion;
		}
		for (Tree tree : manifest.getChildren()) {
			if (tree.getName()
				.startsWith(Constants.BUNDLE_VERSION)) {
				return Version.parseVersion(tree.getName()
					.substring(15));
			}
		}
		return Version.emptyVersion;
	}

	private String getBsn(Tree top) {
		Tree manifest = top.get("<manifest>");
		if (manifest == null) {
			return "";
		}
		for (Tree tree : manifest.getChildren()) {
			if (tree.getName()
				.startsWith(Constants.BUNDLE_SYMBOLICNAME) && tree.getChildren().length > 0) {
				return tree.getChildren()[0].getName();
			}
		}
		return "";
	}

	private Version bumpBundle(Delta delta, Version last, int offset, int base) {
		return switch (delta) {
			case MINOR -> new Version(last.getMajor(), last.getMinor() + offset, base);
			case MAJOR -> new Version(last.getMajor() + offset, base, base);
			case ADDED -> new Version(last.getMajor(), last.getMinor() + offset, base);
			default -> new Version(last.getMajor(), last.getMinor(), last.getMicro() + offset);
		};
	}

	public BundleInfo getBundleInfo() {
		return binfo;
	}
}
