package test.versionpolicy.api;

import aQute.bnd.annotation.ConsumerType;

@ConsumerType
public interface EventHandler {
	void listen(Object o);
}
