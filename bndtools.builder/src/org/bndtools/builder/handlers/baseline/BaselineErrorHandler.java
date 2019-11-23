package org.bndtools.builder.handlers.baseline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.bndtools.builder.utils.MemberValuePairLocationRetriever;
import org.bndtools.utils.jdt.ASTUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;

public class BaselineErrorHandler extends AbstractBuildErrorDetailsHandler {

	private static final String		PACKAGEINFO					= "packageinfo";
	private static final String		PACKAGEINFOJAVA				= "package-info.java";
	private static final String		PROP_SUGGESTED_VERSION		= "suggestedVersion";

	private static final String		ANNOTATION_VERSION_BND_PKG	= "aQute.bnd.annotation";
	private static final String		ANNOTATION_VERSION_OSGI_PKG	= "org.osgi.annotation.versioning";
	private static final String		ANNOTATION_VERSION_NO_PKG	= "Version";
	private static final String		ANNOTATION_VERSION_BND		= ANNOTATION_VERSION_BND_PKG + "."
		+ ANNOTATION_VERSION_NO_PKG;
	private static final String		ANNOTATION_VERSION_OSGI		= ANNOTATION_VERSION_OSGI_PKG + "."
		+ ANNOTATION_VERSION_NO_PKG;

	private static final ILogger	logger						= Logger.getLogger(BaselineErrorHandler.class);

	@Override
	public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
		List<MarkerData> result = new LinkedList<>();

		Info baselineInfo = (Info) location.details;
		IJavaProject javaProject = JavaCore.create(project);

		result.addAll(generatePackageInfoMarkers(baselineInfo, javaProject, location.message));
		result.addAll(generateStructuralChangeMarkers(baselineInfo, javaProject));

