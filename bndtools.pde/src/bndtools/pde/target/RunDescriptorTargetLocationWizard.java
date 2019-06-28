package bndtools.pde.target;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationWizard;

public class RunDescriptorTargetLocationWizard extends Wizard implements ITargetLocationWizard {
	private ITargetDefinition				targetDefinition;
	private RunDescriptorTargetLocation		targetLocation;

	private RunDescriptorTargetLocationPage	targetLocationPage;

	public RunDescriptorTargetLocationWizard() {
		setWindowTitle("Run Descriptor Target Location");
	}

	@Override
	public void setTarget(ITargetDefinition targetDefinition) {
		this.targetDefinition = targetDefinition;
	}

	public void setTargetLocation(RunDescriptorTargetLocation targetLocation) {
		this.targetLocation = targetLocation;
	}

	@Override
	public void addPages() {
		targetLocationPage = new RunDescriptorTargetLocationPage(targetDefinition, targetLocation);
		addPage(targetLocationPage);
	}

	@Override
	public boolean performFinish() {
		targetLocation = targetLocationPage.getBundleContainer();
		return true;
	}

	@Override
	public ITargetLocation[] getLocations() {
		return new ITargetLocation[] {
			targetLocation
		};
	}
}
