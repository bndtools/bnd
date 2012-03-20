package bndtools.preferences.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "bndtools.preferences.ui.messages"; //$NON-NLS-1$
	public static String BndPreferencePage_optionAlwaysEnable;
	public static String BndPreferencePage_optionNeverEnable;
	public static String BndPreferencePage_optionPrompt;
	public static String BndPreferencePage_titleSubBundles;
	public static String BndPreferencePage_grpDebugging_text;
	public static String BndPreferencePage_lblBuildLogging_text;
	public static String BndPreferencePage_grpLaunching_text;
	public static String BndPreferencePage_btnWarnExistingLaunch;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
