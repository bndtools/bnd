package aQute.launcher.agent;

import java.lang.instrument.Instrumentation;

import org.osgi.annotation.bundle.Header;

@Header(name = "Premain-Class", value = "${@class}")
public class LauncherAgent {

	public static Instrumentation	instrumentation;
	public static String			agentArgs;

	public static void premain(String agentArgs, Instrumentation instrumentation) {
		LauncherAgent.instrumentation = instrumentation;
		LauncherAgent.agentArgs = agentArgs;
	}
}
