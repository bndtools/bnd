package _package_;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;

@AngularWebResource(resource={"angular.js","angular-resource.js", "angular-route.js"}, priority=1000)
@BootstrapWebResource(resource="css/bootstrap.css")
@WebServerExtender
@ConfigurerExtender
@Component(name="_pid_")
public class _stem_Application implements REST {

	public String getUpper(RESTRequest rq, String string) {
		return string.toUpperCase();
	}

}
