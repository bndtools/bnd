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
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Differ;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.bnd.version.Version;
import aQute.libg.generics.Create;
import aQute.service.reporter.Reporter;

/**
 * This class maintains
 */
public class Baseline {
	private final static Logger logger = LoggerFactory.getLogger(Baseline.class);

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

		@Deprecated
		public Version	version;
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

			if (!packageFilters.matches(pdiff.getName()))
				continue;

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
					switch (diff.getType()) {
						case ANNOTATION :
						case INTERFACE :
						case CLASS :
						case ENUM :
						case FIELD :
						case METHOD :
							boolean ignore = diff.getChildren()
								.stream()
								.filter(child -> (child.getType() == Type.ANNOTATED) && child.getName()
									.equals("aQute.bnd.annotation.baseline.BaselineIgnore"))
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
						Delta tryDelta = pdiff.getDelta(diff -> {
							if (diff.getType() == Type.INTERFACE && diff.getDelta() == MAJOR) {
								info.providers.add(Descriptors.getShortName(diff.getName()));
								return true;
							}
							return false;
						});

						if (tryDelta != MAJOR) {
							info.suggestedIfProviders = bump(tryDelta, info.olderVersion, 1, 0);
						}
					}
				}
			}
			Delta content;
			switch (delta) {
				case IGNORED :
				case UNCHANGED :
					content = UNCHANGED;
					break;

				case ADDED :
					content = MINOR;
					break;

				case CHANGED : // cannot happen
					content = MICRO;
					break;

				case MICRO :
				case MINOR :
				case MAJOR :
					content = delta;
					break;

				case REMOVED :
				default :
					content = MAJOR;
					break;
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
		binfo.version = binfo.olderVersion = olderVersion;
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

	/**
	 * "Major version zero (0.y.z) is for initial development. Anything may
	 * change at any time. The public API should not be considered stable."
	 *
	 * @see <a href="https://semver.org/#spec-item-4">SemVer</a>
	 */
	private boolean mismatch(Version older, Version newer) {
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
		switch (delta) {
			case UNCHANGED :
				return last;
			case MINOR :
				return new Version(last.getMajor(), last.getMinor() + offset, base);
			case MAJOR :
				return new Version(last.getMajor() + 1, base, base);
			case ADDED :
				return last;
			default :
				return new Version(last.getMajor(), last.getMinor(), last.getMicro() + offset);
		}
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
		switch (delta) {
			case MINOR :
				return new Version(last.getMajor(), last.getMinor() + offset, base);
			case MAJOR :
				return new Version(last.getMajor() + offset, base, base);
			case ADDED :
				return new Version(last.getMajor(), last.getMinor() + offset, base);
			default :
				return new Version(last.getMajor(), last.getMinor(), last.getMicro() + offset);
		}
	}

	public BundleInfo getBundleInfo() {
		return binfo;
	}
}
