package aQute.bnd.build;

import aQute.bnd.service.phases.*;
import aQute.service.reporter.*;

public class DefaultPhases implements Phases {
	final Reporter	reporter;
	final Phases	master;

	public DefaultPhases(Reporter reporter, Phases master) {
		this.reporter = reporter;
		this.master = master;
	}

	public void compile(Project p, boolean test) throws Exception {
		for ( Project dependsOn : p.getDependson()) {
			dependsOn.build(false);
		}
		p.compile(test);
	}

	public void build(Project p, boolean test) throws Exception {
		compile(p,test);
		p.build(test);
	}

	public void test(Project p) throws Exception {
		p.test();
	}

	public void junit(Project p) throws Exception {
		// TODO
	}

	public void release(Project p) throws Exception {
		p.release();
	}

	public void begin(Workspace p) {
	}

	public void end(Workspace p) {
	}

	public void before(Project p, String phase) {
	}
	public void after(Project p, String phase, Exception e) {
	}

	public void action(Project p, String action) throws Exception {
		p.action(action);
	}

	public void valid(Project p) throws Exception {
	}

	public void pack(Run r) throws Exception {
		// TODO
	}


}
