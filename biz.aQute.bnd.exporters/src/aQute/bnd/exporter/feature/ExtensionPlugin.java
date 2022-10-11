package aQute.bnd.exporter.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureExtension;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "FeatureExtensionPlugin")
public class ExtensionPlugin implements Plugin {

	private Map<String, String> map;

	@Override
	public void setProperties(Map<String, String> map) throws Exception {

		this.map = map;

	}

	@Override
	public void setReporter(Reporter processor) {

	}

	public FeatureExtension toExtension() {

		String sType = map.getOrDefault("type", FeatureExtension.Type.ARTIFACTS.toString());
		String sState = map.getOrDefault("state", FeatureExtension.Kind.OPTIONAL.toString());
		String sName = map.getOrDefault("name", "unknmown");
		String sText = map.getOrDefault("text", "");
		final List<String> sList = new ArrayList<>();
		FeatureExtension.Type type = FeatureExtension.Type.valueOf(sType);
		FeatureExtension.Kind state = FeatureExtension.Kind.valueOf(sState);

		if (FeatureExtension.Type.JSON.equals(type)) {} else if (FeatureExtension.Type.TEXT.equals(type)) {
			Arrays.asList(map.getOrDefault("text", "")
				.split(System.lineSeparator()))
				.forEach(v -> sList.add(v));
		}

		return new FeatureExtension() {

			@Override
			public String getName() {
				return sName;
			}

			@Override
			public Type getType() {
				return Type.valueOf(sType);
			}

			@Override
			public Kind getKind() {
				return Kind.valueOf(sState);
			}

			@Override
			public String getJSON() {
				return sText;
			}

			@Override
			public List<String> getText() {
				return sList;
			}

			@Override
			public List<FeatureArtifact> getArtifacts() {
				return null;
			}
		};
	}

}