		return result;
	}

	List<MarkerData> generatePackageInfoMarkers(Info baselineInfo, IJavaProject javaProject, String message)
		throws JavaModelException {
		List<MarkerData> markers = new LinkedList<>();
		for (IClasspathEntry entry : javaProject.getRawClasspath()) {
			if (IClasspathEntry.CPE_SOURCE == entry.getEntryKind()) {
				IPath entryPath = entry.getPath();
				IPath pkgPath = entryPath.append(baselineInfo.packageName.replace('.', '/'));

				// Find in packageinfo file
				IPath pkgInfoPath = pkgPath.append(PACKAGEINFO);
				IFile pkgInfoFile = javaProject.getProject()
					.getWorkspace()
					.getRoot()
					.getFile(pkgInfoPath);
				if (pkgInfoFile != null && pkgInfoFile.exists()) {
					Map<String, Object> attribs = new HashMap<>();
					attribs.put(IMarker.MESSAGE, message.trim());
					attribs.put(PROP_SUGGESTED_VERSION, baselineInfo.suggestedVersion.toString());

					LineLocation lineLoc = findVersionLocation(pkgInfoFile.getLocation()
						.toFile());
					if (lineLoc != null) {
						attribs.put(IMarker.LINE_NUMBER, lineLoc.lineNum);
						attribs.put(IMarker.CHAR_START, lineLoc.start);
						attribs.put(IMarker.CHAR_END, lineLoc.end);
					}

					markers.add(new MarkerData(pkgInfoFile, attribs, true));
				}

				// Find in package-info.java
				IPackageFragment pkg = javaProject.findPackageFragment(pkgPath);
				if (pkg != null) {
					ICompilationUnit pkgInfoJava = pkg.getCompilationUnit(PACKAGEINFOJAVA);
					if (pkgInfoJava != null && pkgInfoJava.exists()) {
						ISourceRange range = findPackageInfoJavaVersionLocation(baselineInfo.packageName, pkgInfoJava);

						Map<String, Object> attribs = new HashMap<>();
						attribs.put(IMarker.MESSAGE, message.trim());
						attribs.put(IJavaModelMarker.ID, 8088);
						attribs.put(PROP_SUGGESTED_VERSION, baselineInfo.suggestedVersion.toString());
						if (range != null) {
							attribs.put(IMarker.CHAR_START, range.getOffset());
							attribs.put(IMarker.CHAR_END, range.getOffset() + range.getLength());
							markers.add(new MarkerData(pkgInfoJava.getResource(), attribs, true,
								BndtoolsConstants.MARKER_JAVA_BASELINE));
						}
					}
				}
			}
		}
		return markers;
	}

	ISourceRange findPackageInfoJavaVersionLocation(String packageName, ICompilationUnit compUnit)
		throws JavaModelException {
		ISourceRange range = null;
		IPackageDeclaration[] pkgDecls = compUnit.getPackageDeclarations();
		if (pkgDecls != null) {
			for (IPackageDeclaration pkgDecl : pkgDecls) {
				if (packageName.equals(pkgDecl.getElementName())) {
					IAnnotation[] annots = pkgDecl.getAnnotations();
					for (IAnnotation annot : annots) {
						String name = annot.getElementName();
						if (ANNOTATION_VERSION_NO_PKG.equals(name) || ANNOTATION_VERSION_OSGI.equals(name)
							|| ANNOTATION_VERSION_BND.equals(name)) {
							ASTParser parser = ASTParser.newParser(AST.JLS11);
							parser.setKind(ASTParser.K_COMPILATION_UNIT);
							parser.setSource(compUnit);
							parser.setResolveBindings(true);
							CompilationUnit ast = (CompilationUnit) parser.createAST(null);
							if (ast != null) {
								MemberValuePairLocationRetriever mvpRetriever = new MemberValuePairLocationRetriever(
									annot, t -> ANNOTATION_VERSION_BND.equals(t) || ANNOTATION_VERSION_OSGI.equals(t),
									"value");
								ast.accept(mvpRetriever);
								range = mvpRetriever.getMemberValuePairSourceRange();
							}
						}
					}
				}
			}
		}
		return range;
	}

	List<MarkerData> generateStructuralChangeMarkers(Info baselineInfo, IJavaProject javaProject)
		throws JavaModelException {
		List<MarkerData> markers = new LinkedList<>();

		Delta packageDelta = baselineInfo.packageDiff.getDelta();

		// Iterate into the package member diffs
		for (Diff pkgMemberDiff : baselineInfo.packageDiff.getChildren()) {
			// Skip deltas that have lesser significance than the overall
			// package delta
			if (pkgMemberDiff.getDelta()
				.ordinal() < packageDelta.ordinal())
				continue;

			if (Delta.ADDED == pkgMemberDiff.getDelta()) {
				@SuppressWarnings("unused")
				Tree pkgMember = pkgMemberDiff.getNewer();
				// markers.addAll(generateAddedTypeMarker(javaProject,
				// pkgMember.getName(), pkgMember.ifAdded()));
			} else if (Delta.REMOVED == pkgMemberDiff.getDelta()) {
				//
			} else {
				Tree pkgMember = pkgMemberDiff.getOlder();
				if (pkgMember != null && (Type.INTERFACE == pkgMember.getType() || Type.CLASS == pkgMember.getType())) {
					String className = pkgMember.getName();

					// Iterate into the class member diffs
					for (Diff classMemberDiff : pkgMemberDiff.getChildren()) {
						// Skip deltas that have lesser significance than the
						// overall package delta (again)
						if (classMemberDiff.getDelta()
							.ordinal() < packageDelta.ordinal())
							continue;

						if (Delta.ADDED == classMemberDiff.getDelta()) {
							Tree classMember = classMemberDiff.getNewer();
							if (Type.METHOD == classMember.getType())
								markers.addAll(generateAddedMethodMarker(javaProject, className, classMember.getName(),
									classMember.ifAdded()));
						} else if (Delta.REMOVED == classMemberDiff.getDelta()) {
							Tree classMember = classMemberDiff.getOlder();
							if (Type.METHOD == classMember.getType()) {
								markers.addAll(generateRemovedMethodMarker(javaProject, className,
									classMember.getName(), classMember.ifRemoved()));
							}
						}
					}
				}
			}

		}

		return markers;
	}

	List<MarkerData> generateAddedMethodMarker(IJavaProject javaProject, String className, final String methodName,
		final Delta requiresDelta) throws JavaModelException {
		final List<MarkerData> markers = new LinkedList<>();
		final CompilationUnit ast = createAST(javaProject, className);
		if (ast != null) {
			ast.accept(new ASTVisitor() {
				@Override
				public boolean visit(MethodDeclaration methodDecl) {
					String signature = ASTUtil.buildMethodSignature(methodDecl);
					if (signature.equals(methodName)) {
						// Create the marker attribs here
						Map<String, Object> attribs = new HashMap<>();
						attribs.put(IMarker.CHAR_START, methodDecl.getStartPosition());
						attribs.put(IMarker.CHAR_END, methodDecl.getStartPosition() + methodDecl.getLength());

						String message = String
							.format("This method was added, which requires a %s change to the package.", requiresDelta);
						attribs.put(IMarker.MESSAGE, message);

						markers.add(new MarkerData(ast.getJavaElement()
							.getResource(), attribs, false));
					}

					return false;
				}
			});
		}
		return markers;
	}

	List<MarkerData> generateRemovedMethodMarker(IJavaProject javaProject, final String className,
		final String methodName, final Delta requiresDelta) throws JavaModelException {
		final List<MarkerData> markers = new LinkedList<>();
		final CompilationUnit ast = createAST(javaProject, className);
		if (ast != null) {
			ast.accept(new ASTVisitor() {
				@Override
				public boolean visit(TypeDeclaration typeDecl) {
					ITypeBinding typeBinding = typeDecl.resolveBinding();
					if (typeBinding != null) {
						if (typeBinding.getBinaryName()
							.equals(className)) {
							Map<String, Object> attribs = new HashMap<>();
							SimpleName nameNode = typeDecl.getName();
							attribs.put(IMarker.CHAR_START, nameNode.getStartPosition());
							attribs.put(IMarker.CHAR_END, nameNode.getStartPosition() + nameNode.getLength());

							String message = String.format(
								"The method '%s' was removed, which requires a %s change to the package.", methodName,
								requiresDelta);
							attribs.put(IMarker.MESSAGE, message);

							markers.add(new MarkerData(ast.getJavaElement()
								.getResource(), attribs, false));
							return false;
						}
					}
					return true;
				}
			});
		}
		return markers;
	}

	@Override
	public List<IMarkerResolution> getResolutions(IMarker marker) {
		List<IMarkerResolution> result = new LinkedList<>();

		final String suggestedVersion = marker.getAttribute(PROP_SUGGESTED_VERSION, null);
		if (suggestedVersion != null) {
			result.add(new IMarkerResolution() {
				@Override
				public void run(IMarker marker) {
					final IFile file = (IFile) marker.getResource();
					final IWorkspace workspace = file.getWorkspace();
					try {
						workspace.run(monitor -> {
							String input = "version " + suggestedVersion;
							ByteArrayInputStream stream = new ByteArrayInputStream(input.getBytes());
							file.setContents(stream, false, true, monitor);
						}, null);
					} catch (CoreException e) {
						logger.logError("Error applying baseline version quickfix.", e);
					}
				}

				@Override
				public String getLabel() {
					return "Change package version to " + suggestedVersion;
				}
			});

		}

		return result;
	}

	@Override
	public List<ICompletionProposal> getProposals(IMarker marker) {
		List<ICompletionProposal> proposals = new LinkedList<>();

		String suggestedVersion = marker.getAttribute(PROP_SUGGESTED_VERSION, null);
		int start = marker.getAttribute(IMarker.CHAR_START, 0);
		int end = marker.getAttribute(IMarker.CHAR_END, 0);
		CompletionProposal proposal = new CompletionProposal("version " + suggestedVersion, start, end - start, end,
			null, "Change package version to " + suggestedVersion, null, null);
		proposals.add(proposal);

		return proposals;
	}

	private LineLocation findVersionLocation(File file) {
		String content;
		try {
			content = IO.collect(file);
			PropertiesLineReader reader = new PropertiesLineReader(content);

			int lineNum = 1;
			LineType type = reader.next();
			while (type != LineType.eof) {
				if (type == LineType.entry) {
					String key = reader.key();
					if ("version".equals(key)) {
						LineLocation loc = new LineLocation();
						loc.lineNum = lineNum;
						IRegion region = reader.region();
						loc.start = region.getOffset();
						loc.end = region.getOffset() + region.getLength();
						return loc;
					}
				}
				type = reader.next();
				lineNum++;
			}
		} catch (Exception e) {
			// ignore
		}

		return null;
	}
}
