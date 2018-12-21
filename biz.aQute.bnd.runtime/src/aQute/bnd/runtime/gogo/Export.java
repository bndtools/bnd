package aQute.bnd.runtime.gogo;

import java.util.Set;
import java.util.TreeSet;

class Export {
	String		pack;
	Set<Long>	exporters	= new TreeSet<>();
	Set<Long>	privates	= new TreeSet<>();

	Export(String packageName) {
		pack = packageName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(state())
			.append(" ")
			.append(pack);

		if (!exporters.isEmpty()) {
			sb.append(" exporters=")
				.append(exporters);
		}
		if (!privates.isEmpty()) {
			sb.append(" privates=")
				.append(privates);
		}
		return sb.toString();
	}

	private String state() {
		return privates.isEmpty() ? " " : "!";
	}
}
