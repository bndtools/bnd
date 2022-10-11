package aQute.bnd.exporter.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.unmodifiable.Lists;
import aQute.bnd.unmodifiable.Maps;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

public class FeatureExporterTest {
	private static final String	proj_bundle_1			= "proj_bundle_1";
	private static final String	proj_bundle_2			= "proj_bundle_2";

	private static final String	WS_PATH					= "test-ws/";

	private static final String	EXPORT_PRJ_FLD_SEG		= "proj_export";

	private String				genWsPath;

	@Test
	public void testBase(@InjectTemporaryDirectory
	String tmp) throws Exception {
		Map<Object, Object> jsonText = process(tmp, "FeatureDefaults");

		Assertions.assertThat(jsonText)
			.containsEntry("id", "TheGroupId:TheSymbolicName:0.0.0-INITIAL")
			.containsEntry("name", "TheName")
			.containsEntry("description", "TheDescription")
			.containsEntry("vendor", "TheVendor")
			.containsEntry("license", "TheLicense")
			.containsEntry("categories", Lists.of("TheCategory"))
			.containsEntry("docurl", "TheDocUrl")
			.containsEntry("bundles", Lists.of("test-ws:proj_bundle_1:0.0.1", "test-ws:proj_bundle_2:0.0.1"))
			.containsEntry("variables", Maps.of("voo", "doo", "foo", "bar"));

	}

	private Map<Object, Object> process(String tmpDir, String bndrun) throws IOException, Exception {
		genWsPath = tmpDir + "/" + WS_PATH;
		final File wsRoot = IO.getFile(genWsPath);
		IO.delete(wsRoot);
		IO.copy(IO.getFile(WS_PATH), wsRoot);
		try (Workspace ws = new Workspace(wsRoot)) {
			ws.addBasicPlugin(new FeatureExporter());
			assertThat(ws.check()).isTrue();
			Project p1 = ws.getProject(proj_bundle_1);
			assertThat(p1).isNotNull();
			assertThat(p1.build()).hasSize(1);

			Project p2 = ws.getProject(proj_bundle_2);
			assertThat(p2).isNotNull();
			assertThat(p2.build()).hasSize(1);

			File fsubs = IO.getFile(genWsPath + "/" + EXPORT_PRJ_FLD_SEG + "/" + bndrun + ".bndrun");
			assertThat(fsubs).exists();

			try (Run subsys = Run.createRun(ws, fsubs)) {
				Map.Entry<String, Resource> featureMap = subsys.export(FeatureExporter.TYPE_OSGI_FEATURE, null);
				try (FileResource featureResource = (FileResource) featureMap.getValue()) {
					return new JSONCodec().dec()
						.from(featureResource.getFile()).get(Map.class);
				}
			}
		}
	}

}
