package bndtools.launch.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "bndtools.launch.ui.messages"; //$NON-NLS-1$
    public static String JUnitTestParamsLaunchTabPiece_descStartingTimeout;
    public static String JUnitTestParamsLaunchTabPiece_errorTimeoutValue;
    public static String JUnitTestParamsLaunchTabPiece_labelKeepAlive;
    public static String JUnitTestParamsLaunchTabPiece_labelStartingTimeout;
    public static String JUnitTestParamsLaunchTabPiece_title;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
