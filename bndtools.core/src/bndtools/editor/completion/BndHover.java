package bndtools.editor.completion;

import org.eclipse.jface.text.*;

public class BndHover implements ITextHover {
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        if (hoverRegion != null) {
            IDocument doc = textViewer.getDocument();
            try {
                String key = doc.get(hoverRegion.getOffset(), hoverRegion.getLength());

                Syntax syntax = Syntax.HELP.get(key);
                if (syntax == null)
                    return null;

                StringBuilder sb = new StringBuilder();
                sb.append(syntax.getLead());
                sb.append("\nE.g. ");
                sb.append(syntax.getExample());

                String text = sb.toString();
                if (text == null)
                    return null;

                if (text.length() > 30) {
                    text = wrap(text, 30);
                }
                return text;
            } catch (Exception e) {
                return e + "";
            }
        }
        return null;
    }

    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        IDocument doc = textViewer.getDocument();
        try {
            int start = offset;
            int end = offset;
            while (start >= 0 && isWordChar(doc.getChar(start)))
                start--;

            while (end < doc.getLength() && isWordChar(doc.getChar(end)))
                end++;

            start++;
            int length = Math.min(doc.getLength(), end - start);
            start = Math.max(0, start);
            return new Region(start, length);
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    static boolean isWordChar(char c) {
        return Character.isJavaIdentifierPart(c) || c == '-' || c == '.';
    }

    static String wrap(String text, int width) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        int r = 0;
        while (r < text.length()) {
            char c = text.charAt(r++);
            switch (c) {
            case '\r' :
            case '\n' :
                if (n != 0)
                    sb.append('\n');
                n = 0;
                break;
            case ' ' :
            case '\t' :
                if (n > width) {
                    sb.append("\n");
                    n = 0;
                } else {
                    sb.append(" ");
                    n++;
                }
                break;
            default :
                sb.append(c);
                n++;
            }
        }
        return sb.toString();
    }

}
