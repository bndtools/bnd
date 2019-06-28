package org.bndtools.core.ui.wizards.ds;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "org.bndtools.core.ui.wizards.ds.messages";	//$NON-NLS-1$

	public static String		NewDSComponentWizard_title;
	public static String		NewDSComponentWizardPage_title;
	public static String		NewDSComponentWizardPage_description;
	public static String		NewDSComponentWizardPage_LC_label;
	public static String		NewDSComponentWizardPage_LC_labelNoActivate;
	public static String		NewDSComponentWizardPage_LC_labelNoArg;
	public static String		NewDSComponentWizardPage_LC_labelConfigMap;
	public static String		NewDSComponentWizardPage_LC_labelComponentContext;
	public static String		NewDSComponentWizardPage_LC_labelBundleContext;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
