package org.bndtools.core.resolve;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "org.bndtools.core.resolve.messages";				//$NON-NLS-1$
	public static String		ResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified;
	public static String		ResolutionJob_jobName;
	public static String		ResolveOperation_errorAddingPackageCaps;
	public static String		ResolveOperation_errorFindingFramework;
	public static String		ResolveOperation_errorGettingBuilders;
	public static String		ResolveOperation_errorLoadingIndexes;
	public static String		ResolveOperation_errorOverview;
	public static String		ResolveOperation_errorProcessingIndex;
	public static String		ResolveOperation_errorReadingBundle;
	public static String		ResolveOperation_errorReadingSystemBundleManifest;
	public static String		ResolveOperation_invalidHeaderFormat;
	public static String		ResolveOperation_invalidRunFile;
	public static String		ResolveOperation_missingFramework;
	public static String		ResolveOperation_missingJrePackageDefinition;
	public static String		ResolveOperation_progressLabel;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
