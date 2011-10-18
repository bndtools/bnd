package org.bndtools.core.utils.filters;

import static org.bndtools.core.utils.filters.ObrConstants.FILTER_BSN;
import static org.bndtools.core.utils.filters.ObrConstants.FILTER_VERSION;
import aQute.libg.version.VersionRange;

public class ObrFilterUtil {

    public static void appendVersionFilter(StringBuilder filter, VersionRange version) {
        if (version != null) {
            if (version.isRange()) {
                if (version.includeLow()) {
                    filter.append("(").append(FILTER_VERSION);
                    filter.append(">=");
                    filter.append(version.getLow());
                    filter.append(")");
                }
                else {
                    filter.append("(!(").append(FILTER_VERSION);
                    filter.append("<=");
                    filter.append(version.getLow());
                    filter.append("))");
                }

                if ( version.includeHigh() ) {
                    filter.append("(").append(FILTER_VERSION);
                    filter.append("<=");
                    filter.append(version.getHigh());
                    filter.append(")");
                }
                else {
                    filter.append("(!(").append(FILTER_VERSION);
                    filter.append(">=");
                    filter.append(version.getHigh());
                    filter.append("))");
                }
            } else {
                filter.append("(").append(FILTER_VERSION);
                filter.append(">=");
                filter.append(version);
                filter.append(")");
            }
        }
    }

    public static void appendBsnFilter(StringBuilder filter, String bsn) {
        filter.append("(");
        filter.append(FILTER_BSN).append("=").append(bsn);
        filter.append(")");
    }
}
