package aQute.bnd.osgi;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.header.*;
import aQute.bnd.service.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.lib.collections.*;

public class BaselineProcessor extends Analyzer {

	public BaselineProcessor() {}

	public BaselineProcessor(Analyzer child) {
		super(child);
	}

	/**
	 * This method attempts to find the baseline jar for the current project. It
	 * reads the -baseline property and treats it as instructions. These
	 * instructions are matched against the bsns of the jars (think sub
	 * builders!). If they match, the sub builder is selected.
	 * <p>
	 * The instruction can then specify the following options:
	 *
	 * <pre>
	 * 	version : baseline version from repository
	 * 	file    : a file path
	 * </pre>
	 *
	 * If neither is specified, the current version is used to find the highest
	 * version (without qualifier) that is below the current version. If a
	 * version is specified, we take the highest version with the same base
	 * version.
	 * <p>
	 * Since baselining is expensive and easily generates errors you must enable
	 * it. The easiest solution is to {@code -baseline: *}. This will match all
	 * sub builders and will calculate the version.
	 *
	 * @return a Jar or null
	 */
	public Jar getBaselineJar() throws Exception {
		String bl = getProperty(Constants.BASELINE);
		if (bl == null || Constants.NONE.equals(bl))
			return null;

		Instructions baselines = new Instructions(getProperty(Constants.BASELINE));
		if (baselines.isEmpty())
			return null; // no baselining

		RepositoryPlugin repo = getBaselineRepo();
		if (repo == null)
			return null; // errors reported already

		String bsn = getBsn();
		Version version = new Version(getVersion());
		SortedSet<Version> versions = removeStagedAndFilter(repo.versions(bsn), repo, bsn);

		if (versions.isEmpty()) {
			// We have a repo
			Version v = Version.parseVersion(getVersion()).getWithoutQualifier();
			if (v.compareTo(Version.ONE) > 0) {
				warning("There is no baseline for %s in the baseline repo %s. The build is for version %s, which is higher than 1.0.0 which suggests that there should be a prior version.",
						getBsn(), repo, v);
			}
			return null;
		}

		//
		// Loop over the instructions, first match commits.
		//

		for (Entry<Instruction,Attrs> e : baselines.entrySet()) {
			if (e.getKey().matches(bsn)) {
				Attrs attrs = e.getValue();
				Version target;

				if (attrs.containsKey("version")) {

					// Specified version!

					String v = attrs.get("version");
					if (!Verifier.isVersion(v)) {
						error("Not a valid version in %s %s", Constants.BASELINE, v);
						return null;
					}

					Version base = new Version(v);
					SortedSet<Version> later = versions.tailSet(base);
					if (later.isEmpty()) {
						error("For baselineing %s-%s, specified version %s not found", bsn, version, base);
						return null;
					}

					// First element is equal or next to the base we desire

					target = later.first();

					// Now, we could end up with a higher version than our
					// current
					// project

				} else if (attrs.containsKey("file")) {

					// Can be useful to specify a file
					// for example when copying a bundle with a public api

					File f = getFile(attrs.get("file"));
					if (f != null && f.isFile()) {
						Jar jar = new Jar(f);
						addClose(jar);
						return jar;
					}
					error("Specified file for baseline but could not find it %s", f);
					return null;
				} else {
					target = versions.last();
				}

				// Fetch the revision

				if (target.getWithoutQualifier().compareTo(version.getWithoutQualifier()) > 0) {
					error("The baseline version %s is higher than the current version %s for %s in %s", target,
							version, bsn, repo);
					return null;
				}
				if (target.getWithoutQualifier().compareTo(version.getWithoutQualifier()) == 0) {
					if (isPedantic()) {
						warning("Baselining against jar");
					}
				}
				File file = repo.get(bsn, target, attrs);
				if (file == null || !file.isFile()) {
					error("Decided on version %s-%s but cannot get file from repo %s", bsn, version, repo);
					return null;
				}
				Jar jar = new Jar(file);
				addClose(jar);
				return jar;
			}
		}

		// Ignore, nothing matched
		return null;
	}

	/**
	 * Remove any staging versions that have a variant with a higher qualifier.
	 * 
	 * @param versions
	 * @param repo
	 * @return
	 * @throws Exception
	 */
	private SortedSet<Version> removeStagedAndFilter(SortedSet<Version> versions, RepositoryPlugin repo, String bsn)
			throws Exception {
		List<Version> filtered = new ArrayList<Version>(versions);
		Collections.reverse(filtered);

		InfoRepository ir = (repo instanceof InfoRepository) ? (InfoRepository) repo : null;

		//
		// Filter any versions that only differ in qualifier
		// The last variable is the last one added. Since we are
		// sorted from high to low, we skip any earlier base versions
		//
		Version last = null;
		for (Iterator<Version> i = filtered.iterator(); i.hasNext();) {
			Version v = i.next();

			// Check if same base version as last
			Version current = v.getWithoutQualifier();
			if (last != null && current.equals(last)) {
				i.remove();
				continue;
			}

			//
			// Check if this is not a master if the repo
			// has a state for each resource
			// /
			if (ir != null && !isMaster(ir, bsn, v))
				i.remove();

			last = current;
		}
		SortedList<Version> set = new SortedList<Version>(filtered);
		trace("filtered for only latest staged: %s from %s in range ", set, versions);
		return set;
	}

	/**
	 * Check if we have a master phase.
	 * 
	 * @param repo
	 * @param bsn
	 * @param v
	 * @return
	 * @throws Exception
	 */
	private boolean isMaster(InfoRepository repo, String bsn, Version v) throws Exception {
		ResourceDescriptor descriptor = repo.getDescriptor(bsn, v);

		//
		// If not there, we assume that is master
		//

		if (descriptor == null)
			return true;

		return descriptor.phase == Phase.MASTER;
	}

	public RepositoryPlugin getReleaseRepo() {
		String repoName = getProperty(Constants.RELEASEREPO);

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin r : repos) {
			if (r.canWrite()) {
				if (repoName == null || r.getName().equals(repoName)) {
					return r;
				}
			}
		}
		if (repoName == null)
			error("Could not find a writable repo for the release repo (-releaserepo is not set)");
		else
			error("No such -releaserepo %s found", repoName);

		return null;
	}

	public RepositoryPlugin getBaselineRepo() {
		String repoName = getProperty(Constants.BASELINEREPO);
		if (repoName == null)
			return getReleaseRepo();

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin r : repos) {
			if (r.getName().equals(repoName))
				return r;
		}
		error("Could not find -baselinerepo %s", repoName);
		return null;
	}

}
