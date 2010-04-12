package aQute.bnd.jareditor;

import org.eclipse.ui.texteditor.*;

public class JarEditor extends AbstractTextEditor {
	public JarEditor() {
	      //install the source configuration
	      setSourceViewerConfiguration(new JarConfiguration());
	      //install the document provider
	      setDocumentProvider(new JarDocumentProvider());
	   }
	   protected void createActions() {
	      super.createActions();
	      //... add other editor actions here
	   }

}
