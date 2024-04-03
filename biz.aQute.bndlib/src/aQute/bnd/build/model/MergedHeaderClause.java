package aQute.bnd.build.model;

import java.util.Objects;

import aQute.bnd.build.model.clauses.HeaderClause;

public record MergedHeaderClause(String key, HeaderClause header, boolean isLocal) {

	@Override
	public int hashCode() {
		return Objects.hash(header);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergedHeaderClause other = (MergedHeaderClause) obj;
		return Objects.equals(header, other.header);
	}

}
