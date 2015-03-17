package aQute.remote.api;

public interface Supervisor {

	
	void event( Event e) throws Exception;
	
	boolean stdout(String out) throws Exception;
	boolean stderr(String out) throws Exception;
	
	byte[] getFile(String sha) throws Exception;
}
