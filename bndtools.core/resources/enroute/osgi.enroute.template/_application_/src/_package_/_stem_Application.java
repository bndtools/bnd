package _package_;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.debug.api.Debug;

@Component(service=_stem_Application.class, property = { Debug.COMMAND_SCOPE + "=_cmd_",
	Debug.COMMAND_FUNCTION + "=_cmd_" },name="_pid_")
public class _stem_Application {

	
	/*
	 * Gogo command
	 */
	public String _cmd_(String m) throws Exception {
		return m;
	}
}
