package aQute.bnd.osgi.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.repository.Repository;

public class XMLResourceGenerator {

	private Repository	repository;
	private String		name;
	private List<URI>	references	= new ArrayList<>();

	public XMLResourceGenerator() {

	}

	public XMLResourceGenerator(Repository repository) {
		this.repository = repository;
	}

	public void save(File location) throws IOException {
		Path tmp = Files.createTempFile(location.toPath().getParent(), "index", ".xml");
		try (FileOutputStream out = new FileOutputStream(tmp.toFile())) {
			save(out);
		}
		Files.move(tmp, location.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	public void save(FileOutputStream out) {

	}

	public static void save(Repository repository, String name, File out) throws Exception {

	}

	public void setName(String name) {
		this.name = name;
	}

	public void addReference(URI reference) {
		this.references.add(reference);
	}

	public void setRepository(Repository repository2) {
		// TODO Auto-generated method stub

	}
}
