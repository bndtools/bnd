package aQute.remote.agent.provider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import aQute.libg.shacache.ShaSource;
import aQute.remote.api.Supervisor;

class SupervisorSource implements ShaSource {

	private Supervisor remote;

	public SupervisorSource(Supervisor supervisor) {
		this.remote = supervisor;
	}

	@Override
	public boolean isFast() {
		return false;
	}

	@Override
	public InputStream get(String sha) throws Exception {
		byte[] data = remote.getFile(sha);
		if (data == null)
			return null;

		return new ByteArrayInputStream(data);
	}
}