package aQute.bnd.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import aQute.bnd.main.RemoteCommand.DistroOptions;
import aQute.bnd.main.RemoteCommand.RemoteOptions;
import aQute.bnd.main.testlib.MockRegistry;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.CommandLine;
import aQute.lib.io.IO;
import biz.aQute.resolve.BndResolver;
import biz.aQute.resolve.ResolutionCallback;
import biz.aQute.resolve.ResolveProcess;
import biz.aQute.resolve.ResolverLogger;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class DistroCommandTest extends TestCase {

	private Framework	framework;
	private File		tmp;

	@Override
	protected void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp");
		tmp.mkdirs();

		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class,
				this.getClass().getClassLoader());

		FrameworkFactory ff = sl.iterator().next();
		Map<String,String> configuration = new HashMap<String,String>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		framework = ff.newFramework(configuration);
		framework.init();
		framework.start();
		BundleContext context = framework.getBundleContext();

		String[] bundles = {
				"../biz.aQute.remote/generated/biz.aQute.remote.agent.jar",
				"testdata/bundles/com.liferay.dynamic.data.mapping.taglib.jar",
				"testdata/bundles/com.liferay.item.selector.taglib.jar"
		};

		for (String bundle : bundles) {
			String location = "reference:" + IO.getFile(bundle).toURI().toString();
			Bundle b = context.installBundle(location);
			b.start();
		}

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		framework.stop();
		framework.waitForStop(10000);
		IO.delete(tmp);
		super.tearDown();
	}

	public void testMultipleCapabilitiesPerNamespace() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		new RemoteCommand(bnd, remoteOptions)._distro(distroOptions);

		assertTrue(distro.exists());

		Object capabilities = new Jar(distro).getManifest().getMainAttributes().getValue(Constants.PROVIDE_CAPABILITY);

		assertNotNull(capabilities);

		// should be at least two osgi.extender=jsp.taglib capabilities
		assertTrue(capabilities.toString().split("jsp.taglib").length == 3);
	}

	public void testResolveAgainstDistro() throws Exception {
		bnd bnd = new bnd();
		CommandLine cmdline = new CommandLine(null);
		List<String> remoteArgs = new ArrayList<>();
		RemoteOptions remoteOptions = cmdline.getOptions(RemoteOptions.class, remoteArgs);

		File distro = new File("generated/tmp/test.distro.jar");

		List<String> distroArgs = new ArrayList<>();
		distroArgs.add("-o");
		distroArgs.add(distro.getPath());
		distroArgs.add("test.distro");
		distroArgs.add("1.0.0");
		DistroOptions distroOptions = cmdline.getOptions(DistroOptions.class, distroArgs);

		new RemoteCommand(bnd, remoteOptions)._distro(distroOptions);

		assertTrue(distro.exists());

		ResolveProcess process = new ResolveProcess();
		ResolverLogger logger = new ResolverLogger();

		MockRegistry registry = new MockRegistry();

		Processor model = new Processor();

		model.setProperty("-distro", distro.getAbsolutePath() + ";version=file");
		model.setProperty("-runfw", "org.eclipse.osgi");
		model.setProperty("-runrequires",
				"osgi.wiring.package;filter:='(osgi.wiring.package=com.liferay.dynamic.data.mapping.taglib.servlet.taglib)'");

		Map<Resource,List<Wire>> requiredResources = process.resolveRequired(model, null, registry,
				new BndResolver(logger), Collections.<ResolutionCallback> emptyList(), logger);

		assertEquals(1, requiredResources.size());
	}

}
