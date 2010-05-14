package bndtools.pieces;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "bndtools.pieces.messages"; //$NON-NLS-1$
	public static String ExportVersionPolicyPiece_labelLinkWithBundle;
	public static String ExportVersionPolicyPiece_labelSpecificVersion;
	public static String ExportVersionPolicyPiece_labelUnspecifiedVersion;
	public static String ExportVersionPolicyPiece_labelVersion;
	public static String ExportVersionPolicyPiece_tooltipLinkWithBundle;
	public static String ExportVersionPolicyPiece_tooltipUnspecifiedVersion;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
