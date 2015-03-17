package aQute.remote.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.lib.converter.Converter;
import aQute.remote.api.Agent;

public class RemoteProjectLauncherPlugin extends ProjectLauncher {

	private LauncherSupervisor supervisor;

	public RemoteProjectLauncherPlugin(Project project) throws Exception {
		super(project);
	}

	@Override
	public String getMainTypeName() {
		return "";
	}

	@Override
	public void update() throws Exception {
		updateFromProject();
		Map<String, String> newer = getBundles(getRunBundles(), Constants.RUNBUNDLES);
		supervisor.getAgent().update(newer);
	}

	@Override
	public void prepare() throws Exception {
		
		updateFromProject();
		
		Parameters runremote = new Parameters(getProject().getProperty(
				"-runremote"));

		for (Entry<String, Attrs> entry : runremote.entrySet()) {
			RunRemoteDTO dto = Converter.cnv(RunRemoteDTO.class,
					entry.getValue());

			dto.name = entry.getKey();
			if (dto.port <= 0)
				dto.port = Agent.DEFAULT_PORT;

			if (dto.host == null)
				dto.host = "localhost";

			supervisor = new LauncherSupervisor();
			supervisor.connect(dto.host,dto.port);
			
			Agent agent = supervisor.getAgent();

			if (agent.isEnvoy()) {
				int secondaryPort = installFramework(agent, dto, entry.getValue());
				supervisor.close();
				supervisor = new LauncherSupervisor();
				supervisor.connect(dto.host,secondaryPort);
			}

			supervisor.setStreams(out, err);
			break;
		}

	}

	private int installFramework(Agent agent, RunRemoteDTO dto,
			Attrs attrs) throws Exception {
		List<String> onpath = new ArrayList<String>(getRunpath());
		
		Map<String, String> runpath = getBundles(onpath, Constants.RUNPATH);
		
		Map<String,Object> properties = new HashMap<String, Object>(getRunProperties());
		calculatedProperties(properties);
		for ( Entry<String, String> entry : attrs.entrySet()) {
			properties.put(entry.getKey(), entry.getValue());
		}
		
		return agent.createFramework(dto.name, runpath.values(), properties);
	}

	/**
	 * This method should go to the ProjectLauncher
	 * @throws Exception 
	 */
	
	private void calculatedProperties(Map<String, Object> properties) throws Exception {
		boolean runKeep = getProject().getRunKeep();
		Parameters capabilities = new Parameters(getProject().mergeProperties(Constants.RUNSYSTEMCAPABILITIES));
		Parameters packages = new Parameters(getProject().mergeProperties(Constants.RUNSYSTEMPACKAGES));
		
		if ( !runKeep )
			properties.put(org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN, org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

		if ( !capabilities.isEmpty())
			properties.put(org.osgi.framework.Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, capabilities.toString());
		
		if ( !packages.isEmpty())
			properties.put(org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, packages.toString());
		
	}

	@Override
	public int launch() throws Exception {
		prepare();
		update();
		return supervisor.join();
	}

	public void close() throws IOException {
		supervisor.close();
	}

	public void cancel() throws Exception {
		supervisor.getAgent().abort();
	}

	@Override
	public void write(String text) throws Exception {
		supervisor.getAgent().stdin(text);
	}

	private Map<String, String> getBundles(Collection<String> collection, String header)
			throws Exception {
		Map<String, String> newer = new HashMap<String, String>();

		for (String c : collection) {
			File f = new File(c);
			String sha = supervisor.addFile(f);
			newer.put(c, sha);
		}
		return newer;
	}

	
	
}
