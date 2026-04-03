package org.bndtools.refactor.types;

import java.util.Date;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component
public class TestRefactors {

	interface Bar {}

	@Activate
	public TestRefactors(BundleContext context, @Reference
	Bar serice) {}

	@Reference
	void setBar(Bar bar) {}

	void unsetBar(Bar bar) {}

	Date getDate() {
		return new Date();
	}

	@Deactivate
	void close() {}
}
