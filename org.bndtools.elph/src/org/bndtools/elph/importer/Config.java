package org.bndtools.elph.importer;

import static org.eclipse.core.runtime.Platform.getStateLocation;
import static org.eclipse.ui.XMLMemento.createReadRoot;
import static org.osgi.framework.FrameworkUtil.getBundle;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;

class Config {
	private static final String OL_REPO_NODE = "open-liberty-repo";
	private static final Bundle bundle = getBundle(Config.class);
	/** a plugin-specific location in the Eclipse workspace */
	// IPath.toPath() is new in eclipse 3.18 so use IPath.toFile().toPath()
	private static final Path configPath = getStateLocation(bundle).toFile().toPath().resolve("settings.xml"); 
	
	private volatile Path olRepo;

	Optional<Path> getOlRepoPath() {
		if (null != olRepo) return Optional.of(olRepo);
		if (!Files.exists(configPath)) {
			System.out.println("Config file not found: " + configPath);
			return Optional.empty();
		}
		try {
			System.out.println("Reading config file from: " + configPath);
			XMLMemento rootNode = createReadRoot(new FileReader(configPath.toFile()));
			return Optional.ofNullable(rootNode.getChild(OL_REPO_NODE))
					.map(IMemento::getTextData)
					.map(Paths::get);
		} catch (WorkbenchException | FileNotFoundException ignored) {
			return Optional.empty();
		}
	}
	
	void saveOlRepoPath(Path olPath) {
		this.olRepo = olPath;
		// overwrite entire file with just this setting
		XMLMemento memento = XMLMemento.createWriteRoot("root");
		memento.createChild(OL_REPO_NODE).putTextData(olPath.toString());
		try (Writer writer = new FileWriter(configPath.toFile()))
		{
			memento.save(writer);
			System.out.println("Saved config to " + configPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}