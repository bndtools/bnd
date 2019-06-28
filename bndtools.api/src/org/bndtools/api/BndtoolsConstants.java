package org.bndtools.api;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import aQute.service.reporter.Report.Location;

public final class BndtoolsConstants {

	public static final String	CORE_PLUGIN_ID					= "bndtools.core";
	public static final String	NATURE_ID						= CORE_PLUGIN_ID + ".bndnature";
	public static final String	BUILDER_ID						= CORE_PLUGIN_ID + ".bndbuilder";

	public static final IPath	BND_CLASSPATH_ID				= new Path("aQute.bnd.classpath.container");

	public static final String	MARKER_BND_PROBLEM				= "bndtools.builder.bndproblem";
	public static final String	MARKER_BND_PATH_PROBLEM			= "bndtools.builder.bndpathproblem";
	public static final String	MARKER_COMPONENT				= "bndtools.builder.componentmarker";
	public static final String	MARKER_BND_WORKSPACE_PROBLEM	= "bndtools.builder.bndworkspaceproblem";
	public static final String	MARKER_BND_MISSING_WORKSPACE	= "bndtools.builder.missingworkspace";
	public static final String	MARKER_JAVA_BASELINE			= "bndtools.builder.packageInfoBaseline";

	/**
	 * Marker attribute name for the bnd {@link Location#context}
	 */
	public static final String	BNDTOOLS_MARKER_CONTEXT_ATTR	= "bndtools.marker.context";
	/**
	 * Marker attribute name for the bnd {@link Location#header}
	 */
	public static final String	BNDTOOLS_MARKER_HEADER_ATTR		= "bndtools.marker.header";

	/**
	 * Marker attribute name for the bnd {@link Location#reference}
	 */
	public static final String	BNDTOOLS_MARKER_REFERENCE_ATTR	= "bndtools.marker.reference";

	/**
	 * Marker attribute name for the bnd {@link Location#file}
	 */
	public static final String	BNDTOOLS_MARKER_FILE_ATTR		= "bndtools.marker.file";

}
