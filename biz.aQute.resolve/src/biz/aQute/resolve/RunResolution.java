package biz.aQute.resolve;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.help.Syntax;
import aQute.bnd.help.instructions.ResolutionInstructions;
import aQute.bnd.help.instructions.ResolutionInstructions.RunStartLevel;
import aQute.bnd.help.instructions.ResolutionInstructions.Runorder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.dot.DOT;
import aQute.lib.exceptions.Exceptions;
import aQute.libg.tarjan.Tarjan;

/**
 * Provides a simple way to resolve a project or bndrun file with support for
 * the {@link BndEditModel}. Static methods are used to resolve and then this
 * class holds the different aspects of the result. Either with an exception or
 * the required and optional wiring.
 * <p>
 * The resolution also then provides convenience methods to link it to the
 * projects
 */
public class RunResolution {
	public final Project					project;
	public final Processor					properties;
	public final Map<Resource, List<Wire>>	required;
	public final Map<Resource, List<Wire>>	optional;
	public final Exception					exception;
	public final String						log;
	public final RunStartLevel				runstartlevel;

	/**
	 * The main workhorse to resolve
	 * 
	 * @param project used for reporting errors
	 * @param actualProperties the actual properties used for resolving. This
	 *            can be the project in builders that do not use the
	 *            {@link BndEditModel}, otherwise it is generally the
	 *            {@link BndEditModel}.
	 * @param callbacks any callbacks
	 * @return a Resolution
	 */
	public static RunResolution resolve(Project project, Processor actualProperties,
		Collection<ResolutionCallback> callbacks) {
		if (callbacks == null)
			callbacks = Collections.emptyList();

		try (ResolverLogger logger = new ResolverLogger()) {
			try {
				ResolveProcess resolve = new ResolveProcess();
				Resolver resolver = new BndResolver(logger);
				resolve.resolveRequired(actualProperties, project, project, resolver, callbacks, logger);
				return new RunResolution(project, actualProperties, resolve.getRequiredWiring(),
					resolve.getOptionalWiring(), logger.getLog());
			} catch (Exception e) {
				return new RunResolution(project, actualProperties, e, logger.getLog());
			}
		} finally {
			// System.out.println(" Project : " + project + " " +
			// project.getBase());
			// System.out.println(" Workspace : " + project.getWorkspace() + " "
			// + project.getWorkspace()
			// .getBase());
			// System.out.println(" Properties: " +
			// actualProperties.getFlattenedProperties());
			// System.out.println(" Repos: " + project.getWorkspace()
			// .getPlugins(Repository.class));

		}
	}

	/**
	 * The secondary workhorse to resolve. This can be used if there is no
	 * interactive editing. In that case the properties to resolve are in the
	 * project and we can use the project both for the domain and the
	 * properties.
	 * 
	 * @param project used for reporting errors and the properties for the
	 *            resolve operations
	 * @param callbacks any callbacks
	 * @return a Resolution
	 */
	public static RunResolution resolve(Project project, Collection<ResolutionCallback> callbacks) {
		return resolve(project, project, callbacks);
	}

	RunResolution(Project project, Processor properties, Map<Resource, List<Wire>> required,
		Map<Resource, List<Wire>> optional, String log) {
		this.project = project;
		this.properties = properties;
		this.required = required;
		this.optional = optional;
		this.log = log;
		this.exception = null;
		this.runstartlevel = getConfig(properties);
	}

	RunResolution(Project project, Processor properties, Exception e, String log) {
		this.project = project;
		this.properties = properties;
		this.exception = e;
		this.log = log;
		this.required = null;
		this.optional = null;
		this.runstartlevel = getConfig(properties);
	}

	/**
	 * Check if the resolution is ok, that is, there was no exception.
	 * 
	 * @return true if there was no exception
	 */
	public boolean isOK() {
		return exception == null;
	}

	/**
	 * Update the {@link BndEditModel} with the calculated set of runbundles.
	 * Use the {@link ResolutionInstructions#runstartlevel(RunStartLevel)}.order
	 * to order the bundles. Do not update it if the list has not changed. In
	 * that case return false (no changes)
	 * <p>
	 * If the {@link ResolutionInstructions#runstartlevel(RunStartLevel)}.order
	 * is {@value Runorder#MERGESORTBYNAMEVERSION} then merge the list with the
	 * previous one.
	 * <p>
	 * return true if the list was changed
	 * 
	 * @param model the edit model to update
	 * @return true if there were changes
	 */
	public boolean updateBundles(BndEditModel model) {
		if (exception != null)
			throw Exceptions.duck(exception);

		List<VersionedClause> newer = new ArrayList<>(nonNull(getRunBundles()));
		List<VersionedClause> older = new ArrayList<>(nonNull(model.getRunBundles()));

		if (newer.equals(older))
			return false;

		//
		// For backward compatibility reason
		// we merge the old case
		//
		if (runstartlevel.order() == Runorder.MERGESORTBYNAMEVERSION) {
			older.retainAll(newer);
			newer.removeAll(older);
			older.addAll(newer);
			newer = older;
		}

		model.setRunBundles(newer);

		return true;
	}

