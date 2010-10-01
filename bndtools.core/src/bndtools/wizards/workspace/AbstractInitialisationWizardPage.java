package bndtools.wizards.workspace;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

import bndtools.Plugin;

public abstract class AbstractInitialisationWizardPage extends WizardPage {

    private boolean dontShow = false;

    public AbstractInitialisationWizardPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    public AbstractInitialisationWizardPage(String pageName) {
        super(pageName);
    }

    public void setDontShow(boolean dontShow) {
        this.dontShow = dontShow;

        IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
        store.setValue(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD, dontShow);
    }

    public boolean isDontShow() {
        return dontShow;
    }

    protected String getError() {
        // No errors to report currently
        return null;
    }

    protected String getWarning() {
        String result = null;
        if (dontShow) {
            result = "WARNING: Bndtools will no longer check whether the bundle repository is up to date!";
        }
        return result;
    }

    protected void validate() {
        String error = getError();
        if(error != null) {
            setMessage(error, IMessageProvider.ERROR);
        } else {
            String warning = getWarning();
            if(warning != null) {
                setMessage(warning, IMessageProvider.WARNING);
            } else {
                setMessage(null);
            }
        }
    }
}
