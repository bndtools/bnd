package bndtools.editor.components;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "bndtools.editor.components.messages"; //$NON-NLS-1$
    public static String ComponentListPart_addButton;
    public static String ComponentListPart_errorJavaType;
    public static String ComponentListPart_fixAddToExportedPkgs;
    public static String ComponentListPart_fixAddToPrivatePkgs;
    public static String ComponentListPart_listSectionTitle;
    public static String ComponentListPart_RemoveButton;
    public static String ComponentListPart_warningDefaultPkg;
    public static String ComponentListPart_warningPkgNotIncluded;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
