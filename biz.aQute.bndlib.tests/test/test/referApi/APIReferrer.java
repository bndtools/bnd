package test.referApi;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.cm.Configuration;
import org.osgi.service.device.Device;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.wireadmin.Wire;

// 000: Export test.referApi,  has private references [org.osgi.service.http, org.osgi.service.component, org.osgi.service.condpermadmin, org.osgi.service.wireadmin, org.osgi.service.event, org.osgi.service.log, org.osgi.service.device],

@SuppressWarnings("unused")
public abstract class APIReferrer extends AtomicReference<Device> implements EventAdmin {
	// refers to org.osgi.service.device and org.osgi.service.event

	private static final long	serialVersionUID	= 1L;

	static Configuration		config;

	public static HttpService publicReference() {
		return null;
	} // org.osgi.service.http

	protected static Wire protectedReference() {
		return null;
	} // org.osgi.service.wireadmin

	private static Configuration privateReference() {
		return null;
	}

	static Configuration packagePrivateReference() {
		return null;
	}

	public static void publicReference(org.osgi.service.component.ComponentConstants ad) {} // org.osgi.service.component

	protected static void protectedReference(BeanArgument ad) {} // org.osgi.service.blueprint.reflect

	static void packagePrivateReference(Configuration ad) {}

	private static void privateReference(Configuration ad) {}

	public static void publicFoo(Class<org.osgi.service.condpermadmin.BundleLocationCondition> foo) {} // org.osgi.service.condpermadmin

	protected static void protectedFoo(Class<LogService> foo) {} // org.osgi.service.log

	private static void privateFoo(Class<Configuration> foo) {}

	static void packagePrivateFoo(Class<Configuration> foo) {}

	public static void foo() {
		Configuration foo;
	}

	private static void foop() {
		Configuration foo;
	}
}
