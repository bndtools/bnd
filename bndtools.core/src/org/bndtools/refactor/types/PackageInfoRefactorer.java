package org.bndtools.refactor.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.JavaSourceType;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.exceptions.Exceptions;

/**
 * Refactorings on a package-info.java file
 */
@Component(service = {
	PackageInfoRefactorer.class, IQuickFixProcessor.class
})
public class PackageInfoRefactorer extends BaseRefactorer implements IQuickFixProcessor {

	public static final String	FILE_NAME		= "package-info.java";
	public final static String	DEFAULT_CONTENT	= "package ${elementName};\n";
	public final static String	EXPORT_A		= "org.osgi.annotation.bundle.Export";
	public final static String	VERSION_A		= "org.osgi.annotation.versioning.Version";
	public final static String	PROVIDER_TYPE_A	= "org.osgi.annotation.versioning.ProviderType";

	@Override
	public void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> root,
		IInvocationContext context) {
		root.isJavaSourceType(JavaSourceType.PACKAGEINFO)
			.upTo(PackageDeclaration.class)
			.noneOfTheseAnnotations(VERSION_A)
			.forEach(packageDeclaration -> {
				builder.build("pck.exp+", "Export package", "package-export", 0,
					() -> export(assistant, packageDeclaration));
			});

	}

	private void export(RefactorAssistant assistant, PackageDeclaration pd) {
		Annotation version = assistant.newAnnotation(VERSION_A, "1.0.0");
		Annotation export = assistant.newAnnotation(EXPORT_A);
		assistant.ensureAnnotation(pd, version);
		assistant.ensureAnnotation(pd, export);
	}

	/**
	 * Process a list of elements in a selection and filter out the packages
	 * that could jave a package-info.java
	 *
	 * @param list a list of any object but no null
	 * @return a list of PackageEntry descriptors
	 */
	public static List<PackageEntry> getPackages(List<?> list) {
		try {
			List<PackageEntry> result = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof IPackageFragmentRoot ipfr) {
					List<IJavaElement> children = Arrays.asList(ipfr.getChildren());
					List<PackageEntry> packages = getPackages(children);
					result.addAll(packages);
				} else if (o instanceof ICompilationUnit cpu) {
					IPackageFragment parent = (IPackageFragment) cpu.getParent();
					result.add(new PackageEntry(parent));
				} else if (o instanceof IPackageFragment ipf) {
					result.add(new PackageEntry(ipf));
				} else {
					// ???
				}
			}
			result.removeIf(pe -> pe.package_.isDefaultPackage());
			return result;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Models the options on the package info. This can be edited by the UI
	 */
	public static class PackageEntry {

		public PackageEntry(IPackageFragment ipf) throws JavaModelException {
			this.package_ = ipf;
			this.packageName = ipf.getElementName();
			ICompilationUnit unit = ipf.getCompilationUnit(FILE_NAME);
			if (unit.exists()) {
				this.include = true;

				IPackageDeclaration pd = unit.getPackageDeclaration(ipf.getElementName());
				if (pd != null && pd.exists()) {
					this.export = pd.getAnnotation(EXPORT_A)
						.exists();
					this.version = (String) RefactorAssistant.getAnnotationValues(pd.getAnnotation(VERSION_A))
						.getOrDefault("value", version);
					this.hasProvider = pd.getAnnotation(PROVIDER_TYPE_A)
						.exists();
				}
			}
		}

		PackageEntry(String package_, boolean export, String version, boolean provider) {
			this.package_ = null;
			this.packageName = package_;
			this.include = true;
			this.export = export;
			this.version = version;
			this.hasProvider = provider;
		}

		public final IPackageFragment	package_;
		public final String				packageName;
		public boolean					include		= true;
		public boolean					export		= true;
		public String					version		= "1.0.0";
		public boolean					hasProvider	= false;
	}

	public static void ensureThat(RefactorAssistant assistant, PackageEntry entry, IProgressMonitor monitor)
		throws Exception {
		PackageDeclaration pd = assistant.ensurePackageDeclaration(entry.packageName);


		if (entry.export) {
			Annotation export = assistant.newAnnotation(EXPORT_A);
			Annotation version = assistant.newAnnotation(VERSION_A, entry.version);
			assistant.ensureAnnotation(pd, export);
			assistant.ensureAnnotation(pd, version);
		} else {
			assistant.deleteAnnotation(pd, EXPORT_A);
			assistant.deleteAnnotation(pd, VERSION_A);
		}

		if (entry.hasProvider) {
			Annotation provider = assistant.newAnnotation(PROVIDER_TYPE_A);
			assistant.ensureAnnotation(pd, provider);
		} else {
			assistant.deleteAnnotation(pd, PROVIDER_TYPE_A);
		}
	}

}
