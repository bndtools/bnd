package name.neilbartlett.eclipse.bndtools.wizards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IType;

public class BundleModel {
	
	private final List<IResource> classpathDirectiveJars = new ArrayList<IResource>();
	private final Set<String> exports = new HashSet<String>();
	private final Set<String> privatePackages = new HashSet<String>();
	
	private String bundleSymbolicName;
	private boolean enableActivator;
	private IType activatorClass;
	private boolean allExports = false;
	
	public String getBundleSymbolicName() {
		return bundleSymbolicName;
	}
	
	public void setBundleSymbolicName(String bundleSymbolicName) {
		this.bundleSymbolicName = bundleSymbolicName;
	}
	
	public boolean isEnableActivator() {
		return enableActivator;
	}
	
	public void setEnableActivator(boolean enableActivator) {
		this.enableActivator = enableActivator;
	}
	
	public IType getActivatorClass() {
		return activatorClass;
	}
	
	public void setActivatorClass(IType activatorClass) {
		this.activatorClass = activatorClass;
	}
	
	public Set<String> getExports() {
		return exports;
	}
	
	public Set<String> getPrivatePackages() {
		return privatePackages;
	}

	public List<IResource> getClasspathDirectiveJars() {
		return classpathDirectiveJars;
	}

	public boolean isAllExports() {
		return allExports;
	}

	public void setAllExports(boolean allExports) {
		this.allExports = allExports;
	}
	
}
