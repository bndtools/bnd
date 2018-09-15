package test.cdi.beans_f;

import java.io.Serializable;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@Bean
@SingleComponent
// bnd's Clazz parser doesn't handle type_use annotations :(
public class ServiceC extends @Service Blah implements @Service Bar, @Service Fee, Serializable {
	private static final long serialVersionUID = 809092323563388531L;
}
