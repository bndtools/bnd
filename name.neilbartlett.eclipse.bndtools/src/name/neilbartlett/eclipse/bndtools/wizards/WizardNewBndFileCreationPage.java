package name.neilbartlett.eclipse.bndtools.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.BndConstants;
import name.neilbartlett.eclipse.bndtools.internal.libs.CollectionStringifier;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.osgi.framework.Constants;

import static name.neilbartlett.eclipse.bndtools.internal.libs.Functions.*;

public class WizardNewBndFileCreationPage extends WizardNewFileCreationPage {
	
	private static final String CLASSPATH_DIRECTIVE = "-classpath";

	private final BundleModel bundleModel;
	
	public WizardNewBndFileCreationPage(String pageName, IStructuredSelection selection, BundleModel bundleModel) {
		super(pageName, selection);
		this.bundleModel = bundleModel;
	}

	@Override
	protected InputStream getInitialContents() {
		StringBuilder builder = new StringBuilder();
		
		// -classpath Directive
		if(bundleModel.getClasspathDirectiveJars().size() > 0) {
			List<String> containerPath = Arrays.asList(getContainerFullPath().segments());
			
			List<IResource> jars = bundleModel.getClasspathDirectiveJars();
			List<List<String>> jarPaths = new ArrayList<List<String>>(jars.size());
			
			for (IResource resource : jars) {
				 List<String> jarPath = Arrays.asList(resource.getFullPath().segments());
				 List<String> relativePath = makeRelative(containerPath, jarPath);
				 jarPaths.add(relativePath);
			}
			appendListProperty(builder, CLASSPATH_DIRECTIVE, jarPaths, new CollectionStringifier<String>("/"));
		}
		
		// Bundle-SymbolicName
		String bundleName = bundleModel.getBundleSymbolicName();
		String expectedFileName = bundleName + ".bnd";
		if(!expectedFileName.equals(getFileName())) {
			appendSimpleProperty(builder, Constants.BUNDLE_SYMBOLICNAME, bundleName);
		}
		
		// Bundle-Activator
		if(bundleModel.isEnableActivator()) {
			IType activator = bundleModel.getActivatorClass();
			appendSimpleProperty(builder, Constants.BUNDLE_ACTIVATOR, activator.getFullyQualifiedName());
			
			IPackageFragment pkg = activator.getPackageFragment();
			if(!bundleModel.getPrivatePackages().contains(pkg) && !bundleModel.getExports().contains(pkg)) {
				bundleModel.getPrivatePackages().add(pkg.getElementName());
			}
		}

		// Export-Package
		if(bundleModel.isAllExports()) {
			appendSimpleProperty(builder, Constants.EXPORT_PACKAGE, "*");
		} else {
			appendListProperty(builder, Constants.EXPORT_PACKAGE, bundleModel.getExports());
		}
		
		// Private-Package
		appendListProperty(builder, BndConstants.PRIVATE_PACKAGE, bundleModel.getPrivatePackages());
		
		
		return new ByteArrayInputStream(builder.toString().getBytes());
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		if(event.type == SWT.Modify) {
			String fileName = getFileName();
			if(fileName.endsWith(".bnd")) {
				fileName = fileName.substring(0, fileName.length() - 4);
			}
			bundleModel.setBundleSymbolicName(fileName);
		}
	}
	
}
