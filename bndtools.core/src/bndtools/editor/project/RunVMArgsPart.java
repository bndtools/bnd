package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.BndConstants;
import bndtools.editor.model.BndEditModel;
import bndtools.utils.ModificationLock;

public class RunVMArgsPart extends SectionPart implements PropertyChangeListener {

    private final ModificationLock lock = new ModificationLock();

    private BndEditModel model;
    private String vmargs = null;
	private Text textField;

    public RunVMArgsPart(Composite parent, FormToolkit toolkit, int style) {
	    super(parent, toolkit, style);
	    createSection(getSection(), toolkit);
	}

	final void createSection(Section section, FormToolkit toolkit) {
	    section.setText("VM Arguments");

	    Composite composite = toolkit.createComposite(section);

	    textField = toolkit.createText(composite, "", SWT.MULTI | SWT.BORDER);
	    textField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        markDirty();
                        vmargs = textField.getText();
                        if(vmargs.length() == 0)
                            vmargs = null;
                        validate();
                    }
                });
            }
        });

	    GridLayout layout = new GridLayout(1, false);
	    layout.marginWidth = 0;
	    layout.marginHeight = 0;
	    composite.setLayout(layout);

	    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
	    gd.heightHint = 75;
	    gd.widthHint = 50;
	    textField.setLayoutData(gd);

	    section.setClient(composite);
    }

	void validate() {
	}

	@Override
	public void initialize(IManagedForm form) {
	    super.initialize(form);
	    model = (BndEditModel) form.getInput();
	    model.addPropertyChangeListener(BndConstants.RUNVMARGS, this);
	}

	@Override
	public void dispose() {
	    super.dispose();
	    if(model != null)
	        model.removePropertyChangeListener(BndConstants.RUNVMARGS, this);
	}

	@Override
	public void refresh() {
	    lock.modifyOperation(new Runnable() {
            public void run() {
                vmargs = model.getRunVMArgs();
                if(vmargs == null)
                    vmargs = ""; //$NON-NLS-1$
                textField.setText(vmargs);
                validate();
            }
        });
	}

	@Override
	public void commit(boolean onSave) {
	    super.commit(onSave);
	    model.setRunVMArgs(vmargs);
	}

	public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        if(page.isActive()) {
            refresh();
        } else {
            markStale();
        }
	}
}