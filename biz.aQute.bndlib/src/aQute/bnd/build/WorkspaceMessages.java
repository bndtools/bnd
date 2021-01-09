package aQute.bnd.build;

import aQute.libg.reporter.Message;
import aQute.service.reporter.Messages;

public interface WorkspaceMessages extends Messages {

	@Message("Invalid ResourceRepositoryStrategy %s. Must be one of the folllowing: %s")
	ERROR InvalidStrategy(String help, String[] args);

}
