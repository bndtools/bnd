package $basePackageName$;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import $api_package$.$serviceName$Service;

@Component(immediate=true)
public class $serviceName$Consumer {
	
	private $serviceName$Service service;
	
	/**
	 * The bind and unbind methods will be called by the service component runtime
	 * when a $serviceName$Service reference is detected in the local service registry
	 * @param s
	 */
	@Reference
	void bind$serviceName$Service($serviceName$Service s) {
		this.service = s;
		System.out.println("$serviceName$Service instance bound to consumer");
	}
	
	void unbind$serviceName$Service($serviceName$Service s) {
		this.service = null;
		System.out.println("$serviceName$Service instance unbound from consumer");
	}
	
	/**
	 * The activate method will be called immediately upon the $serviceName$Service 
	 * reference being bound to this component via the bind method$serviceName$Service
	 * method.
	 */
	void activate() {
		// test method
		String myName = "OSGi Service Consumer";
		System.out.println(myName+" calling $serviceName$Service...");
		System.out.println("$serviceName$Service responded: "+service.methodName(myName));
	}
}