package org.bndtools.builder.handlers.activator;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier.BundleActivatorError;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;

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
		result.add(new MarkerData(getDefaultResource(project), attribs, true));

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
		List<IMarkerResolution> result = new ArrayList<>();

		String content = readContent(marker);
		if (content == null || findBundleActivatorEntry(content) == null)
			return result;

		for (String candidate : findActivatorCandidates(marker)) {
			result.add(new BundleActivatorResolution("Change " + Constants.BUNDLE_ACTIVATOR + " to " + candidate,
				Constants.BUNDLE_ACTIVATOR + ": " + candidate));
		}
		result.add(new BundleActivatorResolution("Remove the " + Constants.BUNDLE_ACTIVATOR + " header", null));

		return result;
	}

	@Override
	public List<ICompletionProposal> getProposals(IMarker marker) {
		List<ICompletionProposal> proposals = new ArrayList<>();

		String content = readContent(marker);
		if (content == null)
			return proposals;
		EntryRegion entry = findBundleActivatorEntry(content);
		if (entry == null)
			return proposals;

		for (String candidate : findActivatorCandidates(marker)) {
			String replacement = Constants.BUNDLE_ACTIVATOR + ": " + candidate;
			proposals.add(new CompletionProposal(replacement, entry.start, entry.end - entry.start,
				replacement.length(), null, "Change " + Constants.BUNDLE_ACTIVATOR + " to " + candidate, null, null));
		}
		proposals.add(new CompletionProposal("", entry.start, entry.endWithEol - entry.start, 0, null,
			"Remove the " + Constants.BUNDLE_ACTIVATOR + " header", null, null));

		return proposals;
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
				// only source types, they are certain to end up in the bundle
				if (type.getCompilationUnit() == null)
					continue;
				int flags = type.getFlags();
				if (!type.isClass() || Flags.isAbstract(flags) || !Flags.isPublic(flags))
					continue;
				// an activator in the default package is itself an error
				if (type.getPackageFragment()
					.getElementName()
					.isEmpty())
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

	private static String readContent(IMarker marker) {
		IResource resource = marker.getResource();
		if (!(resource instanceof IFile))
			return null;
		IPath location = resource.getLocation();
		if (location == null)
			return null;
		try {
			return IO.collect(location.toFile());
		} catch (Exception e) {
			return null;
		}
	}

	private static EntryRegion findBundleActivatorEntry(String content) {
		try {
			PropertiesLineReader reader = new PropertiesLineReader(content);
			for (LineType type = reader.next(); type != LineType.eof; type = reader.next()) {
				if (type == LineType.entry && Constants.BUNDLE_ACTIVATOR.equals(reader.key())) {
					IRegion region = reader.region();
					int start = region.getOffset();
					int end = start + region.getLength();
					int endWithEol = end;
					if (endWithEol < content.length() && content.charAt(endWithEol) == '\r')
						endWithEol++;
					if (endWithEol < content.length() && content.charAt(endWithEol) == '\n')
						endWithEol++;
					return new EntryRegion(start, end, endWithEol);
				}
			}
		} catch (Exception e) {
			// ignore, treated as entry not found
		}
		return null;
	}

	private static class EntryRegion {
		final int	start;
		final int	end;
		final int	endWithEol;

		EntryRegion(int start, int end, int endWithEol) {
			this.start = start;
			this.end = end;
			this.endWithEol = endWithEol;
		}
	}

	private static class BundleActivatorResolution implements IMarkerResolution {
		private final String	label;
		private final String	replacement;	// null removes the entry

		BundleActivatorResolution(String label, String replacement) {
			this.label = label;
			this.replacement = replacement;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public void run(IMarker marker) {
			IResource resource = marker.getResource();
			if (!(resource instanceof IFile))
				return;
			final IFile file = (IFile) resource;
			final IWorkspace workspace = file.getWorkspace();
			try {
				workspace.run(monitor -> {
					String content = readContent(marker);
					if (content == null)
						return;
					EntryRegion entry = findBundleActivatorEntry(content);
					if (entry == null)
						return;
					String newContent;
					if (replacement == null)
						newContent = content.substring(0, entry.start) + content.substring(entry.endWithEol);
					else
						newContent = content.substring(0, entry.start) + replacement + content.substring(entry.end);
					file.setContents(new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8)), false,
						true, monitor);
				}, null);
			} catch (CoreException e) {
				logger.logError("Error applying " + Constants.BUNDLE_ACTIVATOR + " quick fix.", e);
			}
		}
	}

}
