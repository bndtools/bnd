package org.bndtools.builder.handlers.activator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.bndtools.builder.handlers.BndHeaderQuickFix;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier.BundleActivatorError;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;
import bndtools.central.Central;

public class BundleActivatorErrorHandler extends AbstractBuildErrorDetailsHandler {

	private static final String		PROP_ACTIVATOR_CLASS_NAME	= "BundleActivatorError.activatorClassName";
	private static final String		BUNDLE_ACTIVATOR_INTERFACE	= "org.osgi.framework.BundleActivator";

	private static final ILogger	logger						= Logger.getLogger(BundleActivatorErrorHandler.class);

	@Override
	public List<MarkerData> generateMarkerData(IProject project, Processor model, Location location) throws Exception {
		List<MarkerData> result = new ArrayList<>();

		BundleActivatorError baError = (BundleActivatorError) location.details;

		IJavaProject javaProject = JavaCore.create(project);

		Map<String, Object> attribs = createMessageMarkerAttributes(baError, location.message);
		// Eclipse line numbers are 1 indexed
		attribs.put(IMarker.LINE_NUMBER, location.line + 1);

		// Add a marker to the bnd file on the BundleActivator line
		IResource resource = location.file == null ? null : Central.toResource(IO.getFile(location.file));
		result.add(new MarkerData(resource != null ? resource : getDefaultResource(project), attribs, true));

		MarkerData md;
		switch (baError.errorType) {
			case NO_SUITABLE_CONSTRUCTOR :
				md = createMethodMarkerData(javaProject, baError.activatorClassName, "<init>", "()V",
					createMessageMarkerAttributes(baError, location.message), false);
				if (md != null) {
					result.add(md);
					break;
				}
				//$FALL-THROUGH$
			case IS_INTERFACE :
			case IS_ABSTRACT :
			case NOT_PUBLIC :
			case NOT_AN_ACTIVATOR :
			case DEFAULT_PACKAGE :
			case IS_IMPORTED :
				md = createTypeMarkerData(javaProject, baError.activatorClassName,
					createMessageMarkerAttributes(baError, location.message), false);
				if (md != null)
					result.add(md);
				break;
			case NOT_ACCESSIBLE :
			default :
				// No file to mark
				break;
		}

		return result;
	}

	private Map<String, Object> createMessageMarkerAttributes(BundleActivatorError baError, String message) {
		Map<String, Object> attribs = new HashMap<>();
		attribs.put(PROP_ACTIVATOR_CLASS_NAME, baError.activatorClassName);
		attribs.put("BundleActivatorError.errorType", baError.errorType.toString());
		attribs.put(IMarker.MESSAGE, message.trim());
		return attribs;
	}

	@Override
	public List<IMarkerResolution> getResolutions(IMarker marker) {
		return new ArrayList<>(createFixes(marker));
	}

	@Override
	public List<ICompletionProposal> getProposals(IMarker marker) {
		return new ArrayList<>(createFixes(marker));
	}

	private List<BndHeaderQuickFix> createFixes(IMarker marker) {
		List<BndHeaderQuickFix> fixes = new ArrayList<>();
		if (!BndHeaderQuickFix.hasHeader(marker, Constants.BUNDLE_ACTIVATOR))
			return fixes;
		for (String candidate : findActivatorCandidates(marker)) {
			fixes.add(new BndHeaderQuickFix(Constants.BUNDLE_ACTIVATOR, candidate,
				"Change " + Constants.BUNDLE_ACTIVATOR + " to " + candidate));
		}
		fixes.add(new BndHeaderQuickFix(Constants.BUNDLE_ACTIVATOR, null,
			"Remove the " + Constants.BUNDLE_ACTIVATOR + " header"));
		return fixes;
	}

	private List<String> findActivatorCandidates(IMarker marker) {
		List<String> candidates = new ArrayList<>();
		String currentActivator = marker.getAttribute(PROP_ACTIVATOR_CLASS_NAME, null);
		try {
			IJavaProject javaProject = JavaCore.create(marker.getResource()
				.getProject());
			if (javaProject == null || !javaProject.exists())
				return candidates;

			IType activatorInterface = javaProject.findType(BUNDLE_ACTIVATOR_INTERFACE);
			if (activatorInterface == null)
				return candidates;

			ITypeHierarchy hierarchy = activatorInterface.newTypeHierarchy(javaProject, null);
			for (IType type : hierarchy.getAllSubtypes(activatorInterface)) {
				if (!isActivatorCandidate(type, javaProject))
					continue;
				String className = type.getFullyQualifiedName('$');
				if (!className.equals(currentActivator))
					candidates.add(className);
			}
			Collections.sort(candidates);
		} catch (Exception e) {
			logger.logError("Error searching for " + BUNDLE_ACTIVATOR_INTERFACE + " implementations", e);
		}
		return candidates;
	}

	static boolean isActivatorCandidate(IType type, IJavaProject project) throws Exception {
		if (type.getCompilationUnit() == null || !project.equals(type.getJavaProject()))
			return false;
		IPackageFragmentRoot root = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root == null || root.getRawClasspathEntry().isTest())
			return false;
		int flags = type.getFlags();
		if (!type.isClass() || Flags.isAbstract(flags) || !Flags.isPublic(flags)
			|| type.getPackageFragment().getElementName().isEmpty())
			return false;
		if (type.isMember() && !Flags.isStatic(flags))
			return false;
		for (IType enclosing = type.getDeclaringType(); enclosing != null; enclosing = enclosing.getDeclaringType()) {
			if (!Flags.isPublic(enclosing.getFlags()))
				return false;
		}
		boolean declaredConstructor = false;
		for (IMethod method : type.getMethods()) {
			if (method.isConstructor()) {
				declaredConstructor = true;
				if (Flags.isPublic(method.getFlags()) && method.getNumberOfParameters() == 0)
					return true;
			}
		}
		return !declaredConstructor;
	}

}
