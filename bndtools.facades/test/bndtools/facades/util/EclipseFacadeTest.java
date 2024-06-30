package bndtools.facades.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import biz.aQute.bnd.facade.api.Binder;
import bndtools.facades.IAdapterFactoryFacade;
import bndtools.facades.IAdapterFactoryFacade.Delegate;
import bndtools.facades.IAdapterFactoryFacade.Facade;
import bndtools.facades.JTAGFacades;

class EclipseFacadeTest {

	@Test
	void testIAdapterFactoryFacade() throws CoreException {
		IAdapterFactoryFacade a = new IAdapterFactoryFacade();
		assertThat(a).isInstanceOf(IExecutableExtensionFactory.class);

		IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
		Mockito.when(config.getName())
			.thenReturn("test");

		a.setInitializationData(config, "property", "id=foo.bar,timeout=30,description='hello world'");

		IAdapterFactoryFacade.Facade c = (Facade) a.create();
		assertThat(c).isInstanceOf(IAdapterFactory.class);

		Supplier<Delegate> supplier = JTAGFacades.facade(c);
		assertThat(supplier).isInstanceOf(Binder.class);
		@SuppressWarnings("resource")
		Binder<?> binder = (Binder<?>) supplier;
		assertThat(binder.getId()).isEqualTo("foo.bar");
		assertThat(binder.getTimeout()).isEqualTo(30_000_000_000L);
		assertThat(binder.getFacade()
			.get()).isEqualTo(c);
		assertThat(binder.getDomainType()).isEqualTo(IAdapterFactoryFacade.Delegate.class);
	}

}
