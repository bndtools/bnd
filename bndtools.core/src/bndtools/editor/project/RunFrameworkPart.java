package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.libg.header.OSGiHeader;
import bndtools.BndConstants;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.VersionedClause;
import bndtools.utils.ModificationLock;

public class RunFrameworkPart extends SectionPart implements PropertyChangeListener {

    private static final String WARNING_NO_FWK = "WARNING_NO_FWK";

    private final ModificationLock lock = new ModificationLock();

    private BndEditModel model;
    private VersionedClause framework;
    private Combo combo;

    public RunFrameworkPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    final void createSection(Section section, FormToolkit toolkit) {
        section.setText("Run Framework");

        combo = new Combo(section, SWT.DROP_DOWN);
        combo.setItems(new String [] {
                "org.eclipse.osgi",
                "org.apache.felix.framework"
        });

        Listener l = new Listener() {
            public void handleEvent(Event event) {
                markDirty();
                IMessageManager messages = getManagedForm().getMessageManager();

                String text = combo.getText();
                if(text == null || text.length() == 0) {
                    framework = null;
                    messages.addMessage(WARNING_NO_FWK, "No runtime framework specified", null, IMessageProvider.WARNING, combo);
                    return;
                } else {
                    messages.removeMessage(WARNING_NO_FWK, combo);
                }

                Map<String, Map<String, String>> header = OSGiHeader.parseHeader(text);
                if(header.size() != 1) {
                    framework = null;
                    messages.addMessage("ERROR_INVALID_FWK", "Invalid format", null, IMessageProvider.ERROR, combo);
                    return;
                } else {
                    messages.removeMessage("ERROR_INVALID_FWK", combo);
                }

                Entry<String, Map<String, String>> entry = header.entrySet().iterator().next();
                framework = new VersionedClause(entry.getKey(), entry.getValue());
            }
        };
        combo.addListener(SWT.Modify, l);
        combo.addListener(SWT.Selection, l);

        section.setClient(combo);
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);
        model = (BndEditModel) form.getInput();
        model.addPropertyChangeListener(BndConstants.RUNFRAMEWORK, this);
    }

    @Override
    public void dispose() {
        super.dispose();
        if(model != null)
            model.removePropertyChangeListener(BndConstants.RUNFRAMEWORK, this);
    }

    @Override
    public void refresh() {
        framework = model.getRunFramework();

        lock.modifyOperation(new Runnable() {
            public void run() {
                if(framework == null) {
                    combo.setText("");
                } else {
                    StringBuilder builder = new StringBuilder();
                    framework.formatTo(builder);
                    combo.setText(builder.toString());
                }
            }
        });
    }

    @Override
    public void commit(boolean onSave) {
        super.commit(onSave);
        model.setRunFramework(framework);
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
