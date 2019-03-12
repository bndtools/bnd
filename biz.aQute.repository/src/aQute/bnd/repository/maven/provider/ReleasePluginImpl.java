package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.util.promise.Promise;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.IPom;
import aQute.maven.api.Release;

class ReleasePluginImpl {
	List<IPom>			releasedArtifacts	= new CopyOnWriteArrayList<>();
	Project				indexProject;
	IPom				master;
	MavenBndRepository	mvn;

	ReleasePluginImpl(MavenBndRepository mvn, Project project) {
		this.mvn = mvn;
		this.indexProject = project;
	}

	void add(Processor context, IPom pom) {
		if (indexProject == null)
			return;

		releasedArtifacts.add(pom);
		if (context == indexProject) {
			master = pom;
		} else {

		}
	}

	/*
	 * End the release cyle. This will index all artifacts released to the
	 * repository since the begin.
	 */
	void end(Project p, IMavenRepo storage) throws Exception {
		if (releasedArtifacts.isEmpty())
			return;

		if (p != indexProject)
			throw new IllegalArgumentException(
				"Different project that started the release plugin then that ended it " + indexProject + ":" + p);

		if (master == null)
			throw new IllegalArgumentException("The index project was never released so GAV is unknown for index");

		Archive index = master.getRevision()
			.archive("xml", "index");
		String prefix = makeDots(index.remotePath);

		ResourcesRepository repository = createIndex(releasedArtifacts, storage, prefix);
		saveToXml(p, storage, index, repository);
	}

	private void saveToXml(Project p, IMavenRepo storage, Archive index, ResourcesRepository repository)
		throws IOException, Exception {
		XMLResourceGenerator rg = new XMLResourceGenerator();
		File tmpFile = File.createTempFile("index", ".xml");
		try {

			rg.name(master.getRevision()
				.toString());
			rg.repository(repository);
			rg.save(tmpFile);

			try (Release release = storage.release(master.getRevision(), p.getFlattenedProperties());) {
				release.force();
				release.add(index, tmpFile);
			}
		} finally {
			IO.delete(tmpFile);
		}
	}

	private ResourcesRepository createIndex(List<IPom> releasedArtifacts, IMavenRepo storage, String prefix)
		throws Exception {
		ResourcesRepository repo = new ResourcesRepository();

		for (IPom pom : releasedArtifacts) {
			try {
				Promise<File> promise = storage.get(pom.binaryArchive());
				File file = promise.getValue();
				ResourceBuilder rb = new ResourceBuilder();
				String uri = prefix + pom.binaryArchive().remotePath;
				rb.addFile(file, new URI(uri));
				repo.add(rb.build());
			} catch (Exception e) {
				indexProject.exception(e, "Failed to index artifact %s", pom.binaryArchive());
			}
		}

		return repo;
	}

	private String makeDots(String path) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				sb.append("../");
			}
		}
		return sb.toString();
	}

}
