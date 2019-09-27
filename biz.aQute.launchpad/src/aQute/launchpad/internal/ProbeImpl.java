package aQute.launchpad.internal;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import aQute.launchpad.Launchpad;
import aQute.launchpad.Probe;
import aQute.launchpad.Service;
import aQute.lib.exceptions.Exceptions;

public class ProbeImpl implements Probe {

	@Service
	Launchpad				launchpad;

	@Service(timeout = 100)
	ServiceComponentRuntime	scr;

	public ProbeImpl() {}

	@Override
	public void foo() {
		System.out.println("foo");
	}

	@Override
	public Closeable enable(Class<?> componentClass) {
		try {
			Bundle bundle = FrameworkUtil.getBundle(componentClass);
			List<String> components = new ArrayList<>();

			ComponentDescriptionDTO description = scr.getComponentDescriptionDTOs()
				.stream()
				.map(d -> {
					components.add(d.implementationClass);
					return d;
				})
				.filter(d -> d.implementationClass.equals(componentClass.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
					"No component class named " + componentClass.getName() + " present components are: " + components));

			scr.enableComponent(description);

			return () -> {
				try {
					scr.disableComponent(description)
						.getValue();
				} catch (InvocationTargetException | InterruptedException e) {
					throw Exceptions.duck(e);
				}
			};
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public boolean isOk() {
		try {
			if (scr == null)
				return false;

			scr.getClass();
			return true;
		} catch (Exception e0) {
			return false;
		}
	}

}
