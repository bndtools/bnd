package biz.aQute.remote;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.unmodifiable.Maps;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.io.IO;
import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.api.XComponentDTO;
import aQute.remote.api.XEventDTO;
import aQute.remote.api.XResultDTO;
import aQute.remote.util.AgentSupervisor;

@SuppressWarnings("deprecation")
public class EnhancedAgentTest {

	private LaunchpadBuilder	builder;
	private TestSupervisor		supervisor;

	@InjectTemporaryDirectory
	File						tempDir;

	@BeforeEach
	public void before() throws Exception {
		IO.copy(IO.getFile("testdata/agent"), tempDir);
		builder = new LaunchpadBuilder();
		supervisor = new TestSupervisor();
	}

	@AfterEach
	public void after() throws Exception {
		IO.closeAll(builder);
		supervisor.close();
	}

	@Test
	public void test_all_bundles() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
		}
	}

	@Test
	public void test_all_components_with_scr_installed() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_components_with_scr_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(supervisor.getAgent()
				.getAllComponents()).isNotEmpty();
		}
	}

	@Test
	public void test_all_components_without_scr_installed() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllComponents()).isEmpty();
		}
	}

	@Test
	public void test_all_configurations_with_cm_installed() throws Exception {
		try (Launchpad launchpad = builder
			.bndrun(IO.getFile(tempDir, "test_all_configurations_with_cm_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			launchpad.getService(ConfigurationAdmin.class)
				.ifPresent(cm -> {
					try {
						Configuration configuration = cm.getConfiguration("dummy");
						configuration.update(new Hashtable<>(Collections.emptyMap()));
					} catch (IOException e) {}
				});
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(launchpad.getService(ConfigurationAdmin.class)).isPresent();
			assertThat(launchpad.getService(MetaTypeService.class)).isNotPresent();
			assertThat(supervisor.getAgent()
				.getAllConfigurations()).isNotEmpty();
		}
	}

	@Test
	public void test_all_properties() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllProperties()).isNotEmpty();
		}
	}

	@Test
	public void test_all_threads() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
		}
	}

	@Test
	public void test_all_services() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.getAllServices()
				.stream())
					.anyMatch(service -> service.types.contains(StartLevel.class.getName())
						&& service.registeringBundle.equals("org.apache.felix.framework"));
			assertThat(supervisor.getAgent()
				.getAllServices()
				.stream())
					.anyMatch(service -> service.types.contains(PackageAdmin.class.getName())
						&& service.registeringBundle.equals("org.apache.felix.framework"));
		}
	}

	@Test
	public void test_enable_component() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_components_with_scr_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.enableComponent("bnd.with.ds.DummyComponent").result).isEqualTo(XResultDTO.SUCCESS);
			XComponentDTO xComponentDTO = supervisor.getAgent()
				.getAllComponents()
				.stream()
				.filter(comp -> comp.name.equals("bnd.with.ds.DummyComponent"))
				.findAny()
				.get();
			assertThat(supervisor.getAgent()
				.disableComponent(xComponentDTO.name).result).isEqualTo(XResultDTO.SUCCESS);
			xComponentDTO = supervisor.getAgent()
				.getAllComponents()
				.stream()
				.filter(comp -> comp.name.equals("bnd.with.ds.DummyComponent"))
				.findAny()
				.get();
			assertThat(xComponentDTO.state).isEqualTo("DISABLED");
			assertThat(supervisor.getAgent()
				.enableComponent(xComponentDTO.id).result).isEqualTo(XResultDTO.SUCCESS);
			xComponentDTO = supervisor.getAgent()
				.getAllComponents()
				.stream()
				.filter(comp -> comp.name.equals("bnd.with.ds.DummyComponent"))
				.findAny()
				.get();
			// no associated id and that's why cannot be enabled by id
			assertThat(xComponentDTO.state).isEqualTo("DISABLED");
		}
	}

	@Test
	public void test_enable_component_without_scr() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.enableComponent("a.b.c").result).isEqualTo(XResultDTO.SKIPPED);
		}
	}

	@Test
	public void test_delete_config() throws Exception {
		try (Launchpad launchpad = builder
			.bndrun(IO.getFile(tempDir, "test_all_configurations_with_cm_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			launchpad.getService(ConfigurationAdmin.class)
				.ifPresent(cm -> {
					try {
						Configuration configuration = cm.getConfiguration("dummy");
						configuration.update(new Hashtable<>(Collections.emptyMap()));
					} catch (IOException e) {}
				});
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).anyMatch(dto -> dto.pid.equals("dummy"));
			assertThat(supervisor.getAgent()
				.deleteConfiguration("dummy").result).isEqualTo(XResultDTO.SUCCESS);
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).noneMatch(dto -> dto.pid.equals("dummy"));
		}
	}

	@Test
	public void test_delete_config_without_cm() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).noneMatch(dto -> dto.pid.equals("dummy"));
			assertThat(supervisor.getAgent()
				.deleteConfiguration("dummy").result).isEqualTo(XResultDTO.SKIPPED);
		}
	}

	@Test
	public void test_create_factory_config() throws Exception {
		try (Launchpad launchpad = builder
			.bndrun(IO.getFile(tempDir, "test_all_configurations_with_cm_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).noneMatch(dto -> dto.factoryPid.equals("dummy"));
			assertThat(supervisor.getAgent()
				.createFactoryConfiguration("dummy", Maps.of("key", "val")).result).isEqualTo(XResultDTO.SUCCESS);
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).anyMatch(dto -> dto.factoryPid.equals("dummy"));
		}
	}

	@Test
	public void test_create_factory_config_without_cm() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).noneMatch(dto -> dto.factoryPid.equals("dummy"));
			assertThat(supervisor.getAgent()
				.createFactoryConfiguration("dummy", Maps.of("key", "val")).result).isEqualTo(XResultDTO.SKIPPED);
			assertThat(supervisor.getAgent()
				.getAllConfigurations()
				.stream()).noneMatch(dto -> dto.factoryPid.equals("dummy"));
		}
	}

	@Test
	public void test_create_config() throws Exception {
		try (Launchpad launchpad = builder
			.bndrun(IO.getFile(tempDir, "test_all_configurations_with_cm_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.createOrUpdateConfiguration("a.b", Maps.of("key", "val")).result).isEqualTo(XResultDTO.SUCCESS);
		}
	}

	@Test
	public void test_create_config_without_cm() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.createOrUpdateConfiguration("a.b", Maps.of("key", "val")).result).isEqualTo(XResultDTO.SKIPPED);
		}
	}

	@Test
	public void test_disable_component_without_scr() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_without_optionals.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(3);
			assertThat(supervisor.getAgent()
				.getAllThreads()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.disableComponent(0).result).isEqualTo(XResultDTO.SKIPPED);
		}
	}

	@Test
	public void test_disable_component() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_components_with_scr_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			XComponentDTO xComponentDTO = supervisor.getAgent()
				.getAllComponents()
				.stream()
				.filter(comp -> comp.name.equals("bnd.with.ds.DummyComponent"))
				.findAny()
				.get();
			assertThat(supervisor.getAgent()
				.disableComponent(xComponentDTO.name).result).isEqualTo(XResultDTO.SUCCESS);
			xComponentDTO = supervisor.getAgent()
				.getAllComponents()
				.stream()
				.filter(comp -> comp.name.equals("bnd.with.ds.DummyComponent"))
				.findAny()
				.get();
			assertThat(xComponentDTO.state).isEqualTo("DISABLED");
		}
	}

	@Test
	public void test_runtime_info() throws Exception {
		try (Launchpad launchpad = builder.bndrun(IO.getFile(tempDir, "test_all_components_with_scr_installed.bndrun"))
			.create()) {
			supervisor.connect("localhost", Agent.DEFAULT_PORT);
			assertThat(supervisor.getAgent()).isNotNull();
			assertThat(supervisor.getAgent()
				.getAllBundles()).hasSize(5);
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()).isNotEmpty();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("Framework")).isEqualTo("org.apache.felix.framework");
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("Framework Version")).isNotNull();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("Memory Total")).isNotNull();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("Memory Free")).isNotNull();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("OS Name")).isNotNull();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("OS Version")).isNotNull();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("OS Architecture")).isNotNull();
			assertThat(supervisor.getAgent()
				.getRuntimeInfo()
				.get("Uptime")).isNotNull();
		}
	}

	static class TestSupervisor extends AgentSupervisor<Supervisor, Agent> implements Supervisor {
		@Override
		public boolean stdout(String out) throws Exception {
			return true;
		}

		@Override
		public boolean stderr(String out) throws Exception {
			return true;
		}

		public void connect(String host, int port) throws Exception {
			super.connect(Agent.class, this, host, port);
		}

		public void connect(String host, int port, int timeout) throws Exception {
			super.connect(Agent.class, this, host, port, timeout);
		}

		@Override
		public void event(Event e) throws Exception {
			System.out.println(e);
		}

		@Override
		public void onOSGiEvent(XEventDTO event) {
			// TODO
		}
	}

}
