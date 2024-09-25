package test.servicecapability;

import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.osgi.namespace.service.ServiceNamespace;

import aQute.bnd.annotation.service.ServiceCapability;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Domain;
import aQute.lib.io.IO;
import test.servicecapability.misc.Foo;
import test.servicecapability.misc.Foo2;
import test.servicecapability.misc.Foo3;
import test.servicecapability.misc2.Bar;

@ServiceCapability(value = ServiceCapabilityTest.class)
@ServiceCapability(value = Foo.class, attribute = "test=test")
@ServiceCapability(value = Foo2.class, uses = Bar.class)
@ServiceCapability(value = Foo3.class, uses = {
	Bar.class, ServiceCapabilityTest.class
})
public class ServiceCapabilityTest {


	@Test
	public void testServiceCapability() throws Exception {
		Attrs attrs = assertCapability(ServiceCapabilityTest.class);
		Assertions.assertThat(attrs)
			.containsEntry("uses:", "test.servicecapability");
		Assertions.assertThat(attrs)
			.containsEntry("effective:", "active");
	}

	@Test
	public void testServiceCapabilityBar() throws Exception {
		Attrs attrs = assertCapability(Bar.class);
		Assertions.assertThat(attrs)
			.containsEntry("uses:", "test.servicecapability.misc2");
	}

	@Test
	public void testServiceCapabilityFoo() throws Exception {
		Attrs attrs = assertCapability(Foo.class);
		Assertions.assertThat(attrs)
			.containsEntry("uses:", "test.servicecapability.misc");
		Assertions.assertThat(attrs)
			.containsEntry("test", "test");
	}

	@Test
	public void testServiceCapabilityFoo2() throws Exception {
		Attrs attrs = assertCapability(Foo2.class);
		Assertions.assertThat(attrs)
			.containsEntry("uses:", "test.servicecapability.misc2");
	}

	@Test
	public void testServiceCapabilityFoo3() throws Exception {
		Attrs attrs = assertCapability(Foo3.class);
		Assertions.assertThat(attrs)
			.hasEntrySatisfying("uses:", v -> v.contains("test.servicecapability.misc2"));
		Assertions.assertThat(attrs)
			.hasEntrySatisfying("uses:", v -> v.contains("test.servicecapability"));
	}

	Attrs assertCapability(Class<?> serviceClazz) throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.servicecapability, test.servicecapability.misc, test.servicecapability.misc2");
			b.build();


			b.getJar()
				.getManifest()
				.write(System.out);

			Domain domain = Domain.domain(b.getJar()
				.getManifest());

			Parameters provideCapability = domain.getProvideCapability();

			Optional<Attrs> first = provideCapability.entrySet()
				.stream()
				.filter(e -> e.getKey()
					.startsWith(ServiceNamespace.SERVICE_NAMESPACE))
				.map(Map.Entry<String, Attrs>::getValue)
				.filter(a -> a.get("objectClass")
					.equals(serviceClazz.getName()))
				.findFirst();

			Attrs attrs = first.orElse(null);
			Assertions.assertThat(attrs)
				.isNotNull();
			return attrs;
		}
	}

}
