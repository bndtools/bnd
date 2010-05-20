package bndtools.editor.contents;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "bndtools.editor.contents.messages"; //$NON-NLS-1$
    public static String BundleCalculatedImportsPart_columnAttribs;
    public static String BundleCalculatedImportsPart_columnPackage;
    public static String BundleCalculatedImportsPart_description;
    public static String BundleCalculatedImportsPart_error;
    public static String BundleCalculatedImportsPart_errorFindingType;
    public static String BundleCalculatedImportsPart_errorOpeningClass;
    public static String BundleCalculatedImportsPart_errorOpeningJavaEditor;
    public static String BundleCalculatedImportsPart_jobAnalyse;
    public static String BundleCalculatedImportsPart_title;
    public static String BundleCalculatedImportsPart_tooltipShowSelfImports;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
