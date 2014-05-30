package _package_.provider;


import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.log.LogService;

@SuppressWarnings("rawtypes")
@Component
public class _stem_Impl implements LogService {


	@Activate
	void activate() {
		
	}
	
	@Deactivate
	void deactivate() {
		
	}

	@Override
	public void log(int level, String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		// TODO Auto-generated method stub
		
	}
	
	
}
