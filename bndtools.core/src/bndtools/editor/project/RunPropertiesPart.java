package bndtools.editor.project;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.editor.common.PropertyTableSectionPart;

public class RunPropertiesPart extends PropertyTableSectionPart {

    public RunPropertiesPart(Composite parent, FormToolkit toolkit, int style) {
        super(Constants.RUNPROPERTIES, parent, toolkit, style);

        getSection().setText("Runtime Properties");
        getSection().setDescription("These properties will be supplied as configuration to the OSGi framework, and may be used by the framework or any bundle.");
    }

    @Override
    protected Map<String,String> loadProperties(BndEditModel model) {
        return model.getRunProperties();
    }

    @Override
    protected void saveProperties(BndEditModel model, Map<String,String> props) {
        model.setRunProperties(props);
    }
}
