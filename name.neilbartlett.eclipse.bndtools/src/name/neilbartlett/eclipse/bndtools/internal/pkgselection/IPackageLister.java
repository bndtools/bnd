package name.neilbartlett.eclipse.bndtools.internal.pkgselection;

import java.util.Set;

public interface IPackageLister {
	public String[] getPackages(boolean includeNonSource, Set<String> excludes) throws PackageListException;
}
