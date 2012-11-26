package bndtools.ace.launch.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

public class AceLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new AceLaunchTab(),
                new SourceLookupTab(),
                new JavaJRETab(),
                new EnvironmentTab(),
                new CommonTab(),
        };
        setTabs(tabs);
    }
}
