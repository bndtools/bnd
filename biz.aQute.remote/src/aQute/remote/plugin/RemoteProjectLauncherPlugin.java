package aQute.remote.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.RunSession;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.lib.converter.Converter;

public class RemoteProjectLauncherPlugin extends ProjectLauncher {
	private static Converter converter = new Converter();
	static {
		converter.setFatalIsException(false);
	}
	private Parameters runremote;
	private List<RunSessionImpl> sessions = new ArrayList<RunSessionImpl>();
	private boolean prepared;

	public RemoteProjectLauncherPlugin(Project project) throws Exception {
		super(project);
		runremote = new Parameters(getProject()
				.getProperty(Constants.RUNREMOTE));
	}

	@Override
	public String getMainTypeName() {
		return "";
	}

	@Override
	public void update() throws Exception {
		updateFromProject();
		
		Parameters runremote = new Parameters(getProject().getProperty(Constants.RUNREMOTE));
		
		for (RunSessionImpl session : sessions)
			try {
				Attrs attrs = runremote.get(session.getName());
				RunRemoteDTO dto = Converter.cnv(RunRemoteDTO.class,attrs);
				session.update(dto);
			} catch (Exception e) {
				getProject().exception(e, "Failed to update session %s", session.getName());
			}
	}

	@Override
	public void prepare() throws Exception {
		if ( prepared)
			return;
		
		prepared = true;
		
		updateFromProject();

		Map<String, Object> properties = new HashMap<String, Object>(
				getRunProperties());
		calculatedProperties(properties);

		for (Entry<String, Attrs> entry : runremote.entrySet()) {
			RunRemoteDTO dto = converter.convert(RunRemoteDTO.class,
					entry.getValue());
			dto.name = entry.getKey();

			Map<String, Object> sessionProperties = new HashMap<String, Object>(
					properties);
			sessionProperties.putAll(entry.getValue());
			sessionProperties.put("session.name", dto.name);
			RunSessionImpl session = new RunSessionImpl(this, dto, properties);
			sessions.add(session);
		}

	}

	@Override
	public int launch() throws Exception {
		throw new UnsupportedOperationException("This launcher only understands run sessions");
	}

	public void close() {
		for (RunSessionImpl session : sessions)
			try {
				session.close();
			} catch (Exception e) {
				// ignore
			}
	}

	public void cancel() throws Exception {
		for (RunSessionImpl session : sessions)
			try {
				session.cancel();
			} catch (Exception e) {
				// ignore
			}
	}

	@Override
	public void write(String text) throws Exception {
		throw new UnsupportedOperationException("This launcher only understands run sessions");
	}

	@Override
	public List<? extends RunSession> getRunSessions() throws Exception {
		prepare();
		return sessions;
	}

}
