package aQute.bnd.differ;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.service.diff.*;
import aQute.bnd.service.diff.Diff.Ignore;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

/**
 * This class maintains
 * 
 */
public class Baseline {

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
		public String				warning="";

	}

	final Differ	differ;
	final Reporter	bnd;

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
	public Set<Info> baseline(Jar newer, Jar older, Instructions packageFilters)
			throws Exception {
		Tree n = differ.tree(newer);
		Parameters nExports = getExports(newer);
		Tree o = differ.tree(older);
		Parameters oExports = getExports(older);
		if ( packageFilters == null)
			packageFilters = new Instructions();
		
		return baseline(n, nExports, o, oExports, packageFilters);
	}

	public Set<Info> baseline(Tree n, Parameters nExports, Tree o,
			Parameters oExports, Instructions packageFilters)
			throws Exception {
		Diff diff = n.diff(o).get("<api>");
		Set<Info> infos = Create.set();

		for (Diff pdiff : diff.getChildren()) {
			if (pdiff.getType() != Type.PACKAGE) // Just packages
				continue;

			if (pdiff.getName().startsWith("java."))
				continue;

			if (!packageFilters.matches(pdiff.getName()))
				continue;

			final Info info = new Info();
			infos.add(info);

			info.packageDiff = pdiff;
			info.packageName = pdiff.getName();
			info.attributes = nExports.get(info.packageName);
			bnd.trace("attrs for %s %s", info.packageName,info.attributes);

			info.newerVersion = getVersion(info.attributes);
			info.olderVersion = getVersion(oExports.get(info.packageName));
			if (pdiff.getDelta() == Delta.UNCHANGED) {
				info.suggestedVersion = info.olderVersion;
				if( !info.newerVersion.equals(info.olderVersion)) {
					info.warning += "No difference but versions are equal";
				}
			} else if (pdiff.getDelta() == Delta.REMOVED) {
				info.suggestedVersion = null;
			} else if (pdiff.getDelta() == Delta.ADDED) {
				info.suggestedVersion = Version.ONE;
			} else {
				// We have an API change
				info.suggestedVersion = bump(pdiff.getDelta(), info.olderVersion, 1, 0);

				if (info.newerVersion.compareTo(info.suggestedVersion) < 0) {
					info.mismatch = true; // our suggested version is smaller
											// than
											// the
											// old version!

					// We can fix some major problems by assuming
					// that an interface is a provider interface
					if (pdiff.getDelta() == Delta.MAJOR) {

						info.providers = Create.set();
						if (info.attributes != null)
							info.providers.addAll(Processor.split(info.attributes
									.get(Constants.PROVIDER_TYPE_DIRECTIVE)));

						// Calculate the new delta assuming we fix all the major
						// interfaces
						// by making them providers
						Delta tryDelta = pdiff.getDelta(new Ignore() {
							public boolean contains(Diff diff) {
								if (diff.getType() == Type.INTERFACE
										&& diff.getDelta() == Delta.MAJOR) {
									info.providers.add(Descriptors.getShortName(diff.getName()));
									return true;
								}
								return false;
							}
						});

						if (tryDelta != Delta.MAJOR) {
							info.suggestedIfProviders = bump(tryDelta, info.olderVersion, 1, 0);
						}
					}
				}
			}
		}
		return infos;
	}

	private Version bump(Delta delta, Version last, int offset, int base) {
		switch (delta) {
		case UNCHANGED:
			return last;
		case MINOR:
			return new Version(last.getMajor(), last.getMinor() + offset, base);
		case MAJOR:
			return new Version(last.getMajor() + 1, base, base);
		case ADDED:
			return last;
		default:
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

		return OSGiHeader.parseHeader(m.getMainAttributes().getValue(Constants.EXPORT_PACKAGE));
	}

}
