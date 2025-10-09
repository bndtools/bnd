package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Manifest;

/**
 * Bnd Resource for Manifest with correct support for writing the manifest to an
 * output stream.
 */
public class ManifestResource extends WriteResource {
	private final Manifest manifest;

	public ManifestResource(Manifest manifest) {
		this.manifest = requireNonNull(manifest);
	}

	public ManifestResource() {
		this(new Manifest());
	}

	public Manifest getManifest() {
		return manifest;
	}

	@Override
	public long lastModified() {
		return 0L;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		Jar.writeManifest(manifest, out);
	}
}
