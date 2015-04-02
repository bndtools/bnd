package aQute.remote.agent;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.felix.service.command.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;

/**
 * Redirects to a Gogo Command Processor
 */
public class GogoRedirector implements Redirector {

	private AgentServer agentServer;
	private ServiceTracker<CommandProcessor, CommandProcessor> tracker;
	CommandProcessor											processor;
	private CommandSession session;
	private Shell stdin;
	private RedirectOutput stdout;

	public GogoRedirector(AgentServer agentServer, BundleContext context) {
		this.agentServer = agentServer;
		tracker = new ServiceTracker<CommandProcessor, CommandProcessor>(
				context, CommandProcessor.class.getName(), null) {
			@Override
			public CommandProcessor addingService(
					ServiceReference<CommandProcessor> reference) {
				CommandProcessor cp = proxy(CommandProcessor.class,
						super.addingService(reference));
				if ( processor == null)
					openSession(cp);
				return cp;
			}

			@Override
			public void removedService(
					ServiceReference<CommandProcessor> reference,
					CommandProcessor service) {
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
		session = processor.createSession(stdin,
				stdout, stdout);
		stdin.open(session);

	}

	/*
	 * Create a proxy on a class. This is to prevent class cast exceptions. We
	 * get our Gogo likely from another class loader since the agent can reside
	 * on the framework side and we can't force Gogo to import our classes (nor should we).
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

		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class<?>[] { clazz }, new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						Method targetMethod = targetClass.getMethod(
								method.getName(), method.getParameterTypes());
						Object result = targetMethod.invoke(target, args);
						if (result != null && method.getReturnType().isInterface() && targetMethod.getReturnType()!=method.getReturnType())

							try {
								return proxy(method.getReturnType(), result);
							} catch (Exception e) {
							}
						return result;
					}
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
