package aQute.bnd.maven.indexer.plugin.test;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class HelloComponent {

	@Activate
	void start() {
		System.out.println("Hello World!");
	}
	
}
