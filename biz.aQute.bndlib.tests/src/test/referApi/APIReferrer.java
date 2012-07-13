package test.referApi;

import java.util.concurrent.atomic.*;

import org.osgi.service.blueprint.reflect.*;
import org.osgi.service.cm.*;
import org.osgi.service.device.*;
import org.osgi.service.event.*;
import org.osgi.service.http.*;
import org.osgi.service.log.*;
import org.osgi.service.wireadmin.*;

// 000: Export test.referApi,  has private references [org.osgi.service.http, org.osgi.service.component, org.osgi.service.condpermadmin, org.osgi.service.wireadmin, org.osgi.service.event, org.osgi.service.log, org.osgi.service.device], 

public abstract class APIReferrer extends AtomicReference<Device> implements   EventAdmin {
	// refers to org.osgi.service.device and org.osgi.service.event

	Configuration config;
	
	
	public HttpService publicReference() { return null; }		// org.osgi.service.http
	protected Wire protectedReference() { return null; }        // org.osgi.service.wireadmin
	
	private Configuration privateReference() { return null; }
	Configuration packagePrivateReference() { return null; }
    
    public void publicReference( org.osgi.service.component.ComponentConstants ad) {} // org.osgi.service.component
    protected void protectedReference( BeanArgument ad) {}  // org.osgi.service.blueprint.reflect
    
    void packagePrivateReference( Configuration ad) {}
    private void privateReference( Configuration ad) {}
	
    public void publicFoo( Class<org.osgi.service.condpermadmin.BundleLocationCondition> foo) {} // org.osgi.service.condpermadmin
    protected void protectedFoo( Class<LogService> foo) {} // org.osgi.service.log
    
    private void privateFoo( Class<Configuration> foo) {}
    void packagePrivateFoo( Class<Configuration> foo) {}
    
    public void foo() {
    	Configuration foo;
    }
    private void foop() {
    	Configuration foo;
    }
}
