package aQute.tester.test2;

import static aQute.lib.exceptions.ConsumerWithException.asConsumer;
import static aQute.lib.exceptions.FunctionWithException.asFunction;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import aQute.bnd.build.Run;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.lib.io.IO;

public class TerminateFalseTest {

	protected static File					tmp;
	protected static Map<String, Object>	configuration;
	protected static Framework				framework;
	protected static BundleContext			context;
	protected static String					location;
	protected static FileSetRepository		testRepository;

	@SuppressWarnings("restriction")
	@BeforeAll
	public static void setUp(TestInfo testInfo) throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + testInfo.getDisplayName());
		IO.delete(tmp);
		IO.mkdirs(tmp);

		testRepository = new FileSetRepository("testpath",
			Stream.concat(StreamSupport.stream(Files.newDirectoryStream(IO.getFile("generated/")
				.toPath(), "*.jar")
				.spliterator(), false), classpath())
				.map(Path::toFile)
				.collect(toList()));

		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, new File(tmp, "fwstorage").getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.osgi.framework.launch;version=1.2");
		configuration.put("gosh.args", "-sc telnetd -p11311 start");

		framework = new org.apache.felix.framework.FrameworkFactory().newFramework(configuration);
		framework.init();
		framework.start();
		context = framework.getBundleContext();

		Stream
			.of("org.apache.felix.gogo.runtime", "org.apache.felix.gogo.shell", "org.apache.felix.gogo.command",
				"biz.aQute.remote.agent")
			.map(asFunction(bsn -> testRepository.get(bsn, testRepository.versions(bsn)
				.first(), null)))
			.map(File::toURI)
			.map(URI::toString)
			.map("reference:"::concat)
			.map(asFunction(context::installBundle))
			.forEach(asConsumer(Bundle::start));
	}

	@AfterAll
	public static void tearDown() throws Exception {
		assertThat(framework.getState()).as("framework state is active")
			.matches(i -> (i & Bundle.ACTIVE) == Bundle.ACTIVE);
		framework.stop();
		framework.waitForStop(10000);
		IO.delete(tmp);
	}

	@Test
	public void testTester() throws Exception {
		Run run = executeRemoteTests("remote-test-junit4.bndrun");

		assertThat(run.isOk()).isTrue();
	}

	@Test
	public void testTesterJUnitPlatform() throws Exception {
		Run run = executeRemoteTests("remote-test-junit5.bndrun");

		assertThat(run.isOk()).isFalse();
	}

	@SuppressWarnings("restriction")
	protected Run executeRemoteTests(String bndrun) throws Exception {
		File runFile = IO.getFile(bndrun);

		Run run = Run.createRun(null, runFile);

		run.getWorkspace()
			.addBasicPlugin(testRepository);
		run.test(IO.getFile("generated/tmp/testdir"), null);
		return run;
	}

	static Stream<Path> classpath() {
		ClassLoader classLoader = TerminateFalseTest.class.getClassLoader();
		try {
			Field f = classLoader.getClass()
				.getDeclaredField("ucp");
			f.setAccessible(true);
			Object ucpObj = f.get(classLoader);
			f = ucpObj.getClass()
				.getDeclaredField("path");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<URL> path = (List<URL>) f.get(ucpObj);
			return path.stream()
				.map(asFunction(URL::toURI))
				.map(Paths::get);
		} catch (Throwable t) {
			return Stream.empty();
		}
	}

}
