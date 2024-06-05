/*
 * Copyright (c) 2021,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.bndtools.elph.bnd;

import static java.util.Comparator.comparing;
import static java.util.Spliterator.ORDERED;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.bndtools.elph.bnd.ProjectPaths.asNames;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.bndtools.elph.util.IO;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class BndCatalog {
    private static final String SAVE_FILE = "deps.save";
    public static final String SAVE_FILE_DESC = "dependency save file";

    private static<T> SimpleDirectedGraph<T, DefaultEdge> newGraph() {
        return new SimpleDirectedGraph<>(DefaultEdge.class);
    }

    final Path root;
    final IO io;
    final Path saveFile;
    final SimpleDirectedGraph<BndProject, DefaultEdge> digraph = newGraph();
    final Map<String, BndProject> nameIndex = new TreeMap<>();
    final MultiValuedMap<Path, BndProject> pathIndex = new HashSetValuedHashMap<>();
    final CountDownLatch analysisCompletion = new CountDownLatch(1);

    public static BndCatalog create(Path bndWorkspace, IO io, Path repoSettingsDir) throws IOException {
    	BndCatalog catalog = new BndCatalog(bndWorkspace, io, repoSettingsDir);
    	// Asynchronously re-load saved dependencies or analyze using bnd.
    	// The catalog will be usable for simple queries, 
    	// but will wait for analysis to complete when exploring dependencies.
    	Job.create("Analyze Liberty project dependencies", monitor -> catalog.loadDeps() ? OK_STATUS : catalog.analyze(monitor)).schedule();
    	return catalog;
    }
    
    private BndCatalog(Path bndWorkspace, IO io, Path repoSettingsDir) throws IOException {
        this.io = io;
        this.root = bndWorkspace;
        this.saveFile = repoSettingsDir.resolve(SAVE_FILE);
        // add the vertices
        try (var files = Files.list(bndWorkspace)) {
            files
                    .filter(Files::isDirectory)                      // for every subdirectory
                    .filter(p -> Files.exists(p.resolve("bnd.bnd"))) // that has a bnd file
                    .map(BndProject::new)                            // create a Project object
                    .forEach(digraph::addVertex);                    // add it as a vertex to the graph
        }

        // index projects by name
        digraph.vertexSet().stream()
                .peek(p -> nameIndex.put(p.name, p))
                .peek(p -> pathIndex.put(p.root.getFileName(), p))
                .filter(BndProject::symbolicNameDiffersFromName)
                .forEach(p -> nameIndex.put(p.symbolicName, p));


        // index projects by name and by symbolic name as paths
        // (even if those paths don't exist)
        // to allow globbing searches on them
        nameIndex.forEach((name, project) -> pathIndex.put(Paths.get(name), project));

        // add the edges
        digraph.vertexSet().forEach(p -> p.initialDeps.stream()
                .map(nameIndex::get)
                .filter(Objects::nonNull)
                .filter(not(p::equals))
                .forEach(q -> digraph.addEdge(p, q)));

        // make everything depend on 'cnf'
        var cnf = nameIndex.get("cnf");
        digraph.vertexSet().stream().filter(not(cnf::equals)).forEach(p -> digraph.addEdge(p, cnf));

        // make some bundles depend on build.image
        var buildImage = nameIndex.get("build.image");
        digraph.vertexSet().stream()
                .filter(not(p -> p.isNoBundle))
                .filter(not(p -> p.publishWlpJarDisabled))
                .forEach(p -> digraph.addEdge(p, buildImage));
    }

    private IStatus analyze(IProgressMonitor pm) {
        Set<BndProject> bndProjects = digraph.vertexSet();
        var bnd = new BndWorkspace(io, root, nameIndex::get);
        SubMonitor subMonitor = SubMonitor.convert(pm, bndProjects.size());
        for(BndProject p: bndProjects) {
			subMonitor.setTaskName("Analyzing " + p.name);
			subMonitor.split(1);
        	bnd.getBuildAndTestDependencies(p)
        			.filter(not(p::equals))
        			.forEach(q -> digraph.addEdge(p,q));
        }
        var text = digraph.edgeSet()
                .stream()
                .map(this::formatEdge)
                .collect(joining("\n", "", "\n"));
        io.writeFile(SAVE_FILE_DESC, saveFile, text);
        analysisCompletion.countDown();
        return OK_STATUS;
    }
    
    private String formatEdge(DefaultEdge e) {
        return "%s -> %s".formatted(digraph.getEdgeSource(e), digraph.getEdgeTarget(e));
    }

    private boolean loadDeps() {
        if (!Files.exists(saveFile)) return false;
        FileTime saveTime = IO.getLastModified(saveFile);
        Predicate<BndProject> isNewer = p -> saveTime.compareTo(p.timestamp) < 0;
        var newerCount = nameIndex.values()
                .stream()
                .filter(isNewer)
                .peek(p -> io.debugf("bnd file for %s is newer than save file %s", p, saveFile))
                .count();
        io.logf("%d projects have bnd files newer than %s", newerCount, saveFile);
        if (newerCount > 0) return false;
        io.readFile(SAVE_FILE_DESC, saveFile, this::loadDep);
        analysisCompletion.countDown();
        return true;
    }

    private void loadDep(String dep) {
        String[] parts = dep.split(" -> ");
        if (parts.length != 2) {
            io.warn("Failed to parse dependency", dep);
            return;
        }
        BndProject source = nameIndex.get(parts[0]);
        BndProject target = nameIndex.get(parts[1]);
        if (null == source || null == target) {
            io.logf("Could not add saved dependency: %s -> %s\tsource project=%s\ttarget project=%s", parts[0], parts[1], source, target);
        } else {
            digraph.addEdge(source, target);
        }
    }
    
    private void waitForAnalysis() {
		try {
			analysisCompletion.await();
		} catch(InterruptedException e) {
			throw new Error(e);
		}		
	}

    public Stream<Path> findProjects(String pattern) {
        @SuppressWarnings("resource")
		var set = pathIndex.keySet().stream()
                // Use Java's globbing support to match paths
                .filter(FileSystems.getDefault().getPathMatcher("glob:" + pattern)::matches)
                // find all the projects indexed by each matching path
                .map(pathIndex::get)
                // create a single stream from all the found collections
                .flatMap(Collection::stream)
                .map(p -> p.root)
                // put the results into a set to eliminate duplicates
                .collect(toUnmodifiableSet());
        if (set.isEmpty()) io.warn("No project found matching pattern \"" + pattern + '"');
        return set.stream();
    }

    public Stream<Path> findProjects(Stream<String> patterns) {
        return patterns.flatMap(this::findProjects);
    }

    private BndProject find(String name) {
        BndProject result = nameIndex.get(name);
        if (null == result) throw new Error("No project found with name \"" + name + '"');
        return result;
    }

    public Set<Path> getLeavesOfSubset(Collection<Path> subset, int max) {
        waitForAnalysis();
        assert max > 0;
        var nodes = asNames(subset).map(this::find).collect(toUnmodifiableSet());
        var subGraph = new AsSubgraph<>(digraph, nodes);
        var leaves = subGraph.vertexSet().stream()
                .filter(p -> subGraph.outgoingEdgesOf(p).size() == 0)
                .map(p -> p.root)
                .sorted()
                .limit(max)
                .collect(toCollection(TreeSet::new));
        io.debugf("getLeafProjects() found %d leaf projects", leaves.size());
        return leaves;
    }

	public Stream<Path> getRequiredProjectPaths(Collection<String> projectNames) {
        waitForAnalysis();
        var deps = getProjectAndDependencySubgraph(projectNames);
        var rDeps = new EdgeReversedGraph<>(deps);
        var topo = new TopologicalOrderIterator<>(rDeps, comparing(p -> p.name));
        return stream(topo).map(p -> p.root);
    }

    public Stream<Path> inTopologicalOrder(Stream<Path> paths) {
        waitForAnalysis();
        var projects = paths.map(ProjectPaths::toName).map(nameIndex::get).collect(toSet());
        var subGraph = new EdgeReversedGraph<>(new AsSubgraph<>(digraph, projects));
        var topo = new TopologicalOrderIterator<>(subGraph, comparing(p -> p.name));
        return stream(topo).map(p -> p.root);
    }

    public Stream<Path> getDependentProjectPaths(Collection<String> projectNames) {
        waitForAnalysis();
        return projectNames.stream()
                .map(this::find)
                .map(digraph::incomingEdgesOf)
                .flatMap(Set::stream)
                .map(digraph::getEdgeSource)
                .map(p -> p.root)
                .distinct();
    }

    Graph<BndProject, ?> getProjectAndDependencySubgraph(Collection<String> projectNames) {
        waitForAnalysis();
        // collect the named projects to start with
        var projects = projectNames.stream()
                .map(this::find)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableSet());
        var results = new HashSet<BndProject>();
        // collect all known dependencies, breadth-first
        while (!projects.isEmpty()) {
            results.addAll(projects);
            projects = projects.stream()
                    .map(digraph::outgoingEdgesOf)
                    .flatMap(Set::stream)
                    .map(digraph::getEdgeTarget)
                    .filter(not(results::contains))
                    .collect(toUnmodifiableSet());
        }
        return new AsSubgraph<>(digraph, results);
    }

    private static <T> Stream<T> stream(Iterator<T> iterator) {
        var spl = Spliterators.spliteratorUnknownSize(iterator, ORDERED);
        return StreamSupport.stream(spl, false);
    }

    public String getProjectDetails(Path path) {
        String name = path.getFileName().toString();
        BndProject project = nameIndex.get(name);
        return project.details();
    }
}
