package bndtools.internal.ui;

import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Instructions;
import aQute.libg.header.Attrs;
import bndtools.Central;
import bndtools.Plugin;

/**
 * A decorator for {@link IPackageFragment}s that adds an icon if the package is exported by the bundle manifest.
 * 
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
            IPackageFragment pkg = (IPackageFragment) element;
            String pkgName = pkg.getElementName();

            IJavaProject javaProject = pkg.getJavaProject();
            IProject project = javaProject.getProject();

            Instructions exports = Central.getExportedPackageModel(project);
            for (Entry<Instruction,Attrs> entry : exports.entrySet()) {
                if (entry.getKey().matches(pkgName)) {
                    decoration.addOverlay(plusIcon);
                    break;
                }
            }
        }
    }
}
