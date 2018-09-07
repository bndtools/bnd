package test.cdi.beans_f;

import java.io.Serializable;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@Bean
@Service(Foo.class)
@SingleComponent
public class ServiceB implements Foo, Serializable {
	private static final long serialVersionUID = 809092323563388531L;
}
