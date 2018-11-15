package biz.aQute.bnd.diagnostics.gogo.impl;

import java.io.Closeable;
import java.io.IOException;

import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;

public class ComponentAnalyzer implements Closeable, Converter {

	final ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scr;

	public ComponentAnalyzer(BundleContext context) {
		scr = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(context,
			ServiceComponentRuntime.class, null);
		scr.open();
	}



	@Descriptor("Show the list of available components")
	public Object ds(Bundle... bs) {
		return getScr().getComponentDescriptionDTOs(bs);
	}

	private ServiceComponentRuntime getScr() {
		return scr.getService();
	}



	@Override
	public void close() throws IOException {
		scr.close();
	}

	@Override
	public Object convert(Class<?> arg0, Object arg1) throws Exception {
		return null;
	}

	@Override
	public CharSequence format(Object arg0, int arg1, Converter arg2) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
