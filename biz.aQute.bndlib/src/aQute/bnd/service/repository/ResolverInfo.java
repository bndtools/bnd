package aQute.bnd.service.repository;

import aQute.bnd.annotation.*;
import aQute.bnd.util.dto.*;

@ProviderType
public interface ResolverInfo {

	enum State {
		Pending, Missing, Unresolveable, Resolveable;
	}

	class ResolveStatus extends DTO {
		public State	state;
		public String	message;
	}

	ResolveStatus getResolveStatus(byte[] sha) throws Exception;

	ResolveStatus getResolveStatus(String filterExpr) throws Exception;

}
