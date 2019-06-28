package bndtools.preferences.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "bndtools.preferences.ui.messages";		//$NON-NLS-1$
	public static String		BndPreferencePage_btnBuildBeforeLaunch;
	public static String		BndPreferencePage_miscGroup;
	public static String		BndPreferencePage_btnNoAskPackageInfo;
	public static String		BndPreferencePage_btnAliasRequirements;
	public static String		BndPreferencePage_cmbBuildLogging_None;
	public static String		BndPreferencePage_cmbBuildLogging_Basic;
	public static String		BndPreferencePage_cmbBuildLogging_Full;
	public static String		BndPreferencePage_editorGroup;
	public static String		BndPreferencePage_headlessGroup;
	public static String		BndPreferencePage_headlessCreate_text;
	public static String		BndPreferencePage_versionControlIgnoresGroup_text;
	public static String		BndPreferencePage_versionControlIgnoresCreate_text;
	public static String		BndPreferencePage_btnEditorOpenSourceTab;
	public static String		BndPreferencePage_btnCheckCnfNow_BndConf;
	public static String		BndPreferencePage_btnCheckCnfNow_Exists;
	public static String		BndPreferencePage_optionAlwaysEnable;
	public static String		BndPreferencePage_optionNeverEnable;
	public static String		BndPreferencePage_optionPrompt;
	public static String		BndPreferencePage_titleSubBundles;
	public static String		BndPreferencePage_lblBuildLogging_text;
	public static String		BndPreferencePage_grpLaunching_text;
	public static String		BndPreferencePage_btnWarnExistingLaunch;
	public static String		BndPreferencePage_msgCheckValidHeadless;
	public static String		BndPreferencePage_msgCheckValidVersionControlIgnores;
	public static String		BndPreferencePage_namedPluginDeprecated_text;
	public static String		BndPreferencePage_btnOfflineWorkspace;
	public static String		BndPreferencePage_decorOfflineWorkspace;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
