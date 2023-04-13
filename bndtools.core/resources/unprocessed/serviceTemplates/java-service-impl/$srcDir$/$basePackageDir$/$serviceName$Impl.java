package $basePackageName$;

import org.osgi.service.component.annotations.Component;

import $api_package$.$serviceName$Service;

/**
 * An implementation of the $serviceName$Service.  
  *
 */
@Component(immediate=true)
public class $serviceName$Impl implements $serviceName$Service {
	
	public String methodName(String arg) {
		System.out.println("$serviceName$Impl received method call with arg="+arg);
		return "Hello " + arg;
	}
	
}