package aQute.bnd.build;

import aQute.bnd.service.action.Action;

public class ScriptAction implements Action {
	final String	script;
	final String	type;

	public ScriptAction(String type, String script) {
		this.script = script;
		this.type = type;
	}

	@Override
	public void execute(Project project, String action) throws Exception {
		project.script(type, script);
	}

	@Override
	public void execute(Project project, Object... args) throws Exception {
		project.script(type, script, args);
	}

}
