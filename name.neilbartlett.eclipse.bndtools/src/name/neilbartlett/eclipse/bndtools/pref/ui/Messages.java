package name.neilbartlett.eclipse.bndtools.pref.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "name.neilbartlett.eclipse.bndtools.pref.ui.messages"; //$NON-NLS-1$
	public static String BndPreferencePage_optionAlwaysEnable;
	public static String BndPreferencePage_optionNeverEnable;
	public static String BndPreferencePage_optionPrompt;
	public static String BndPreferencePage_titleSubBundles;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
