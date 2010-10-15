package bndtools.wizards.workspace;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "bndtools.wizards.workspace.messages"; //$NON-NLS-1$

    public static String CnfSetupCreate;
	public static String CnfSetupCreateTitle;
    public static String CnfSetupCreateExplanation;
	public static String CnfSetupCreateSkip;

	public static String CnfSetupUpdate;
	public static String CnfSetupUpdateTitle;
	public static String CnfSetupUpdateExplanation;
	public static String CnfSetupUpdateSkip;

	public static String CnfSetupNever;

    public static String CnfSetupNeverWarning;

    public static String CnfSetupNeverWarningTitle;
    public static String DontShowMessageAgain;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
