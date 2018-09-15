package test.dynamicimport;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;

public class DynamicImport implements ConfigurationAdmin {

	public DynamicImport(BundleContext bc, EventAdmin ea) {}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration getConfiguration(String pid) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration getConfiguration(String pid, String location) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

}
