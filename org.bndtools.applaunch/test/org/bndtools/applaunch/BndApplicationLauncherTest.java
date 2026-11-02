package org.bndtools.applaunch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
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

	@Test
	public void initializeCompletesWithoutAnApplicationBundle() {
		BundleContext context = mock(BundleContext.class);
		@SuppressWarnings("unchecked")
		ServiceReference<Object> reference = (ServiceReference<Object>) mock(ServiceReference.class);
		@SuppressWarnings("unchecked")
		ServiceRegistration<Runnable> registration = (ServiceRegistration<Runnable>) mock(ServiceRegistration.class);
		when(reference.getProperty("launcher.arguments")).thenReturn(new String[] {
			"-initialize"
		});
		when(context.registerService(eq(Runnable.class), any(Runnable.class),
			ArgumentMatchers.<Dictionary<String, ?>>any())).thenReturn(registration);

		assertThat(new LauncherTracker(context).addingService(reference)).isSameAs(registration);
		verify(context, never()).getBundles();
		verify(context, never()).registerService(eq(ApplicationLauncher.class), any(ApplicationLauncher.class), any());
	}
}