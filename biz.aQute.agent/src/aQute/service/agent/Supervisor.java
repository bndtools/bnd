package aQute.service.agent;

public interface Supervisor {

	
	void event( Event e) throws Exception;
	
	void stdout(String out) throws Exception;
	void stderr(String out) throws Exception;
	
	byte[] getFile(String sha) throws Exception;
}
