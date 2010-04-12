package aQute.bnd.junit;


import org.eclipse.debug.ui.*;
import org.eclipse.debug.ui.sourcelookup.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.*;
import org.eclipse.jdt.junit.launcher.*;


@SuppressWarnings("restriction")
public class OSGiJUnitTabGroup extends org.eclipse.jdt.internal.junit.launcher.JUnitTabGroup {
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {        
        ILaunchConfigurationTab[] tabs= new ILaunchConfigurationTab[] {
            new JUnitLaunchConfigurationTab(),
            new OSGiArgumentsTab(),
            new JavaArgumentsTab(),
            new JavaClasspathTab(),
            new JavaJRETab(),
            new SourceLookupTab(),
            new EnvironmentTab(),
            new CommonTab()
        };
        setTabs(tabs);
    }

}
