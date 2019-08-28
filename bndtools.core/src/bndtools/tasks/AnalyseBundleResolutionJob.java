package bndtools.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.resource.Capability;

import aQute.lib.io.IO;
import bndtools.model.resolution.RequirementWrapper;

public class AnalyseBundleResolutionJob extends Job {

	private static final ILogger					logger	= Logger.getLogger(AnalyseBundleResolutionJob.class);

	private final Set<? extends CapReqLoader>		loaders;

	private Map<String, List<RequirementWrapper>>	requirements;
	private Map<String, List<Capability>>			capabilities;

	public AnalyseBundleResolutionJob(String name, Set<? extends CapReqLoader> loaders) {
		super(name);
		this.loaders = loaders;
	}

	private static <K, V> void mergeMaps(Map<K, List<V>> from, Map<K, List<V>> into) {
		for (Entry<K, List<V>> entry : from.entrySet()) {
			K key = entry.getKey();

			List<V> list = into.get(key);
			if (list == null) {
				list = new ArrayList<>();
				into.put(key, list);
			}

			list.addAll(entry.getValue());
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			// Load all the capabilities and requirements
			Map<String, List<Capability>> allCaps = new HashMap<>();
			Map<String, List<RequirementWrapper>> allReqs = new HashMap<>();
			for (CapReqLoader loader : loaders) {
				try {
					Map<String, List<Capability>> caps = loader.loadCapabilities();
					mergeMaps(caps, allCaps);

					Map<String, List<RequirementWrapper>> reqs = loader.loadRequirements();
					mergeMaps(reqs, allReqs);
				} catch (Exception e) {
					logger.logError("Error in Bnd resolution analysis.", e);
				} finally {
					IO.close(loader);
				}
			}

			// Check for resolved requirements
			for (String namespace : allReqs.keySet()) {
				List<RequirementWrapper> rws = allReqs.get(namespace);
				List<Capability> candidates = allCaps.get(namespace);

				if (candidates == null)
					continue;

				for (RequirementWrapper rw : rws) {
					String filterStr = rw.requirement.getDirectives()
						.get("filter");
					if (filterStr != null) {
						aQute.lib.filter.Filter filter = new aQute.lib.filter.Filter(filterStr);
						for (Capability cand : candidates) {
							if (filter.matchMap(cand.getAttributes())) {
								rw.resolved = true;
								break;
							}
						}
					}
				}
			}

			// Generate the final results
			// Set<File> resultFiles = builderMap.keySet();
			// resultFileArray = resultFiles.toArray(new File[0]);

			this.requirements = allReqs;
			this.capabilities = allCaps;

			// showResults(resultFileArray, importResults, exportResults);
			return Status.OK_STATUS;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, List<RequirementWrapper>> getRequirements() {
		return Collections.unmodifiableMap(requirements);
	}

	public Map<String, List<Capability>> getCapabilities() {
		return Collections.unmodifiableMap(capabilities);
	}
}
