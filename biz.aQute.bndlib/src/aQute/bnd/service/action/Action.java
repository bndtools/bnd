package aQute.bnd.service.action;

import aQute.bnd.build.*;

public interface Action {
	void execute(Project project, String action) throws Exception;
	void execute(Project project, Object ... args) throws Exception;
}
