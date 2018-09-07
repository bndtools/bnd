package test.cdi.beans_g;

import javax.enterprise.context.ApplicationScoped;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Reference;

@ApplicationScoped
@Bean
public class AppScopedBean {

	@Reference
	Foo foo;

}
