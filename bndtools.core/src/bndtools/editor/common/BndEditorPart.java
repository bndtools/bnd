package bndtools.editor.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;

import aQute.bnd.build.model.BndEditModel;

public abstract class BndEditorPart extends SectionPart implements PropertyChangeListener {

    protected BndEditModel model;

    private List<String> subscribedProps = new LinkedList<String>();

    public BndEditorPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
    }

    protected abstract String[] getProperties();

    protected abstract void refreshFromModel();

    protected abstract void commitToModel(boolean onSave);

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);
        model = (BndEditModel) form.getInput();

        for (String prop : getProperties()) {
            subscribedProps.add(prop);
            model.addPropertyChangeListener(prop, this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (model != null)
            for (String prop : subscribedProps) {
                model.removePropertyChangeListener(prop, this);
            }
    }

    @Override
    public void refresh() {
        refreshFromModel();
        super.refresh();
    }

    @Override
    public void commit(boolean onSave) {
        super.commit(onSave);
        commitToModel(onSave);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        if (page.isActive()) {
            refresh();
        } else {
            markStale();
        }
    }

}
