package bndtools.facades.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jdt.core.IClasspathContainer;
import org.junit.Test;
import org.mockito.Mockito;

import biz.aQute.bnd.facade.api.Binder;
import biz.aQute.bnd.facade.api.Binder.TestAdapter;
import bndtools.facades.jdt.IClasspathContainerFacade;

public class IClasspathContainerFacadeTest {

	@Test
	public void testSimple() throws CoreException {
		TestAdapter ta = new Binder.TestAdapter() {};

		IClasspathContainerFacade icf = new IClasspathContainerFacade();
		EclipseBinder<IClasspathContainer> eb = icf;
		assertThat(eb.binder).isNull();
		IConfigurationElement ice = Mockito.mock(IConfigurationElement.class);
		icf.setInitializationData(ice, "whatever", "facade.id");
		assertThat(eb.binder).isNotNull();
		assertThat(eb.binder.getId()).isEqualTo("facade.id");
		assertThat(ta.facade(eb.binder)
			.get()).isEqualTo(icf);

	}
}
