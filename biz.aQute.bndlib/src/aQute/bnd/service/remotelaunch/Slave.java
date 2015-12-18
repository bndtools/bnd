package aQute.bnd.service.remotelaunch;

import java.util.List;

public interface Slave {
	void sync(String localId, byte[] sha1) throws Exception;

	void update(String localId, byte[] contents) throws Exception;

	void close() throws Exception;

	void launch(List<String> launch) throws Exception;

	String prefix() throws Exception;
}
