package aQute.bnd.plugin.editors;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;

import aQute.bnd.help.*;

public class BndCompletionProcessor implements IContentAssistProcessor {

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int offset) {
        ICompletionProposal[] result= new ICompletionProposal[Syntax.HELP.size()];
            int i=0;
        for ( Syntax s : Syntax.HELP.values()) {
            IContextInformation info= new ContextInformation(
                    s.getHeader(), s.getHeader());
            result[i++]= new CompletionProposal(s.getHeader()+": ", offset-1, 1, s.getHeader().length()+2, null, s.getHeader(), info, s.getLead()); //$NON-NLS-1$
        }
        return result;
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] {'-'};
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return new char[] {'-'};
    }

    public IContextInformationValidator getContextInformationValidator() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

}
