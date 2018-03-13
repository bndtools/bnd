package aQute.bnd.service.repository;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.util.dto.DTO;

@ProviderType
public interface ResolverInfo {

	enum State {
		Pending,
		Missing,
		Unresolveable,
		Resolveable;
	}

	class ResolveStatus extends DTO {
		public State	state;
		public String	message;
	}

	ResolveStatus getResolveStatus(byte[] sha) throws Exception;

	ResolveStatus getResolveStatus(String filterExpr) throws Exception;

}
