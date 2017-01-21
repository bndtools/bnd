package {{basePackageName}}.command;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import {{basePackageName}}.api.ExampleApi;
import osgi.enroute.debug.api.Debug;

/**
 * This is the implementation. It registers the Api interface and calls it
 * through a Gogo command.
 * 
 */
@Component(service=ExampleApiCommand.class, property = { Debug.COMMAND_SCOPE + "=example",
		Debug.COMMAND_FUNCTION + "=example" }, name="{{basePackageName}}.command")
public class ExampleApiCommand {
	private ExampleApi target;

	public void example(String message) {
		target.say(message);
	}

	@Reference
	void setExampleApi(ExampleApi service) {
		this.target = service;
	}

}
