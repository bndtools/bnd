package aQute.bnd.repository.maven.pom.provider;

import java.io.File;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.maven.provider.MavenRepository;

public abstract class InnerRepository extends ResourcesRepository {

	private final MavenRepository	mavenRepository;
	private final File				location;

	public InnerRepository(MavenRepository mavenRepository, File location) {
		this.mavenRepository = mavenRepository;
		this.location = location;
	}

	public File getLocation() {
		return location;
	}

	public MavenRepository getMavenRepository() {
		return mavenRepository;
	}

	abstract void refresh() throws Exception;

}
