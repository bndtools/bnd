package aQute.remote.agent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import aQute.remote.api.Supervisor;
import aQute.remote.api.XEventDTO;

public class OSGiEventHandler implements EventHandler {

	private Supervisor supervisor;

	public OSGiEventHandler(Supervisor supervisor) {
		this.supervisor = supervisor;
	}

	@Override
	public void handleEvent(Event event) {
		final XEventDTO dto = new XEventDTO();

		dto.received = System.currentTimeMillis();
		dto.properties = initProperties(event);
		dto.topic = event.getTopic();

		supervisor.onOSGiEvent(dto);
	}

	private Map<String, String> initProperties(final Event event) {
		final Map<String, String> properties = new HashMap<>();

		for (final String propertyName : event.getPropertyNames()) {
			Object propertyValue = event.getProperty(propertyName);
			if (propertyValue instanceof String[]) {
				propertyValue = Arrays.asList((String[]) propertyValue);
			}
			properties.put(propertyName, propertyValue.toString());
		}
		return properties;
	}

}
