package bndtools.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.texteditor.templates.TemplatesView;

import bndtools.PartConstants;

public class BndPerspective implements IPerspectiveFactory {

    public static final String ID_PROJECT_EXPLORER = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

    public static final String VIEW_ID_JUNIT_RESULTS = "org.eclipse.jdt.junit.ResultView";
    private static final String VIEW_ID_CONSOLE = "org.eclipse.ui.console.ConsoleView";
    private static final String VIEW_ID_SEARCH = "org.eclipse.search.ui.views.SearchView";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();

        IFolderLayout leftFolder = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea);
        leftFolder.addView(JavaUI.ID_PACKAGES);
        leftFolder.addView(JavaUI.ID_TYPE_HIERARCHY);

        layout.addView(PartConstants.VIEW_ID_REPOSITORIES, IPageLayout.BOTTOM, 0.66f, "left");

        IFolderLayout outputFolder = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.75f, editorArea);
        outputFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        outputFolder.addView(JavaUI.ID_JAVADOC_VIEW);
        outputFolder.addView(PartConstants.VIEW_ID_IMPORTSEXPORTS);
        outputFolder.addPlaceholder(VIEW_ID_SEARCH);
        outputFolder.addPlaceholder(VIEW_ID_CONSOLE);
        outputFolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
        outputFolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
        outputFolder.addPlaceholder(VIEW_ID_JUNIT_RESULTS);

        IFolderLayout outlineFolder = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.75, editorArea); //$NON-NLS-1$
        outlineFolder.addView(IPageLayout.ID_OUTLINE);
        outlineFolder.addPlaceholder(TemplatesView.ID);

        layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
        layout.addActionSet(JavaUI.ID_ACTION_SET);
        layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
        layout.addActionSet("bndtools.actions");
        layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

        // views - java
        layout.addShowViewShortcut(VIEW_ID_JUNIT_RESULTS);
        layout.addShowViewShortcut(JavaUI.ID_PACKAGES);
        layout.addShowViewShortcut(JavaUI.ID_TYPE_HIERARCHY);
        layout.addShowViewShortcut(JavaUI.ID_SOURCE_VIEW);
        layout.addShowViewShortcut(JavaUI.ID_JAVADOC_VIEW);

        // views - search
        layout.addShowViewShortcut(VIEW_ID_SEARCH);

        // views - debugging
        layout.addShowViewShortcut(VIEW_ID_CONSOLE);

        // views - standard workbench
        layout.addShowViewShortcut(PartConstants.VIEW_ID_IMPORTSEXPORTS);
        layout.addShowViewShortcut(PartConstants.VIEW_ID_REPOSITORIES);
        layout.addShowViewShortcut(PartConstants.VIEW_ID_JPM);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
        layout.addShowViewShortcut(ID_PROJECT_EXPLORER);

        // new actions - Java project creation wizard
        layout.addNewWizardShortcut(PartConstants.WIZARD_ID_NEWPROJECT);
        layout.addNewWizardShortcut(PartConstants.WIZARD_ID_NEWWORKSPACE);
        layout.addNewWizardShortcut(PartConstants.WIZARD_ID_NEWBNDRUN);
        layout.addNewWizardShortcut(PartConstants.WIZARD_ID_NEWWRAPPROJECT);
        layout.addNewWizardShortcut(PartConstants.WIZARD_ID_NEWBND);
        layout.addNewWizardShortcut(PartConstants.WIZARD_ID_NEWBLUEPRINT_XML);
        layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard"); //$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewClassCreationWizard"); //$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewEnumCreationWizard"); //$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewAnnotationCreationWizard"); //$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard");//$NON-NLS-1$

        layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective");
        layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective");
    }
}