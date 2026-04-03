package bndtools.tasks;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.io.IO;
import bndtools.model.resolution.RequirementWrapper;

public class AnalyseBundleResolutionJob extends Job {

	private static final ILogger					logger	= Logger.getLogger(AnalyseBundleResolutionJob.class);

	private final Set<? extends CapReqLoader>		loaders;

	private Map<String, List<RequirementWrapper>>	requirements;
	private Map<String, List<Capability>>			capabilities;
	private EE										ee;

	public AnalyseBundleResolutionJob(String name, Set<? extends CapReqLoader> loaders) {
		this(name, loaders, null);
	}

	public AnalyseBundleResolutionJob(String name, Set<? extends CapReqLoader> loaders, EE ee) {
		super(name);
		this.loaders = loaders;
		this.ee = ee;
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
				List<RequirementWrapper> rws = allReqs.getOrDefault(namespace, emptyList());
				List<Capability> candidates = allCaps.getOrDefault(namespace, emptyList());

				List<Capability> javaCandidates = ee == null ? emptyList()
					: ee.getResource()
						.getCapabilities(namespace);

				outer: for (RequirementWrapper rw : rws) {
					String filterDirective = rw.requirement.getDirectives()
						.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					if (filterDirective == null) {
						continue;
					}
					Predicate<Capability> predicate = ResourceUtils.filterMatcher(rw.requirement);
					for (Capability cand : candidates) {
						if (predicate.test(cand)) {
							rw.resolved = true;
							continue outer;
						}
					}
					for (Capability cand : javaCandidates) {
						if (predicate.test(cand)) {
							rw.java = true;
							continue outer;
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
