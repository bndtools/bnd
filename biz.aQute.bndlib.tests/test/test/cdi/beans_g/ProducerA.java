package test.cdi.beans_g;

import javax.enterprise.inject.Produces;

import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

@Bean
public class ProducerA {

	@Produces
	@Service
	Gru create(@Reference Fee fee) {
		return new Gru() {};
	}

	@Produces
	@Service
	Glum glum = new Glum() {};

}
