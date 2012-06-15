package bndtools.internal.ui;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.ExportedPackage;
import bndtools.utils.FileUtils;

/**
 * A decorator for {@link IPackageFragment}s that adds an icon if the package is exported by
 * the bundle manifest.
 * @author duckAsteroid
 */
public class ExportedPackageDecorator extends LabelProvider implements ILightweightLabelDecorator {

    private ImageDescriptor plusIcon;
    
    public ExportedPackageDecorator() {
        super();
        this.plusIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/plus-decorator.png");
    }
    
    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof IPackageFragment) {
            IPackageFragment pkg = (IPackageFragment)element;
            IJavaProject javaProject = pkg.getJavaProject();
            IProject project = javaProject.getProject();
            try {
                // Load project file and model
                IFile projectFile = project.getFile(Project.BNDFILE);
                BndEditModel projectModel;
                IDocument projectDocument = FileUtils.readFully(projectFile);
                if (projectDocument == null)
                    projectDocument = new Document();
                projectModel = new BndEditModel();
                projectModel.loadFrom(projectDocument);
                
                if (isExported(pkg, projectModel.getExportedPackages())) {
                    decoration.addOverlay(plusIcon);
                }
                
            }
            catch(Exception e) {
                // Do nothing
                //throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
            }
        }
    }
    /**
     * Tests if the given package is exported by any of the exported packages in the BND model
     * @param pkg The package to test if exported
     * @param exportedPackages The list of exported packages from BND
     * @return <code>true</code> if the package is exported, <code>false</code> otherwise
     */
    private boolean isExported(IPackageFragment pkg, List<ExportedPackage> exportedPackages) {
        String packageName = pkg.getElementName();
        for(ExportedPackage export : exportedPackages) {
            String expPkgName = export.getName();
            if (expPkgName.equals("*")) {
                return true; // everything matches 'Export-Package: *'
            }
            else if (expPkgName.endsWith(".*")) {
                // e.g. Export-Package: com.acme.* 
                // if package name starts with exported package name - less .* then it's a match
                // so let's trim off the .*
                expPkgName = expPkgName.substring(0, expPkgName.length() - 2);
                if ((packageName.equals(expPkgName)) || (packageName.startsWith(expPkgName + "."))) {
                    return true;
                }
            } 
            else if (packageName.equals(expPkgName)) {
                // otherwise exact match required
                return true;
            }
        }
        return false;
    }

}
