package _package_.provider;


import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import osgi.enroute.debug.api.Debug;
import _package_.api._stem_;

@Component(property={Debug.COMMAND_SCOPE+"=say", Debug.COMMAND_FUNCTION+"=say"})
public class _stem_Impl implements _stem_ {
	private Logger logger;
	private String name;
	
	@Activate
	void activate(Map<String,Object> map) {
		name = map.containsKey("name") ? (String) map.get("name") : "World";
		say("Hello " + name);
	}

	@Deactivate
	void deactivate(Map<String,Object> map) {
		say("Goodbye " + name);
	}

	@Override
	public void say(String message) {
		logger.info(message);
	}
	
	@Reference
	void setLogger(Logger logger) {
		this.logger = logger;
	}

}
