package aQute.remote.launcher.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.lib.converter.Converter;
import aQute.remote.api.Agent;
import aQute.remote.supervisor.provider.SupervisorClient;

public class RemoteProjectLauncher extends ProjectLauncher {

	private SupervisorClient supervisor;
	private final Project project;

	public RemoteProjectLauncher(Project project) throws Exception {
		super(project);
		this.project = project;
	}

	@Override
	public String getMainTypeName() {
		return "";
	}

	@Override
	public void update() throws Exception {
		Map<String, String> newer = new HashMap<String, String>();

		for (Container c : Container.flatten(project.getRunbundles())) {
			if (c.getError() != null)
				continue;

			File f = c.getFile();
			if (!f.isFile()) {
				error("Run bundle is not a file: %s", f);
				continue;
			}
			
			String sha = supervisor.addFile(f);
			newer.put(f.getAbsolutePath(), sha);
		}
		
		supervisor.getAgent().update(newer);
	}

	@Override
	public void prepare() throws Exception {
		String remote = getProject().getProperty("-remote",
				"host=localhost,port=" + Agent.DEFAULT_PORT);
		Attrs attrs = OSGiHeader.parseProperties(remote);

		int port = Converter.cnv(Integer.class, attrs.get("port"));
		String host = Converter.cnv(String.class, attrs.get("host"));

		supervisor = SupervisorClient.link(host, port);
		
	}

	@Override
	public int launch() throws Exception {
		prepare();
		update();
		return supervisor.join();
	}

	public void cancel() throws IOException {
		supervisor.cancel();
	}
	
	public void close() throws IOException {
		supervisor.close();
	}

}
