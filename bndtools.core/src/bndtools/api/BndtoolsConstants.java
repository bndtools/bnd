package bndtools.api;

import org.eclipse.core.runtime.Path;

import bndtools.Plugin;

public final class BndtoolsConstants {

    public static final String NATURE_ID = Plugin.PLUGIN_ID + ".bndnature";

    public static final Path BND_CLASSPATH_ID = new Path("aQute.bnd.classpath.container");

    public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";
    public static final String MARKER_BND_CLASSPATH_PROBLEM = Plugin.PLUGIN_ID + ".bnd_classpath_problem";

}
