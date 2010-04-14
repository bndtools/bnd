package aQute.bnd.plugin.editors;

import org.eclipse.ui.editors.text.*;

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
