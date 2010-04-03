package bndtools.editor.project;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import aQute.lib.osgi.Constants;
import bndtools.editor.PropertyTableSectionPart;
import bndtools.editor.model.BndEditModel;

public class RunPropertiesPart extends PropertyTableSectionPart {
	
	public RunPropertiesPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.RUNPROPERTIES, parent, toolkit, style);
		
		getSection().setText("Run Properties");
	}

	@Override
	protected Map<String, String> loadProperties(BndEditModel model) {
		return model.getRunProperties();
	}

	@Override
	protected void saveProperties(BndEditModel model, Map<String, String> props) {
		model.setRunProperties(props);
	}

}
