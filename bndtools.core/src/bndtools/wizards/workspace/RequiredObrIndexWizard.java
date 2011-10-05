package bndtools.wizards.workspace;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

public class RequiredObrIndexWizard extends Wizard {

    private final RequiredObrIndexWizardPage page;

    public RequiredObrIndexWizard(IProject project, List<String> urls) {
        page = new RequiredObrIndexWizardPage(project, urls);

        addPage(page);
    }

    @Override
    public boolean performFinish() {
        return true;
    }

    public List<String> getCheckedUrls() {
        return page.getCheckedUrls();
    }

}
