package aQute.bnd.service.phases;

import aQute.bnd.build.*;

public interface Phases {
	void begin(Workspace p);

	void end(Workspace p);

	void before(Project p, String phase);

	void after(Project p, String phase, Exception e);

	void compile(Project p, boolean test) throws Exception;

	void build(Project p, boolean test) throws Exception;

	void test(Project p) throws Exception;

	void junit(Project p) throws Exception;

	void release(Project p) throws Exception;

	void valid(Project p) throws Exception;

	void pack(Run r) throws Exception;

	void action(Project p, String action) throws Exception;
}
