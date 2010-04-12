package aQute.bnd.jareditor;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.texteditor.*;

public class JarDocumentProvider implements IDocumentProvider {
	Document	doc;
	PrintWriter out;
	
	public void aboutToChange(Object arg0) {
		// TODO Auto-generated method stub

	}

	public void addElementStateListener(IElementStateListener arg0) {
		// TODO Auto-generated method stub

	}

	public boolean canSaveDocument(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void changed(Object arg0) {
		// TODO Auto-generated method stub

	}

	public void connect(Object source) throws CoreException {
		try {
			if (source instanceof FileEditorInput) {
				FileEditorInput input = (FileEditorInput) source;
				File file = input.getPath().toFile();
				doc = new Document(print(file));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void disconnect(Object arg0) {
		// TODO Auto-generated method stub

	}

	public IAnnotationModel getAnnotationModel(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IDocument getDocument(Object arg0) {
		return doc;
	}

	public long getModificationStamp(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getSynchronizationStamp(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isDeleted(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean mustSaveDocument(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeElementStateListener(IElementStateListener arg0) {
		// TODO Auto-generated method stub

	}

	public void resetDocument(Object arg0) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void saveDocument(IProgressMonitor arg0, Object arg1,
			IDocument arg2, boolean arg3) throws CoreException {
		// TODO Auto-generated method stub

	}
	
	
	String print(File file) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);
		aQute.bnd.main.bnd x = new aQute.bnd.main.bnd();
		x.setOut(ps);
		x.doPrint(file.getAbsolutePath(), -1);
		ps.close();
		return new String( bos.toByteArray());
	}

}
