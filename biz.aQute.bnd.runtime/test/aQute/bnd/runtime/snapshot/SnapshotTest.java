package aQute.bnd.runtime.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.junit.BeforeClass;
import org.junit.Test;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.lib.io.IO;

public class SnapshotTest {
	static final String	org_apache_felix_framework		= "org.apache.felix.framework;version='[5.6.10,5.6.11)'";
	static final String	org_apache_felix_scr			= "org.apache.felix.scr;version='[2.1.12,2.1.13)'";
	static final String	org_apache_felix_log			= "org.apache.felix.log;version='[1.2.0,1.2.1)'";
	static final String	org_apache_felix_configadmin	= "org.apache.felix.configadmin;version='[1.9.10,1.9.11)'";
	static final String	org_apache_felix_gogo_runtime	= "org.apache.felix.gogo.runtime;version='[1.1.0,1.1.0]'";
	static File tmp = IO.getFile("generated/snapshot");

	@BeforeClass
	static public void before() {
		IO.delete(tmp);
		tmp.mkdirs();
	}

	LaunchpadBuilder	builder	= new LaunchpadBuilder().runfw(org_apache_felix_framework)
		.bundles("biz.aQute.bnd.runtime.snapshot")
		.bundles(org_apache_felix_log)
		.bundles(org_apache_felix_configadmin)
		.bundles(org_apache_felix_scr)
		.bundles(org_apache_felix_gogo_runtime)
		.set("snapshot.dir", tmp.getAbsolutePath());

	@Service
	CommandProcessor	gogo;

	@Test
	public void testMinimum() throws Exception {
		try (Launchpad fw = builder.create()) {

		}
		System.out.println();

	}

	@Test
	public void testSnapshotCommand() throws Exception {
		File f1 = new File(tmp, "foo.json");
		assertThat(f1).doesNotExist();
		File f2 = new File(tmp, "snapshottest-testSnapshotCommand.json");
		assertThat(f2).doesNotExist();

		try (Launchpad fw = builder.create()
			.inject(this)) {

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);

			try (CommandSession session = gogo.createSession(in, out, out)) {
				session.execute("snapshot " + f1.getAbsoluteFile()
					.toURI()
					.getPath());
				assertThat(f1).isFile();
			}
		}

		assertThat(f2).isFile();

	}

	@Test
	public void testSnapshotDefaultName() throws Exception {
		File f = new File("generated/snapshot/snapshottest-testSnapshotDefaultName.json");
		f.delete();
		assertThat(f).doesNotExist();

		try (Launchpad fw = builder.create()) {

		}
		assertThat(f).isFile();

	}

}
