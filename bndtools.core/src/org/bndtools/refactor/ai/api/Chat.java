package org.bndtools.refactor.ai.api;

public interface Chat {

	Reply ask(String question);

	void system(String command);

	void assistant(String command);

	void model(String model);

	void clear();

	void setProlog(String string);

	void clear(String role);
}
