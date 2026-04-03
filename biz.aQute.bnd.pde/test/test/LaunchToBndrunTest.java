package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.IDocument;
import aQute.lib.io.IO;
import biz.aQute.bnd.pde.launch2bndrun.LaunchToBndrun;

@ExtendWith(SoftAssertionsExtension.class)
public class LaunchToBndrunTest {

	@InjectSoftAssertions
	SoftAssertions softly;

	@ParameterizedTest
	@ValueSource(ints = {
		1, 4, 10, 20
	})
	void defaultStartLevel(int startLevel) throws Exception {
		LaunchToBndrun lbr = new LaunchToBndrun(startLevel, IO.stream(Path.of("testresources/simple.launch")));

		BndEditModel model = lbr.getModel();

		List<VersionedClause> startlevels = model.getRunBundlesDecorator();

		assertThat(startlevels).as("size")
			.hasSizeGreaterThan(0);

		VersionedClause last = startlevels.get(startlevels.size() - 1);

		assertThat(last.getName()).as("name")
			.isEqualTo("*");
		List<String> startlevelAttr = last.getListAttrib(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE);
		assertThat(startlevelAttr).as("attr")
			.hasSize(1)
			.contains("" + startLevel);
	}

	@Test
	void emptyFile_throwsExeption() throws Exception {
		assertThatCode(() -> new LaunchToBndrun(0, IO.stream(""))).isInstanceOf(Exception.class);
	}

	@Test
	void nonLaunchXML_throwsParseException() throws Exception {
		final String xml = """
				<?xml version="1.0" encoding="UTF-8" standalone="no"?>
				<someotherroot/>
			""".trim();
		assertThatCode(() -> new LaunchToBndrun(0, IO.stream(xml))).hasMessageContaining("launchConfiguration");
	}

	static VersionedClause vc(String bsn) {
		return new VersionedClause(bsn);
	}

	static VersionedClause vcs(String bsn) {
		return vcRaw(bsn, "snapshot");
	}

	static VersionedClause vc(String bsn, String version) {
		return vcRaw(bsn, "[" + version + "," + version + "]");
	}

	static VersionedClause vcsl(String bsn, int startlevel) {
		Attrs attrs = new Attrs();
		attrs.put(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, startlevel + "");
		return new VersionedClause(bsn, attrs);
	}

	static VersionedClause vcRaw(String bsn, String versionRange) {
		Attrs attrs = new Attrs();
		attrs.put(Constants.VERSION_ATTRIBUTE, versionRange);
		return new VersionedClause(bsn, attrs);
	}

	@Test
	void loadsSimpleLaunch() throws Exception {
		final LaunchToBndrun lbr = new LaunchToBndrun(4, IO.stream(Path.of("testresources/simple.launch")));
		runChecks(lbr.getModel());
	}

	@Test
	void savesSimpleLaunch() throws Exception {
		final LaunchToBndrun lbr = new LaunchToBndrun(4, IO.stream(Path.of("testresources/simple.launch")));

		IDocument doc = new aQute.bnd.properties.Document(lbr.getContents());
		runChecks(new BndEditModel(doc));
	}

	@Test
	void contents_comesFromDoc() throws Exception {
		final LaunchToBndrun lbr = new LaunchToBndrun(4, IO.stream(Path.of("testresources/simple.launch")));

		softly.assertThat(lbr.getContents())
			.isEqualTo(lbr.getDoc()
				.get());
	}

	void runChecks(BndEditModel model) {

		List<VersionedClause> runBundles = model
			.getRunBundles();

		//@formatter:off
		softly.assertThat(runBundles).containsExactly(
			vcs("my.bundle.one"),
			vcs("my.bundle.two"),
			vc("my.versioned.bundle", "1.2.3"),
			vc("bcpg", "1.69.0"),
			vc("com.sun.xml", "1.2.18.v202109010034"),
			vc("slf4j.api", "1.7.30"),
			vc("slf4j.jcl"),
			vc("stax2-api")
			);
		//@formatter:on

		softly.assertThat(model.getRunBundlesDecorator())
			.as("runbundles+")
			.containsExactly(vcsl("my.bundle.two", 32), vcsl("bcpg", 1), vcsl("slf4j.jcl", 6), vcsl("*", 4));

		softly.assertThat(model.getRunVMArgs())
			.as("vmargs")
			.isEqualTo("--param1, --param2=something-other");
		softly.assertThat(model.getRunProperties())
			.as("runproperties")
			.containsEntry("prop1", "one")
			.containsEntry("prop2", "something,else");

		softly.assertThat(model.getRunProgramArgs())
			.as("programargs")
			.isEqualTo("-arg1, a, -arg2, b, -consoleLog, -console");

		softly.assertThat(model.getRunFw())
			.as("runfw")
			.isEqualTo("my.framework.bundle");

		softly.assertThat(model.getEE())
			.as("ee")
			.isEqualTo(EE.JavaSE_11);
	}
}