	/**
	 * Sort the resources based on their dependencies. Least dependent bundles
	 * are first. This method uses a toplogical sort. If there are cycles then
	 * clearly the topological sort is not perfect.
	 * 
	 * @param resolution the required wiring
	 * @return a topologically sorted list of resources
	 */
	public List<Resource> sortByDependencies(Map<Resource, List<Wire>> resolution) {
		Map<Resource, List<Resource>> dependencies = getGraph(resolution);

		Collection<? extends Collection<Resource>> topologicalSort = Tarjan.tarjan(dependencies);
		return topologicalSort.stream()
			.flatMap(Collection::stream)
			.filter(r -> !ResourceUtils.isInitialRequirement(r))
			.collect(Collectors.toList());
	}

	/**
	 * Turn the wiring into a a->b map.
	 * 
	 * @param resolution the wiring
	 * @return a map where the vertices are connected to other vertices.
	 */
	public Map<Resource, List<Resource>> getGraph(Map<Resource, List<Wire>> resolution) {
		Map<Resource, List<Resource>> dependencies = new HashMap<>();
		for (Resource r : resolution.keySet()) {
			dependencies.put(r, new ArrayList<>());
		}
		resolution.values()
			.stream()
			.flatMap(Collection::stream)
			.forEach(wire -> {
				List<Resource> list = dependencies.computeIfAbsent(wire.getRequirer(), k -> new ArrayList<>());
				list.add(wire.getProvider());
			});
		return dependencies;
	}

	/**
	 * Get a list of ordered run bundles.
	 * 
	 * @return a list of ordered bundles
	 */
	public List<VersionedClause> getRunBundles() {
		List<Resource> orderedResources = getOrderedResources();
		List<VersionedClause> versionedClauses = ResourceUtils.toVersionedClauses(orderedResources);
		int begin = runstartlevel.begin();
		if (begin > 0) {
			// We allow 0 to set all to 1 level
			int step = Math.max(0, runstartlevel.step());

			int n = begin;
			for (VersionedClause vc : versionedClauses) {
				vc.getAttribs()
					.put(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, Integer.toString(n));
				n += step;
			}
		}
		return versionedClauses;
	}

	/*
	 * Order the resources by Runorder
	 */
	public List<Resource> getOrderedResources(Map<Resource, List<Wire>> resolution, Runorder runorder) {
		if (resolution == null)
			return Collections.emptyList();
		switch (runorder) {
			case LEASTDEPENDENCIESFIRST :
				return sortByDependencies(resolution);

			case LEASTDEPENDENCIESLAST :
				List<Resource> sortByDependencies = sortByDependencies(resolution);
				Collections.reverse(sortByDependencies);
				return sortByDependencies;

			case RANDOM :
				List<Resource> list = new ArrayList<>(ResourceUtils.sortByNameVersion(resolution.keySet()));
				Collections.shuffle(list);
				return list;

			case MERGESORTBYNAMEVERSION :
			case SORTBYNAMEVERSION :
			default :
				return ResourceUtils.sortByNameVersion(resolution.keySet());
		}
	}

	public List<Resource> getOrderedResources() {
		return getOrderedResources(required, runstartlevel.order());
	}

	public String dot(String name) {
		DOT<Resource> dot = new DOT<>(name, getGraph(required));
		dot.prune();
		int n = 0;
		Collection<Resource> sortByDependencies = getOrderedResources();

		for (Resource r : sortByDependencies) {
			IdentityCapability ic = ResourceUtils.getIdentityCapability(r);
			dot.name(r, ic.osgi_identity() + "[" + n + "]");
			n++;
		}

		return dot.render();
	}

	/**
	 * Get a list of runbundles as containers
	 */

	public List<Container> getContainers() throws Exception {

		List<Container> containers = new ArrayList<>();
		for (Resource r : required.keySet()) {

			Container bundle = project.getBundle(r);
			containers.add(bundle);
		}
		return containers;
	}

	private <L, T> Collection<T> nonNull(Collection<T> rl) {
		if (rl != null)
			return rl;

		return Collections.emptyList();
	}

	private RunStartLevel getConfig(Processor properties) {
		ResolutionInstructions instructions = Syntax.getInstructions(properties, ResolutionInstructions.class);
		return instructions.runstartlevel(new RunStartLevel() {

			@Override
			public int step() {
				return -1;
			}

			@Override
			public Runorder order() {
				return Runorder.MERGESORTBYNAMEVERSION;
			}

			@Override
			public int begin() {
				return -1;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}
		});
	}

}
