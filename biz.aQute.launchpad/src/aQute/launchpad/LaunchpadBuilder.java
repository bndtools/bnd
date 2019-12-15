package aQute.launchpad;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.lib.io.IO;

/**
 * This class is a builder for frameworks that can be used in JUnit testing.
 */
public class LaunchpadBuilder extends AbstractLaunchpadBuilder<LaunchpadBuilder> {

	final static ExecutorService	executor			= Executors.newCachedThreadPool();
	final static File				projectDir			= IO.work;
	final static RemoteWorkspace	workspace			= RemoteWorkspaceClientFactory.create(projectDir,
		new RemoteWorkspaceClient() {});
	final static RunSpecification	projectTestSetup;
	final static AtomicInteger		counter				= new AtomicInteger();

	static {
		projectTestSetup = workspace.analyzeTestSetup(IO.work.getAbsolutePath());

		//
		// We only want the raw setup and not the run spec since this
		// makes it impossible to start a clean framework. Make them
		// immutable so that they can't be modified accidentally.
		//

		projectTestSetup.runbundles = Collections.emptyList();
		projectTestSetup.runpath = Collections.emptyList();
		projectTestSetup.properties = Collections.emptyMap();
		projectTestSetup.runfw = Collections.emptyList();

		projectTestSetup.extraSystemPackages = Collections.unmodifiableMap(projectTestSetup.extraSystemPackages);
		projectTestSetup.extraSystemCapabilities = Collections
			.unmodifiableMap(projectTestSetup.extraSystemCapabilities);

		Runtime.getRuntime()
			.addShutdownHook(new Thread(() -> {
				try {
					workspace.close();
				} catch (IOException e) {
					// ignore
				}
			}));
	}

	/**
	 * Start a framework assuming the current working directory is the project
	 * directory.
	 */
	public LaunchpadBuilder() {
		super(projectDir, workspace);
		// This ensures a deep clone.
		local.mergeWith(projectTestSetup);
	}

}
