package bndtools.editor.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;

public class BndCompletionProcessor implements IContentAssistProcessor {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(?:.*\\s)*(.*)$");
    
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int offset) {
        try {
            String pre = viewer.getDocument().get(0, offset);
            Matcher matcher = PREFIX_PATTERN.matcher(pre);
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                ICompletionProposal[] found = proposals(prefix, offset);
                if (found.length == 1) {
                    found[0].apply(viewer.getDocument());
                    viewer.setSelectedRange(offset + (found[0].getDisplayString().length() - prefix.length() + 2), 0);
                    return new ICompletionProposal[0];
                }
                else {
                    return found;
                }
            }
            else {
                return proposals(null, offset);
            }
        } catch (BadLocationException e) {
            return proposals(null, offset);
        }
    }

    private ICompletionProposal[] proposals(String prefix, int offset) {
        ArrayList<ICompletionProposal> results = new ArrayList<ICompletionProposal>(Syntax.HELP.size());
        for (Syntax s : Syntax.HELP.values()) {
            if (prefix == null || s.getHeader().startsWith(prefix)) {
                IContextInformation info = new ContextInformation(s.getHeader(), s.getHeader());
                String text = prefix == null ? s.getHeader() : s.getHeader().substring(prefix.length());
                results.add(new CompletionProposal(text + ": ", offset, 0, text.length() + 2, null, s.getHeader(), info, s.getLead())); //$NON-NLS-1$                
            }
        }
        Collections.sort(results, new Comparator<ICompletionProposal>() {
            public int compare(ICompletionProposal p1, ICompletionProposal p2) {
                return p1.getDisplayString().compareTo(p2.getDisplayString());
            }            
        });
        return results.toArray(new ICompletionProposal[results.size()]);
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
