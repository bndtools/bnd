package test.versionpolicy.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface EventAdmin {
	void post(Object o);
}
