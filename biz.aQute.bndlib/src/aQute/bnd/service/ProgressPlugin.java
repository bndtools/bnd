package aQute.bnd.service;

public interface ProgressPlugin {

	void startedTask(String taskName, int size);
	
	void worked(int units);

	void done();

}
