package _package_;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(name="_pid_")
public class _stem_ {

	@Activate
	void activate(Map<String, Object> props) {
		System.out.println(props.get("name"));
	}

	@Deactivate
	void deactivate(Map<String, Object> props) {
		System.out.println(props.get("name"));
	}
}
