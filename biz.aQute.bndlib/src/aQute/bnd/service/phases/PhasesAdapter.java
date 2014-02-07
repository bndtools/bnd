package aQute.bnd.service.phases;

import aQute.bnd.build.*;
import aQute.service.reporter.*;

public abstract class PhasesAdapter {
	final Phases	master;
	final Phases	next;
	final Reporter	reporter;

	public PhasesAdapter(Reporter reporter, Phases master, Phases next) {
		this.reporter = reporter;
		this.master = master;
		this.next = next;

	}
	
	
	public void begin(Workspace p) {
		next.begin(p);
	}

	public void end(Workspace p) {
		next.end(p);
	}

	public void before(Project p, String phase) {
		next.before(p, phase);
	}

	public void after(Project p, String phase, Exception e) {
		next.after(p, phase, e);
	}

	public void compile(Project p, boolean test) throws Exception {
		next.compile(p, test);
	}

	public void build(Project p, boolean test) throws Exception {
		next.build(p, test);
	}

	public void test(Project p) throws Exception {
		next.test(p);
	}

	public void junit(Project p) throws Exception {
		next.junit(p);
	}

	public void release(Project p) throws Exception {
		next.release(p);
	}

	public void valid(Project p) throws Exception {
		next.valid(p);
	}

	public void action(Project p, String action) throws Exception {
		next.action(p, action);
	}

}
