package biz.aQute.resolve;

import java.io.File;

import org.osgi.resource.Resource;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;

public class DistroRepository extends ResourcesRepository {

	public DistroRepository(File distro) throws Exception {
		Domain manifest = Domain.domain(distro);
		ResourceBuilder rb = new ResourceBuilder();
		rb.addManifest(manifest);

		Resource resource = rb.build();
		add(resource);
	}
}
