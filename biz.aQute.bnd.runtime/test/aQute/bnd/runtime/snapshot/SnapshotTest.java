package aQute.bnd.runtime.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.lib.io.IO;

public class SnapshotTest {
	static File tmp = IO.getFile("generated/snapshot");

	@BeforeClass
	static public void before() {
		IO.delete(tmp);
		tmp.mkdirs();
		System.setProperty("snapshot.dir", tmp.getAbsolutePath());
	}

	@AfterClass
	static public void after() {
		System.getProperties()
			.remove("snapshot.dir");
	}
	// static Workspace w;
	// static {
	// try {
	// Workspace.findWorkspace(IO.work);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	LaunchpadBuilder	builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework")
		.bundles(
			"biz.aQute.bnd.runtime.snapshot, org.apache.felix.log, org.apache.felix.configadmin, org.apache.felix.scr, org.apache.felix.gogo.runtime");

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
