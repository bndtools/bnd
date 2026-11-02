package org.bndtools.applaunch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class BndApplicationLauncherTest {

	@Test
	public void initializeRegistersNoopMainThreadService() {
		BundleContext context = mock(BundleContext.class);
		@SuppressWarnings("unchecked")
		ServiceRegistration<Runnable> registration = (ServiceRegistration<Runnable>) mock(ServiceRegistration.class);
		AtomicReference<Runnable> callback = new AtomicReference<>();
		AtomicReference<Dictionary<String, ?>> properties = new AtomicReference<>();
		when(context.registerService(eq(Runnable.class), any(Runnable.class),
			ArgumentMatchers.<Dictionary<String, ?>>any()))
			.thenAnswer(invocation -> {
				callback.set(invocation.getArgument(1));
				properties.set(invocation.getArgument(2));
				return registration;
			});

		assertThat(BndApplicationLauncher.registerInitializationCallback(context)).isSameAs(registration);
		assertThat(properties.get().get("main.thread")).isEqualTo("true");
		assertThatCode(callback.get()::run).doesNotThrowAnyException();
	}
}