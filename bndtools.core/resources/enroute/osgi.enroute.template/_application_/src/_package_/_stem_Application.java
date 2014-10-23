package _package_;

import org.osgi.dto.DTO;
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

	public static class SignupData extends DTO {
		public String name;
		public long time;
	}

	interface SignupRequest extends RESTRequest {
		SignupData _body();
	}

	public String postSignup(SignupRequest rq) {
		SignupData body = rq._body();
		return "Welcome " + body.name;
	}

	public String _cmd_(String m) throws Exception {
		return m;
	}
}
