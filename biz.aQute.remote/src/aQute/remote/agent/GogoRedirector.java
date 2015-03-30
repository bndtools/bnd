package aQute.remote.agent;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Redirects to a Gogo Command Processor
 */
public class GogoRedirector implements Redirector {

	private AgentServer agentServer;
	private ServiceTracker<CommandProcessor, CommandProcessor> tracker;
	private CommandProcessor processor;
	private CommandSession session;
	private RedirectInput stdin;
	private RedirectOutput stdout;
	
	public GogoRedirector(AgentServer agentServer, BundleContext context) {
		this.agentServer = agentServer;
		tracker = new ServiceTracker<CommandProcessor, CommandProcessor>(
				context, CommandProcessor.class, null) {
			@Override
			public CommandProcessor addingService(
					ServiceReference<CommandProcessor> reference) {
				CommandProcessor cp = super.addingService(reference);
					openSession(cp);
				return cp;
			}

			@Override
			public void removedService(
					ServiceReference<CommandProcessor> reference,
					CommandProcessor service) {
				super.removedService(reference, service);
				if ( service == processor) {
					closeSession(service);
					CommandProcessor replacement = getService();
					if ( replacement == null) {
						openSession(replacement);
					}
				}
			}

		};
		tracker.open();
	}

	private void closeSession(CommandProcessor service) {
		session.close();
	}

	private synchronized void openSession(CommandProcessor replacement) {
		if ( processor != null)
			return;

		processor = replacement;
		List<AgentServer> agents = Arrays.asList(agentServer);
		session = processor.createSession(stdin=new RedirectInput(), stdout=new RedirectOutput(agents, null, false), new RedirectOutput(agents, null, true));
		
	}
	@Override
	public void close() throws IOException {
		closeSession(processor);
	}

	@Override
	public int getPort() {
		return -1;
	}

	@Override
	public void stdin(String s) throws Exception {
		stdin.add(s);
	}

	@Override
	public PrintStream getOut() throws Exception {
		return stdout;
	}

}
