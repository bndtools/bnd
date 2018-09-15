package test.cdi.beans_h;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.reference.BindBeanServiceObjects;
import org.osgi.service.cdi.reference.BindService;
import org.osgi.service.cdi.reference.BindServiceReference;

@Bean
public class AppScopedBean {
	void bindChars(BindService<Character> c) {}

	void bindInts(BindBeanServiceObjects<Integer> i) {}

	void bindLongs(BindServiceReference<Long> l) {}
}
