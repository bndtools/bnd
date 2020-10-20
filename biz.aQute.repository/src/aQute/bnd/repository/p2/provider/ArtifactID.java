package aQute.bnd.repository.p2.provider;

import java.util.Objects;

import aQute.bnd.version.Version;

class ArtifactID {
	private final String	id;
	private final Version	version;
	private final String	md5;

	public ArtifactID(String id, Version version, String md5) {
		super();
		this.id = id;
		this.version = version;
		this.md5 = md5;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((md5 == null) ? 0 : md5.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtifactID other = (ArtifactID) obj;
		return Objects.equals(id, other.id) && Objects.equals(md5, other.md5) && Objects.equals(version, other.version);
	}
}
