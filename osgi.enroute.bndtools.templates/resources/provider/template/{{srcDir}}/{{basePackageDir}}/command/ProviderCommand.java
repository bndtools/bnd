package {{basePackageName}}.command;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import {{basePackageName}}.api.Provider;
import osgi.enroute.debug.api.Debug;

/**
 * This is the implementation. It registers the Provider interface and calls it
 * through a Gogo command.
 * 
 */
@Component(service=ProviderCommand.class, property = { Debug.COMMAND_SCOPE + "=provider",
		Debug.COMMAND_FUNCTION + "=provider" }, name="{{projectName}}.command")
public class ProviderCommand {
	private Provider target;

	public void provider(String message) {
		target.say(message);
	}

	@Reference
	void setProvider(Provider service) {
		this.target = service;
	}

}
