package bndtools.editor.model;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import aQute.lib.properties.BadLocationException;

public class IDocumentWrapper implements aQute.lib.properties.IDocument {

    private final IDocument document;

    public IDocumentWrapper(IDocument document) {
        this.document = document;
    }

    public int getNumberOfLines() {
        return document.getNumberOfLines();
    }

    public aQute.lib.properties.IRegion getLineInformation(int lineNum) throws aQute.lib.properties.BadLocationException {
        try {
            return new IRegionWrapper(document.getLineInformation(lineNum));
        } catch (org.eclipse.jface.text.BadLocationException e) {
            BadLocationException ex = new BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public String get() {
        return document.get();
    }

    public String get(int offset, int length) throws aQute.lib.properties.BadLocationException {
        try {
            return document.get(offset, length);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            BadLocationException ex = new BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public String getLineDelimiter(int line) throws aQute.lib.properties.BadLocationException {
        try {
            return document.getLineDelimiter(line);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            BadLocationException ex = new BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public int getLength() {
        return document.getLength();
    }

    public void replace(int offset, int length, String data) throws aQute.lib.properties.BadLocationException {
        try {
            document.replace(offset, length, data);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            BadLocationException ex = new BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public char getChar(int offset) throws aQute.lib.properties.BadLocationException {
        try {
            return document.getChar(offset);
        } catch (org.eclipse.jface.text.BadLocationException e) {
            BadLocationException ex = new BadLocationException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public class IRegionWrapper implements aQute.lib.properties.IRegion {

        private final IRegion region;

        public IRegionWrapper(IRegion region) {
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
