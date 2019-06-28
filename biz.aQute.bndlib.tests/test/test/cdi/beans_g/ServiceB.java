package test.cdi.beans_g;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.MinimumCardinality;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.BeanServiceObjects;

@Bean
@SingleComponent
public class ServiceB implements Foo, Serializable {
	private static final long				serialVersionUID	= 809092323563388531L;

	// level A
	@Reference
	Character								c_value;

	@Reference
	ServiceReference<Integer>				i_value;

	@Reference(Long.class)
	Map<String, Object>						l_value;

	@Reference
	Map.Entry<Map<String, Object>, Boolean>	b_value;

	@Reference
	BeanServiceObjects<Short>				s_value;

	// level B
	@Reference
	Optional<Bar>							bar;

	void set(@Reference @MinimumCardinality(2) List<Baz> bazi, @Reference Bif bif) {}

	public ServiceB(@Reference Provider<Fum> value, @Reference Provider<List<Glum>> glums) {}
}
