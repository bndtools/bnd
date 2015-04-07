package aQute.remote.plugin;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.lib.converter.*;
import aQute.remote.util.*;

/**
 * This is the plugin. It is found by bnd on the -runpath when it needs to
 * launch. The plugin is reponsible for launching the specification. In this
 * case, we will inspect -runremote. The -runremote can contain a number of
 * remote framework specifications.
 */
public class RemoteProjectLauncherPlugin extends ProjectLauncher {
	private static Converter		converter	= new Converter();
	static {
		converter.setFatalIsException(false);
	}
	private Parameters				runremote;
	private List<RunSessionImpl>	sessions	= new ArrayList<RunSessionImpl>();
	private boolean					prepared;

	/**
	 * The well defined launcher
	 * 
	 * @param project
	 *            the project or Run
	 */
	public RemoteProjectLauncherPlugin(Project project) throws Exception {
		super(project);
		runremote = new Parameters(getProject().getProperty(Constants.RUNREMOTE));
	}

	/**
	 * We do not have a main for a remote framework
	 */
	@Override
	public String getMainTypeName() {
		return "";
	}

	/**
	 * Called when a change in the IDE is detected. We will then upate from the
	 * project and then update the remote framework.
	 */
	@Override
	public void update() throws Exception {
		updateFromProject();

		Parameters runremote = new Parameters(getProject().getProperty(Constants.RUNREMOTE));

		for (RunSessionImpl session : sessions)
			try {
				Attrs attrs = runremote.get(session.getName());
				RunRemoteDTO dto = Converter.cnv(RunRemoteDTO.class, attrs);
				session.update(dto);
			}
			catch (Exception e) {
				getProject().exception(e, "Failed to update session %s", session.getName());
			}
	}

	/**
	 * We parse the -runremote and create sessions for each one of them
	 */

	@Override
	public void prepare() throws Exception {
		if (prepared)
			return;

		prepared = true;

		updateFromProject();

		Map<String,Object> properties = new HashMap<String,Object>(getRunProperties());
		calculatedProperties(properties);

		for (Entry<String,Attrs> entry : runremote.entrySet()) {
			RunRemoteDTO dto = converter.convert(RunRemoteDTO.class, entry.getValue());
			dto.name = entry.getKey();

			Map<String,Object> sessionProperties = new HashMap<String,Object>(properties);
			sessionProperties.putAll(entry.getValue());
			sessionProperties.put("session.name", dto.name);

			tryDeployBundleFromRunpath(sessionProperties, "biz.aQute.remote.agent");

			RunSessionImpl session = new RunSessionImpl(this, dto, properties);
			sessions.add(session);
		}
	}

	/**
	 * TODO provide backward compatibility with the older API when IDE did not
	 * have multiple sessions. This should be straightforward to do since this
	 * method should not return until the process has exited. So we should be
	 * able to just launch all the sessions in their own threads and then sync.
	 */
	@Override
	public int launch() throws Exception {
		throw new UnsupportedOperationException("This launcher only understands run sessions");
	}

	/**
	 * Make sure all sessions are closed
	 */
	public void close() {
		for (RunSessionImpl session : sessions)
			try {
				session.close();
			}
			catch (Exception e) {
				// ignore
			}
	}

	/**
	 * Kill!
	 */
	public void cancel() throws Exception {
		for (RunSessionImpl session : sessions)
			try {
				session.cancel();
			}
			catch (Exception e) {
				// ignore
			}
	}

	/**
	 * Send any given text to the remote framework and treat it as input
	 */
	@Override
	public void write(String text) throws Exception {
		throw new UnsupportedOperationException("This launcher only understands run sessions");
	}

	/**
	 * Get the sessions
	 */
	@Override
	public List< ? extends RunSession> getRunSessions() throws Exception {
		prepare();
		return sessions;
	}

	private void tryDeployBundleFromRunpath(Map<String,Object> sessionProperties, String bsn) {
		final String jmx = (String) sessionProperties.get("jmx");

		JMXBundleDeployer jmxBundleDeployer = null;

		try {
			if (jmx != null) {
				int port = Integer.parseInt(jmx);
				jmxBundleDeployer = new JMXBundleDeployer(port);
			} else {
				jmxBundleDeployer = new JMXBundleDeployer();
			}
		}
		catch (Exception e) {
			// ignore
		}

		if (jmxBundleDeployer != null) {
			for (String path : this.getRunpath()) {
				File file = new File(path);
				try {
					if (bsn.equals(new Jar(file).getBsn())) {
						long bundleId = jmxBundleDeployer.deploy(bsn, file);

						trace("agent installed bundleId ", bundleId);
						break;
					}
				}
				catch (Exception e) {
					//
				}
			}
		}
	}
}
