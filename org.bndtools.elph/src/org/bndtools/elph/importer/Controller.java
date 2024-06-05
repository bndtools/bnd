package org.bndtools.elph.importer;

import static java.util.stream.Collectors.toCollection;
import static org.bndtools.elph.importer.EclipseWorkspace.listProjects;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bndtools.elph.bnd.BndCatalog;
import org.bndtools.elph.util.IO;

class Controller {
	private static final IO io = new IO();
	
	/** The Open Liberty repo location */
	private final Path repo;
	private final BndCatalog bndCatalog; 
	
	Controller(Path repo) {
		this.repo = validateRepo(repo);
		Path bndWorkspaceDir = repo.resolve("dev");
		Path repoSettingsDir = repo.resolve(".elph");
        if (!Files.isDirectory(repoSettingsDir)) {
            io.verifyOrCreateDir("Liberty git repository Elph settings directory", repoSettingsDir);
            // make sure the entire contents of the directory are ignored, including the .gitignore
            io.writeFile(".lct git ignore file", repoSettingsDir.resolve(".gitignore"), "*");
        }
		try {
			this.bndCatalog = BndCatalog.create(bndWorkspaceDir, io, repoSettingsDir);
			// import the cnf project (kicking off more background work in the bndtools plugin)
			EclipseWorkspace.importCnf(bndWorkspaceDir);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOError(e);
		}
	}

	Controller(String repo) {
		this(Paths.get(repo));
	}
	
	Path getRepo() {
		return repo;
	}
	
    static Path getRepoSettingsDir(Path repo) {
        Path dir = repo.resolve(".elph");
        if (!Files.isDirectory(dir)) {
            io.verifyOrCreateDir("LCT git repository settings directory", dir);
            // make sure the entire contents of the directory are ignored, including the .gitignore
            io.writeFile(".lct git ignore file", dir.resolve(".gitignore"), "*");
        }
        return dir;
    }

	List<String> listBndProjects(String pattern) {
		return bndCatalog.findProjects(pattern).map(Controller::toName).collect(toCollection(ArrayList::new));
	}
	
	List<String> listUnimportedProjects(String pattern) {
		List<String> list = listBndProjects(pattern);
		list.removeAll(listProjects());
		return list;
	}

	void findProjectsAndDeps(Collection<String> names, Collection<? super Path> collection, boolean includeUsers) {
		Set<Path> set = bndCatalog.findProjects(names.stream()).collect(Collectors.toSet());
		// add users first to pick up dependencies of users
		if (includeUsers) addUsers(set);
		addDeps(set);
		// remove already seen projects
		set.removeAll(collection);
		// add unique new projects to original collection
		collection.addAll(set);
	}

	private void addDeps(Set<Path> set) {
		bndCatalog.getRequiredProjectPaths(toNames(set)).forEach(set::add);
	}

	private void addUsers(Set<Path> set) {
		bndCatalog.getDependentProjectPaths(toNames(set)).forEach(set::add);
	}
	
	Queue<Path> inDependencyOrder(Collection<Path> projects) {
		return bndCatalog.inTopologicalOrder(projects.stream()).collect(toCollection(LinkedList::new));
	}
	
    private static Path validateRepo(Path olRepo) throws RuntimeException {
        if (!Files.isDirectory(olRepo)) throw new RuntimeException("Open Liberty repository is not a valid directory: " + olRepo);
        else if (!Files.isDirectory(olRepo.resolve(".git"))) throw new RuntimeException("Open Liberty repository does not appear to be a git repository: " + olRepo);
        else if (!Files.isDirectory(olRepo.resolve("dev"))) throw new RuntimeException("Open Liberty repository does not contain an expected 'dev' subdirectory: " + olRepo);
        return olRepo;
	}
    
    public static Set<String> toNames(Collection<Path> projects) { return asNames(projects).collect(toCollection(TreeSet::new)); }

    public static Stream<String> asNames(Collection<Path> projects) { return projects.stream().map(Controller::toName); }
    
    public static String toName(Path project) { return project.getFileName().toString(); }
}