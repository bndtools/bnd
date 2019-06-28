package bndtools.editor.project;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "bndtools.editor.project.messages";	//$NON-NLS-1$
	public static String		RunBundlesPart_addWizardDescription;
	public static String		RunBundlesPart_addWizardTitle;
	public static String		RunBundlesPart_description;
	public static String		RunBundlesPart_errorGettingBuilders;
	public static String		RunBundlesPart_title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
