package aQute.bnd.launch;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

public class Shortcut implements ILaunchShortcut {

    public Shortcut() {
        System.out.println("Constructor Launch Shortcut");
    }

    protected String getLaunchConfigurationTypeId() {
        return "aQute.bnd.launch";
    }

    public void launch(ISelection selection, String mode) {
        IStructuredSelection is = (IStructuredSelection) selection;
        if ( is.getFirstElement() != null ) {
            Object selected = is.getFirstElement();
            IJavaElement element = null;
            if (selected instanceof IJavaElement )
                element = (IJavaElement) selected;
            else if ( selected instanceof IAdaptable ) {
                element = (IJavaElement) ((IAdaptable)selected).getAdapter(IJavaElement.class);
            }
            
            if ( element != null )
                launch(element.getJavaProject(), mode);
        }
        // TODO figure out which project we are in???
    }

    public void launch(IEditorPart editor, String mode) {
        IEditorInput input = editor.getEditorInput();
        IJavaElement je = (IJavaElement) input.getAdapter(IJavaElement.class);
        if (je != null) {
            IJavaProject jproject = je.getJavaProject();
            if (jproject != null) {
                launch(jproject, mode);
            }
        }
    }

    void launch(IJavaProject project, String mode) {
        try {
            ILaunchConfiguration config = find(project);
            if (config == null)
                config = createConfiguration(project);
            
            DebugUITools.launch(config, mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ILaunchConfigurationType getConfigurationType() {
        return getLaunchManager()
                .getLaunchConfigurationType("aQute.bnd.launch");
    }

    ILaunchConfiguration find(IJavaProject project) throws CoreException {
        List<ILaunchConfiguration> candidateConfigs = new ArrayList<ILaunchConfiguration>();
        ILaunchConfiguration[] configs = DebugPlugin.getDefault()
                .getLaunchManager().getLaunchConfigurations(
                        getConfigurationType());

        for (int i = 0; i < configs.length; i++) {
            ILaunchConfiguration config = configs[i];
            if (config
                    .getAttribute(
                            IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                            "").equals(project.getElementName())) { //$NON-NLS-1$
                candidateConfigs.add(config);
            }
        }
        
        if (candidateConfigs.size() == 0) {
        	return null;
        }
        // return the latest
        return candidateConfigs.get(candidateConfigs.size() - 1);
    }

    protected ILaunchManager getLaunchManager() {
        return DebugPlugin.getDefault().getLaunchManager();
    }

    protected ILaunchConfiguration createConfiguration(IJavaProject type)
            throws Exception {
        ILaunchConfiguration config = null;
        ILaunchConfigurationWorkingCopy wc = null;
        ILaunchConfigurationType configType = getConfigurationType();
        wc = configType.newInstance(null, getLaunchManager()
                .generateUniqueLaunchConfigurationNameFrom(
                        type.getElementName()));
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                type.getElementName());
        config = wc.doSave();
        return config;
    }

}
