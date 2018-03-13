package aQute.bnd.maven;

import java.util.Map;

import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public class MavenGroup implements BsnToMavenPath, Plugin {
	String groupId = "";

	@Override
	public String[] getGroupAndArtifact(String bsn) {
		String[] result = new String[2];
		result[0] = groupId;
		result[1] = bsn;
		return result;
	}

	@Override
	public void setProperties(Map<String, String> map) {
		if (map.containsKey("groupId")) {
			groupId = map.get("groupId");
		}
	}

	@Override
	public void setReporter(Reporter processor) {}

}
