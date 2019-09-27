package bndtools.editor.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import aQute.bnd.build.model.BndEditModel;
import bndtools.central.Central;
import bndtools.editor.BndEditor;

public abstract class BndEditorPart extends SectionPart implements PropertyChangeListener {

	protected BndEditModel		model;

	private final AtomicBoolean	committing		= new AtomicBoolean(false);
	private final List<String>	subscribedProps	= new LinkedList<>();

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
	public final void refresh() {
		if (!Central.hasWorkspaceDirectory()) {
			refreshFromModel();
		} else {
			Central.onWorkspaceAsync(workspace -> {
				ScrolledForm form = getManagedForm().getForm();
				if (form.isDisposed())
					return;

				refreshFromModel();

				if (BndEditor.SYNC_MESSAGE.equals(form.getMessage())) {
					form.setMessage(null, IMessageProvider.NONE);
				}
			});
		}
		super.refresh();
	}

	@Override
	public final void commit(boolean onSave) {
		committing.compareAndSet(false, true);
		super.commit(onSave);
		commitToModel(onSave);
		committing.compareAndSet(true, false);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (committing.get())
			return;
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		if (page.isActive()) {
			refresh();
		} else {
			markStale();
		}
	}

}
