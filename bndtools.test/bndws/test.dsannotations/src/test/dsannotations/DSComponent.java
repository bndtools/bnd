package test.dsannotations;

import java.net.URL;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DSComponent {


	@Activate
	void start() {
		
	}

	@Reference
	void foo(URL url) {}
	
}
