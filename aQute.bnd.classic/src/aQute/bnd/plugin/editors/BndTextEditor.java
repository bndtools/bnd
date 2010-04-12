package aQute.bnd.plugin.editors;

import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.texteditor.*;

public class BndTextEditor extends TextEditor {
    
	BndTextEditor() {
		setSourceViewerConfiguration(new BndSourceViewerConfiguration(getSharedColors()));
	}
	
	protected void createActions() {
        super.createActions();
        
//        IAction contentAssistAction = new ContentAssistAction(null, "ContentAssistProposal.", this); //$NON-NLS-1$
//        contentAssistAction.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
//        setAction("ContentAssistProposal", contentAssistAction);
//        markAsStateDependentAction("ContentAssistProposal", true);
	}
}
