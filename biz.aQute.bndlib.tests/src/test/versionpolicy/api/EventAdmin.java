package test.versionpolicy.api;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface EventAdmin {
	void post(Object o);
}
