package aQute.bnd.build.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Domain;

public enum OSGI_CORE {
	R4_0_1,
	R4_2_1,
	R4_3_0,
	R4_3_1,
	R5_0_0,
	R6_0_0,
	R7_0_0;

	private Domain manifest;

	public Domain getManifest() throws IOException {
		if (manifest == null) {
			try (InputStream resource = OSGI_CORE.class.getResourceAsStream("osgi-core/" + name() + ".mf")) {
				Manifest m = new Manifest(resource);
				manifest = Domain.domain(m);
			}
		}
		return manifest;
	}
}
