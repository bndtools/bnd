package org.osgi.service.indexer.impl;

import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;

public class Util {
	public static void addVersionFilter(StringBuilder filter, VersionRange version, VersionKey key) {
		if (version.isRange()) {
			if (version.includeLow()) {
				filter.append("(").append(key.getKey());
				filter.append(">=");
				filter.append(version.getLow());
				filter.append(")");
			} else {
				filter.append("(!(").append(key.getKey());
				filter.append("<=");
				filter.append(version.getLow());
				filter.append("))");
			}

			if (version.includeHigh()) {
				filter.append("(").append(key.getKey());
				filter.append("<=");
				filter.append(version.getHigh());
				filter.append(")");
			} else {
				filter.append("(!(").append(key.getKey());
				filter.append(">=");
				filter.append(version.getHigh());
				filter.append("))");
			}
		} else {
			filter.append("(").append(key.getKey()).append(">=");
			filter.append(version);
			filter.append(")");
		}
	}
}
