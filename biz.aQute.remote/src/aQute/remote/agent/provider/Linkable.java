package aQute.remote.agent.provider;

import java.io.Closeable;

public interface Linkable<L, R> extends Closeable {
	L get();

	void setRemote(R remote);

}
