package aQute.bnd.service.remotelaunch;

public interface Master {
	byte[] getData(byte[] sha1);

	void bye();
}
