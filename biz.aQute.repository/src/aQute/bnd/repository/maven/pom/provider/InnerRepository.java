package aQute.bnd.repository.maven.pom.provider;

import java.io.File;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.maven.provider.MavenRepository;

abstract class InnerRepository extends ResourcesRepository {

	private final MavenRepository	mavenRepository;
	private final File				location;

	InnerRepository(MavenRepository mavenRepository, File location) {
		super();
		this.mavenRepository = mavenRepository;
		this.location = location;
	}

	File getLocation() {
		return location;
	}

	MavenRepository getMavenRepository() {
		return mavenRepository;
	}

	abstract void refresh() throws Exception;

	abstract boolean isStale() throws Exception;
}
