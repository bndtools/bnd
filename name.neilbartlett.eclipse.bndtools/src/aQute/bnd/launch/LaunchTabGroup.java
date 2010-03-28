package aQute.bnd.launch;

import org.eclipse.debug.ui.*;
import org.eclipse.debug.ui.sourcelookup.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.*;

public class LaunchTabGroup extends AbstractLaunchConfigurationTabGroup {

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        		new BndLaunchTab(),
                new JavaArgumentsTab(),
                new JavaClasspathTab(),
                new JavaJRETab(),
                new EnvironmentTab(),
                new CommonTab(),
        };
        setTabs(tabs);
    }

}
