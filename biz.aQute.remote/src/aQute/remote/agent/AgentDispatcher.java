package aQute.remote.agent;

import java.io.DataOutputStream;
import java.io.File;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;
import aQute.remote.util.Link;

public class AgentDispatcher {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static AgentServer createFramework(String name,
			Map<String, Object> configuration, final File storage,
			final File shacache, DataOutputStream out) throws Exception {

		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(
				FrameworkFactory.class, AgentServer.class.getClassLoader());
		FrameworkFactory ff = null;
		for (FrameworkFactory fff : sl) {
			ff = fff;
			// break;
		}

		if (ff == null)
			throw new IllegalArgumentException("No framework on runpath");

		Framework framework = ff.newFramework((Map) configuration);
		framework.init();
		framework.getBundleContext().addFrameworkListener(
				new FrameworkListener() {

					@Override
					public void frameworkEvent(FrameworkEvent event) {
						System.err.println("FW Event " + event);
					}
				});

		framework.start();
		final BundleContext context = framework.getBundleContext();

		AgentServer as = new AgentServer(name, context, shacache);
		Link<Agent,Supervisor> link = new Link<Agent,Supervisor>(Supervisor.class, as, null, out);
		as.setLink(link);
		return as;
	}

}
