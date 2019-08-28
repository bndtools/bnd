package bndtools.internal.pkgselection;

public interface IPackageLister {
	String[] getPackages(boolean includeNonSource, IPackageFilter filter) throws PackageListException;
}
