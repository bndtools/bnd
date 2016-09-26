package aQute.jpm.lib;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DependencyCollector {
	private final static Logger				logger	= LoggerFactory.getLogger(DependencyCollector.class);
	private final List<ArtifactData>		list	= new ArrayList<ArtifactData>();
	private final JustAnotherPackageManager	jpm;
	private boolean							synced;

	public DependencyCollector(JustAnotherPackageManager jpm) {
		this.jpm = jpm;
	}

	/**
	 * Add a revision to this collector and start the download
	 * 
	 * @throws Exception
	 */
	public void add(String coordinate, String name) throws Exception {
		logger.debug("add {} = {}", coordinate, name);
		ArtifactData candidate = jpm.getCandidateAsync(coordinate);
		if (candidate == null) {
			jpm.reporter.error("Cannot find %s", coordinate);
			return;
		}
		list.add(candidate);
	}

	void sync() throws InterruptedException {
		if (!synced) {
			for (ArtifactData artifact : list) {
				artifact.sync();
				if (artifact.error != null)
					jpm.reporter.error("Download error %s for %s", artifact.error, artifact.url);
				File file = new File(artifact.file);
				if (!file.isFile())
					jpm.reporter.error("No file found for %s", artifact.url);
			}
			synced = true;
		}
	}

	public Collection< ? extends String> getPaths() throws InterruptedException {
		sync();
		List<String> paths = new ArrayList<String>();
		for (ArtifactData artifact : list) {
			if (artifact.error == null) {
				paths.add(artifact.file);
			}
		}

		return paths;
	}

	public List<byte[]> getDigests() throws InterruptedException {
		sync();
		List<byte[]> digests = new ArrayList<byte[]>();
		for (ArtifactData artifact : list) {
			if (artifact.error == null) {
				digests.add(artifact.sha);
			}
		}
		return digests;
	}

	public void add(ArtifactData artifact) {
		list.add(artifact);

	}
}
