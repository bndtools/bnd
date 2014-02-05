package aQute.bnd.ant;

import java.util.*;

import aQute.lib.collections.*;

public class SettingsEchoHelpers {
	public static final void printProperties(Properties props) {
		List<String> keysList = new ArrayList<String>();
		int maxLength = 1;
		for (Object entry : props.keySet()) {
			String key = (String) entry;
			maxLength = Math.max(maxLength, key.length());
			keysList.add(key);
		}

		String fmt = String.format("%%-%ds = %%s\n", maxLength);

		List<String> sortedKeys = new SortedList<String>(keysList);
		for (String key : sortedKeys) {
			String value = props.getProperty(key, "<empty>");
			System.out.printf(fmt, key, value);
		}
	}
}
