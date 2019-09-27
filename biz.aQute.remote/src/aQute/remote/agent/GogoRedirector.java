package aQute.remote.agent;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Create a new Gogo Shell Command Session if there is a Gogo Command Processor
 * service present. If no Command Processor is present we will idle (a service
 * tracker is used that will handle multiple and switches).
 * <p>
 * There is a bit of a class space problem since the agent can be started on the
 * framework side of the class path. For this reason, we carry a copy of the
 * Gogo API classes and we will use proxies to use them. This leaves the Gogo
 * API unconstrained.
 */
public class GogoRedirector implements Redirector {

	private AgentServer											agentServer;
	private ServiceTracker<CommandProcessor, CommandProcessor>	tracker;
	private CommandProcessor									processor;
	private CommandSession										session;
	private Shell												stdin;
	private RedirectOutput										stdout;

	/**
	 * Create a redirector
	 *
	 * @param agentServer the server
	 * @param context the context, needed to get the
	 */
	public GogoRedirector(AgentServer agentServer, BundleContext context) {
		this.agentServer = agentServer;
		tracker = new ServiceTracker<CommandProcessor, CommandProcessor>(context, CommandProcessor.class.getName(),
			null) {

			@Override
			public CommandProcessor addingService(ServiceReference<CommandProcessor> reference) {
				CommandProcessor cp = proxy(CommandProcessor.class, super.addingService(reference));
				if (processor == null)
					openSession(cp);
				return cp;
			}

			@Override
			public void removedService(ServiceReference<CommandProcessor> reference, CommandProcessor service) {
				super.removedService(reference, service);
				if (service == processor) {
					closeSession(service);
					CommandProcessor replacement = getService();
					if (replacement != null) {
						openSession(replacement);
					}
				}
			}

		};
		tracker.open();
	}

	void closeSession(CommandProcessor service) {
		if (session != null) {
			session.close();
			processor = null;
		}
	}

	synchronized void openSession(CommandProcessor replacement) {
		processor = replacement;
		List<AgentServer> agents = Arrays.asList(agentServer);
		stdout = new RedirectOutput(agents, null, false);
		stdin = new Shell();
		session = processor.createSession(stdin, stdout, stdout);
		stdin.open(session);

	}

	/*
	 * Create a proxy on a class. This is to prevent class cast exceptions. We
	 * get our Gogo likely from another class loader since the agent can reside
	 * on the framework side and we can't force Gogo to import our classes (nor
	 * should we).
	 */
	@SuppressWarnings("unchecked")
	<T> T proxy(final Class<T> clazz, final Object target) {
		final Class<?> targetClass = target.getClass();

		//
		// We could also be in the same class space, in that case we
		// can just return the value
		//

		if (targetClass == clazz)
			return clazz.cast(target);

		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {
			clazz
		}, (proxy, method, args) -> {
			Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
			Object result = targetMethod.invoke(target, args);
			if (result != null && method.getReturnType()
				.isInterface() && targetMethod.getReturnType() != method.getReturnType())

				try {
					return proxy(method.getReturnType(), result);
				} catch (Exception e) {}
			return result;
		});
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
