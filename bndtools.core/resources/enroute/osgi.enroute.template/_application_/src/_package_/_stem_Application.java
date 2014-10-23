package _package_;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;

@AngularWebResource.Require
@BootstrapWebResource.Require
@WebServerExtender.Require
@Component(property = { Debug.COMMAND_SCOPE + "=_cmd_",
	Debug.COMMAND_FUNCTION + "=_cmd_" },name="_pid_")
public class _stem_Application implements REST {

	public String getUpper(RESTRequest rq, String string) {
		return string.toUpperCase();
	}

	public String _cmd_(String m) throws Exception {
		return m;
	}
}
