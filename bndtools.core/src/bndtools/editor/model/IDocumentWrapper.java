package bndtools.editor.model;

public class IDocumentWrapper implements aQute.bnd.properties.IDocument {

    private final org.eclipse.jface.text.IDocument document;

    public IDocumentWrapper(org.eclipse.jface.text.IDocument document) {
        this.document = document;
    }

    public int getNumberOfLines() {
        return document.getNumberOfLines();
    }

    public aQute.bnd.properties.IRegion getLineInformation(int lineNum) throws aQute.bnd.properties.BadLocationException {
        try {
            return new IRegionWrapper(document.getLineInformation(lineNum));
        } catch (org.eclipse.jface.text.BadLocationException e) {
            aQute.bnd.properties.BadLocationException ex = new aQute.bnd.properties.BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public String get() {
        return document.get();
    }

    public String get(int offset, int length) throws aQute.bnd.properties.BadLocationException {
        try {
            return document.get(offset, length);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            aQute.bnd.properties.BadLocationException ex = new aQute.bnd.properties.BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public String getLineDelimiter(int line) throws aQute.bnd.properties.BadLocationException {
        try {
            return document.getLineDelimiter(line);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            aQute.bnd.properties.BadLocationException ex = new aQute.bnd.properties.BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public int getLength() {
        return document.getLength();
    }

    public void replace(int offset, int length, String data) throws aQute.bnd.properties.BadLocationException {
        try {
            document.replace(offset, length, data);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            aQute.bnd.properties.BadLocationException ex = new aQute.bnd.properties.BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public char getChar(int offset) throws aQute.bnd.properties.BadLocationException {
        try {
            return document.getChar(offset);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            aQute.bnd.properties.BadLocationException ex = new aQute.bnd.properties.BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public static class IRegionWrapper implements aQute.bnd.properties.IRegion {

        private final org.eclipse.jface.text.IRegion region;

        public IRegionWrapper(org.eclipse.jface.text.IRegion region) {
            this.region = region;
        }

        public int getLength() {
            return region.getLength();
        }

        public int getOffset() {
            return region.getOffset();
        }
    }
}
