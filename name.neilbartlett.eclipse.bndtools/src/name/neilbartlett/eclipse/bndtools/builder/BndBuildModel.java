package name.neilbartlett.eclipse.bndtools.builder;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;

import org.eclipse.core.runtime.IPath;

import aQute.lib.osgi.Instruction;

public class BndBuildModel {
	
	private final IPath path;
	private IPath targetPath;
	private Set<Instruction> includes;
	private Collection<? extends IPath> classpath;

	public BndBuildModel(IPath path) {
		this.path = path;
	}
	public IPath getTargetPath() {
		return targetPath;
	}
	public void setTargetPath(IPath targetPath) {
		this.targetPath = targetPath;
	}
	public IPath getPath() {
		return path;
	}
	public void setIncludes(Set<Instruction> includes) {
		this.includes = includes;
	}
	public boolean containsPackage(String packageName) {
		for (Instruction instruction : includes) {
			Matcher matcher = instruction.getMatcher(packageName);
			
			if(matcher.matches())
				return true;
		}
		return false;
	}
	public boolean containsAny(Collection<? extends String> affectedPackages) {
		for (Instruction instruction : includes) {
			for (String pkg : affectedPackages) {
				Matcher matcher = instruction.getMatcher(pkg);
				
				if(matcher.matches())
					return true;
			}
		}
		return false;
	}
	public Collection<? extends IPath> getClasspath() {
		return classpath;
	}
	public void setClasspath(Collection<? extends IPath> classpath) {
		this.classpath = classpath;
	}
}
