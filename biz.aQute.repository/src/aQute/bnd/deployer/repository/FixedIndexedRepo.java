package aQute.bnd.deployer.repository;

import aQute.bnd.build.Workspace;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.Registry;
import aQute.service.reporter.Reporter.SetLocation;

/*
 * Used for backward compatibility
 */
@Deprecated
public class FixedIndexedRepo extends OSGiRepository {

	@Override
	public void setRegistry(Registry registry) {
		super.setRegistry(registry);
		Workspace workspace = registry.getPlugin(Workspace.class);
		SetLocation location = workspace
			.warning("FixedIndexedRepository is deprecated, please use " + OSGiRepository.class.getName());
		location.header("-plugin.*")
			.context("FixedIndexedRepository");
	}
}
