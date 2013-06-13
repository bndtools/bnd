package bndtools.api;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public final class BndtoolsConstants {

	public static final String CORE_PLUGIN_ID = "bndtools.core";
    public static final String NATURE_ID = CORE_PLUGIN_ID + ".bndnature";
    public static final String BUILDER_ID = CORE_PLUGIN_ID + ".bndbuilder";

    public static final IPath BND_CLASSPATH_ID = new Path("aQute.bnd.classpath.container");

    public static final String MARKER_BND_PROBLEM = "bndtools.builder.bndproblem";
    public static final String MARKER_BND_CLASSPATH_PROBLEM = "bndtools.builder.bnd_classpath_problem";

}
