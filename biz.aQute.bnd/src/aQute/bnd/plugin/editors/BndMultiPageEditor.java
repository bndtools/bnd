package aQute.bnd.plugin.editors;

import java.io.*;
import java.text.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;

/**
 */
public class BndMultiPageEditor extends MultiPageEditorPart implements
        IResourceChangeListener {

    private Table table;
    /** The text editor used in page 0. */
    private TextEditor editor;


    /** The text widget used in page 2. */
    private StyledText text;

    /**
     * Creates a multi-page editor example.
     */
    public BndMultiPageEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    /**
     * Creates page 0 of the multi-page editor, which contains a text editor.
     */
    void createTextEditor() {
        try {
            editor = new BndTextEditor();

            int index = addPage(editor, getEditorInput());
            setPageText(index, editor.getTitle());
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                    "Error creating nested text editor", null, e.getStatus());
        }
    }

    /**
     * Creates page 1 of the multi-page editor, which allows you to change the
     * font used in page 2.
     */
    void createPage1() {

        Composite composite = new Composite(getContainer(), SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);

        int index = addPage(composite);

        final TableViewer tableViewer = new TableViewer(composite, SWT.BORDER);
        table = tableViewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        tableColumn.setWidth(100);
        tableColumn.setText("Key");

        final TableColumn tableColumn_1 = new TableColumn(table, SWT.NONE);
        tableColumn_1.setWidth(1000);
        tableColumn_1.setText("Value");
        setPageText(index, "Properties");
    }

    /**
     * Creates page 2 of the multi-page editor, which shows the sorted text.
     */
    void createPage2() {
        Composite composite = new Composite(getContainer(), SWT.NONE);
        FillLayout layout = new FillLayout();
        composite.setLayout(layout);
        text = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
        text.setEditable(false);

        int index = addPage(composite);
        setPageText(index, "Preview");
    }

    
    
    
    void createPage3() {
        
    }
    /**
     * Creates the pages of the multi-page editor.
     */
    protected void createPages() {
        createTextEditor();
        createPage1();
        createPage2();
    }

    /**
     * The <code>MultiPageEditorPart</code> implementation of this
     * <code>IWorkbenchPart</code> method disposes all nested editors.
     * Subclasses may extend.
     */
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    /**
     * Saves the multi-page editor's document.
     */
    public void doSave(IProgressMonitor monitor) {
        getEditor(0).doSave(monitor);
    }

    /**
     * Saves the multi-page editor's document as another file. Also updates the
     * text for page 0's tab, and updates this multi-page editor's input to
     * correspond to the nested editor's.
     */
    public void doSaveAs() {
        IEditorPart editor = getEditor(0);
        editor.doSaveAs();
        setPageText(0, editor.getTitle());
        setInput(editor.getEditorInput());
    }

    public void setInput(IEditorInput iei ) {
	    super.setInput(iei);
	    setPartName( iei.getName());
	    setContentDescription(iei.getToolTipText());
	    if ( iei instanceof IFileEditorInput ) {
	        IFileEditorInput in = (IFileEditorInput) iei;
	        IProject project = in.getFile().getProject();
	        setPartName(project.getName());
	    }
	}

    /*
     * (non-Javadoc) Method declared on IEditorPart
     */
    public void gotoMarker(IMarker marker) {
        setActivePage(0);
        IDE.gotoMarker(getEditor(0), marker);
    }

    /**
     * The <code>MultiPageEditorExample</code> implementation of this method
     * checks that the input is an instance of <code>IFileEditorInput</code>.
     */
    public void init(IEditorSite site, IEditorInput editorInput)
            throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput))
            throw new PartInitException(
                    "Invalid Input: Must be IFileEditorInput");
        super.init(site, editorInput);
    }

    /*
     * (non-Javadoc) Method declared on IEditorPart.
     */
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Calculates the contents of page 2 when the it is activated.
     */
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        if (newPageIndex == 2) {
            sortWords();
        }
    }

    /**
     * Closes all project files on project close.
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow()
                            .getPages();
                    for (int i = 0; i < pages.length; i++) {
                        if (((FileEditorInput) editor.getEditorInput())
                                .getFile().getProject().equals(
                                        event.getResource())) {
                            IEditorPart editorPart = pages[i].findEditor(editor
                                    .getEditorInput());
                            pages[i].closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }


    /**
     * Sorts the words in page 0, and shows them in page 2.
     */
    void sortWords() {

        String editorText = editor.getDocumentProvider().getDocument(
                editor.getEditorInput()).get();

        StringTokenizer tokenizer = new StringTokenizer(editorText,
                " \t\n\r\f!@#\u0024%^&*()-_=+`~[]{};:'\",.<>/?|\\");
        ArrayList<String> editorWords = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            editorWords.add(tokenizer.nextToken());
        }

        Collections.sort(editorWords, Collator.getInstance());
        StringWriter displayText = new StringWriter();
        for (int i = 0; i < editorWords.size(); i++) {
            displayText.write(((String) editorWords.get(i)));
            displayText.write(System.getProperty("line.separator"));
        }
        text.setText(displayText.toString());
    }
}
