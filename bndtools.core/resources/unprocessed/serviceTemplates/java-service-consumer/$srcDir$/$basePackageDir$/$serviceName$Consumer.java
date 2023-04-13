package $basePackageName$;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;

import $api_package$.$serviceName$Service;

@Component(immediate=true)
public class $serviceName$Consumer {
	
	final $serviceName$Service service;
	
	@Activate
	public $serviceName$Consumer(@Reference $serviceName$Service service) {
		this.service = service;
		// test service method
		System.out.println("$serviceName$Consumer calling $serviceName$Service");
		System.out.println("$serviceName$Service response is "+service.methodName("$serviceName$Consumer"));
	}

}