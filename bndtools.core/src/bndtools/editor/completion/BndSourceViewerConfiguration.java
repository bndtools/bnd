package bndtools.editor.completion;

import java.util.*;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

public class BndSourceViewerConfiguration extends SourceViewerConfiguration {

    Token T_DEFAULT;
    Token T_MACRO;
    Token T_ERROR;
    Token T_COMMENT;
    Token T_INSTRUCTION;
    Token T_OPTION;
    Token T_DIRECTIVE;

    static final String SINGLELINE_COMMENT_TYPE = "___slc";
    static Properties syntax = null;

    BndScanner scanner;
    MultiLineCommentScanner multiLineCommentScanner;

    public BndSourceViewerConfiguration(ISharedTextColors colors) {
        T_DEFAULT = new Token(new TextAttribute(colors.getColor(new RGB(0, 0, 0))));
        T_MACRO = new Token(new TextAttribute(colors.getColor(new RGB(0, 255, 0)), null, SWT.BOLD));
        T_ERROR = new Token(new TextAttribute(colors.getColor(new RGB(255, 0, 0)), null, SWT.BOLD));
        T_COMMENT = new Token(new TextAttribute(colors.getColor(new RGB(128, 0, 0))));
        T_INSTRUCTION = new Token(new TextAttribute(colors.getColor(new RGB(0, 0, 255)), null, SWT.BOLD));
        T_OPTION = new Token(new TextAttribute(colors.getColor(new RGB(0, 0, 255))));
        T_DIRECTIVE = new Token(new TextAttribute(colors.getColor(new RGB(60, 60, 255)), null, SWT.BOLD));
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();
        configureReconciler(reconciler, IDocument.DEFAULT_CONTENT_TYPE, getBndScanner());
        configureReconciler(reconciler, SINGLELINE_COMMENT_TYPE, getMultiLineCommentScanner());
        return reconciler;
    }

    private static void configureReconciler(PresentationReconciler reconciler, String partitionType, ITokenScanner scanner) {
        DefaultDamagerRepairer dr;
        dr = new DefaultDamagerRepairer(scanner);
        reconciler.setDamager(dr, partitionType);
        reconciler.setRepairer(dr, partitionType);
    }

    protected BndScanner getBndScanner() {
        if (scanner == null) {
            scanner = new BndScanner(this);
        }
        return scanner;
    }

    class MultiLineCommentScanner extends RuleBasedScanner {

        public MultiLineCommentScanner() {
            setDefaultReturnToken(T_COMMENT);
        }

    }

    protected MultiLineCommentScanner getMultiLineCommentScanner() {
        if (multiLineCommentScanner == null) {
            multiLineCommentScanner = new MultiLineCommentScanner();
        }
        return multiLineCommentScanner;
    }

    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) || SINGLELINE_COMMENT_TYPE.equals(contentType)) {
            return new String[] {
                    "#", "//"
            };
        }
        return null;
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new BndHover();
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer viewer) {
        ContentAssistant assistant = new ContentAssistant();
        assistant.setContentAssistProcessor(new BndCompletionProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setContentAssistProcessor(new BndCompletionProcessor(), SINGLELINE_COMMENT_TYPE);
        assistant.enableAutoActivation(true);
        return assistant;
    }
}