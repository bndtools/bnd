package test.versionpolicy.api;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface EventHandler {
	void listen(Object o);
}
