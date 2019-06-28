package org.bndtools.core.editors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.properties.Document;
import aQute.bnd.properties.IDocument;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import bndtools.central.Central;
import bndtools.central.RepositoryUtils;

public class ImportPackageQuickFixProcessor implements IQuickFixProcessor {

	static ILogger			logger					= Logger.getLogger(ImportPackageQuickFixProcessor.class);

	// Relevance constants
	public static final int	ADD_BUNDLE				= 15;
	public static final int	ADD_BUNDLE_WORKSPACE	= ADD_BUNDLE + 1;

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.IsClassPathCorrect :
			case IProblem.ImportNotFound :
			case IProblem.UndefinedType :
				return true;
			default :
				return false;
		}
	}

	private static Requirement buildRequirement(String importName) {
		final CapReqBuilder b = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
		b.addFilter(PackageNamespace.PACKAGE_NAMESPACE, importName, null, new Attrs());
		return b.buildSyntheticRequirement();
	}

	private Collection<Capability> searchRepository(Repository osgiRepo, String pkg) {
		final Requirement req = buildRequirement(pkg);
		Map<Requirement, Collection<Capability>> providers = osgiRepo.findProviders(Collections.singleton(req));
		return providers.get(req);
	}

	static class BndBuildPathHandler {
		private final IInvocationContext	context;
		private IProject					eclipseProject;
		private IFile						bndFile;
		private IDocument					bndDoc;
		private BndEditModel				bndModel;
		private List<VersionedClause>		buildPath;
		private Set<String>					buildPathBundles;
		private Set<VersionedClause>		buildPathVersioned;
		private IProgressMonitor			monitor;

		public BndBuildPathHandler(IInvocationContext context) {
			this.context = context;
		}

		public IProgressMonitor getProgressMonitor() {
			return monitor;
		}

		public void setProgressMonitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		// This has been pushed into this method to defer initialization as this
		// makes testing easier.
		void loadFileInfo() {
			if (bndFile != null) {
				return;
			}
			final ICompilationUnit compUnit = context.getCompilationUnit();
			final IJavaProject java = compUnit.getJavaProject();
			eclipseProject = java.getProject();
			bndFile = eclipseProject.getFile(Project.BNDFILE);
		}

		Workspace getWorkspace() throws Exception {
			return Central.getWorkspace();
		}

		void loadModel() throws CoreException {
			if (buildPathVersioned != null) {
				return;
			}
			loadFileInfo();
			String contents;
			try {
				contents = IO.collect(new InputStreamReader(bndFile.getContents(), bndFile.getCharset()));
				bndDoc = new Document(contents);
				bndModel = new BndEditModel(getWorkspace());
				bndModel.loadFrom(bndDoc);
			} catch (IOException e) {
				throw new CoreException(
					new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, "Error trying to read " + bndFile, e));
			} catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID,
					"Error trying to access Bnd workspace", e));
			}

			final List<VersionedClause> origBuildPath = bndModel.getBuildPath();
			buildPath = origBuildPath == null ? new ArrayList<>() : new ArrayList<>(origBuildPath);
			final int capacity = 2 * buildPath.size() + 1;
			buildPathBundles = new HashSet<>(capacity);
			buildPathVersioned = new HashSet<>(capacity);
			for (VersionedClause bundleVersion : buildPath) {
				buildPathBundles.add(bundleVersion.getName());
				buildPathVersioned.add(bundleVersion);
			}
		}

		public List<VersionedClause> getBuildPath() throws CoreException {
			loadModel();
			return buildPath;
		}

		public IFile getBndFile() {
			loadFileInfo();
			return bndFile;
		}

		public void addBundle(String bundle) throws CoreException {
			addBundle(new VersionedClause(bundle, new Attrs()));
		}

		public void addBundle(VersionedClause versionedBundle) throws CoreException {
			loadModel();
			buildPathBundles.add(versionedBundle.getName());
			if (buildPathVersioned.add(versionedBundle)) {
				buildPath.add(versionedBundle);
			}
			bndModel.setBuildPath(buildPath);
			bndModel.saveChangesTo(bndDoc);
			InputStream str;
			try {
				str = new ByteBufferInputStream(bndDoc.get()
					.getBytes(bndFile.getCharset()));
			} catch (UnsupportedEncodingException e) {
				throw new CoreException(
					new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, "Invalid encoding for " + bndFile, e));
			}
			bndFile.setContents(str, IResource.KEEP_HISTORY, monitor);
		}

		public boolean containsBundle(String bundle) throws CoreException {
			loadModel();
			// TODO: This will need modification to handle sub-bundles as the
			// BSN is derived differently.
			final String bsn = eclipseProject.getName();
			return bsn.equals(bundle) || buildPathBundles.contains(bundle);
		}
	}

	class AddBundleJob extends WorkspaceJob {
		private final String				bundle;
		private final BndBuildPathHandler	handler;

		public AddBundleJob(IInvocationContext context, String bundle) {
			super("Adding '" + bundle + "' to Bnd build path");
			this.bundle = bundle;
			handler = getBuildPathHandler(context);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IResourceRuleFactory ruleFactory = workspace.getRuleFactory();
			setRule(ruleFactory.modifyRule(handler.getBndFile()));
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) {
			try {
				handler.setProgressMonitor(monitor);
				handler.addBundle(bundle);

				return Status.OK_STATUS;
			} catch (CoreException e) {
				return e.getStatus();
			}
		}
	}

	BndBuildPathHandler getBuildPathHandler(IInvocationContext context) {
		return new BndBuildPathHandler(context);
	}

	// Lazily initialize this or else it won't load during testing.
	static Image ICON;

	class AddBundleCompletionProposal implements IJavaCompletionProposal {

		final String				bundle;
		final String				displayString;
		final int					relevance;
		final IInvocationContext	context;

		public AddBundleCompletionProposal(String bundle, List<String> r, int relevance, IInvocationContext context) {
			this.bundle = bundle;
			this.relevance = relevance;
			this.context = context;
			final String firstRepo = r.get(0);
			switch (r.size()) {
				case 1 :
					this.displayString = Strings.format("Add bundle '%s' to Bnd build path (from %s)", bundle,
						firstRepo);
					break;
				case 2 :
					this.displayString = Strings.format("Add bundle '%s' to Bnd build path (from %s + 1 other)", bundle,
						firstRepo);
					break;
				default :
					this.displayString = Strings.format("Add bundle '%s' to Bnd build path (from %s + %d others)",
						bundle, firstRepo, r.size() - 1);
					break;
			}
		}

		@Override
		public void apply(org.eclipse.jface.text.IDocument document) {
			AddBundleJob job = new AddBundleJob(context, bundle);
			job.schedule();
		}

		/**
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public Point getSelection(org.eclipse.jface.text.IDocument document) {
			return new Point(context.getSelectionOffset(), context.getSelectionLength());
		}

		@Override
		public String getAdditionalProposalInfo() {
			return displayString;
		}

		@Override
		public String getDisplayString() {
			return displayString;
		}

		@Override
		public Image getImage() {
			if (ICON == null) {
				try {
					ICON = Icons.desc("bundle")
						.createImage();
				} catch (RuntimeException e) {
					logger.logError("Couldn't load bundle image", e);
				}
			}
			return ICON;
		}

		@Override
		public IContextInformation getContextInformation() {
			return new IContextInformation() {

				@Override
				public String getContextDisplayString() {
					return "Added " + bundle + " to build path";
				}

				@Override
				public Image getImage() {
					return null;
				}

				@Override
				public String getInformationDisplayString() {
					return "Added " + bundle + " to build path - info";
				}

			};
		}

		@Override
		public int getRelevance() {
			return relevance;
		}
	}

	// Wrap these methods to make it easier to test.
	Workspace getWorkspace() throws Exception {
		return Central.getWorkspace();
	}

	// Wrap these methods to make it easier to test.
	Repository getWorkspaceRepo() throws Exception {
		return Central.getWorkspaceR5Repository();
	}

	List<RepositoryPlugin> listRepositories() {
		return RepositoryUtils.listRepositories(true);
	}

	IJavaCompletionProposal[] getSuggestions(Set<String> pkgs, IInvocationContext context) throws CoreException {
		List<RepositoryPlugin> ps = listRepositories();

		final BndBuildPathHandler handler = getBuildPathHandler(context);
		MultiMap<String, String> bundles = new MultiMap<>();
		String wsName = null;
		boolean loggedWorkspaceError = false;
		for (String pkg : pkgs) {
			for (RepositoryPlugin p : ps) {
				Collection<Capability> caps = null;
				if (p instanceof Repository) {
					caps = searchRepository((Repository) p, pkg);
				} else if (p instanceof WorkspaceRepository) {
					try {
						caps = searchRepository(getWorkspaceRepo(), pkg);
						wsName = p.getName();
					} catch (Exception e) {
						if (!loggedWorkspaceError) {
							logger.logError("Error trying to fetch the repository for the current workspace", e);
							loggedWorkspaceError = true;
						}
					}
				}
				if (caps == null) {
					continue;
				}
				for (Capability cap : caps) {
					final String bsn = capabilityToBSN(cap);
					if (bsn != null && !handler.containsBundle(bsn)) {
						bundles.add(bsn, p.getName());
					}
				}
			}
		}
		if (bundles.isEmpty()) {
			return null;
		}
		IJavaCompletionProposal[] retval = new IJavaCompletionProposal[bundles.size()];
		int i = 0;
		for (Map.Entry<String, List<String>> bundle : bundles.entrySet()) {
			// NOTE: The call to contains() here, based on the current MultiMap
			// implementation, will
			// do a linear search. Because a single bundle is unlikely to be
			// found in more than a few
			// repos in any given workspace this probably won't make a
			// difference, but if any performance issues show up
			// in the future this would be a place to look.
			final int relevance = bundle.getValue()
				.contains(wsName) ? ADD_BUNDLE_WORKSPACE : ADD_BUNDLE;
			retval[i++] = new AddBundleCompletionProposal(bundle.getKey(), bundle.getValue(), relevance, context);
		}
		return retval;
	}

	private Name getPackageFromImportNotFound(IInvocationContext context, IProblemLocation location) {
		ASTNode selectedNode = location.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return null;
		}
		ImportDeclaration declaration = (ImportDeclaration) ((selectedNode instanceof ImportDeclaration) ? selectedNode
			: ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION));
		if (declaration == null) {
			return null;
		}

		Name name = declaration.getName();

		if (!name.isQualifiedName()) {
			if (!declaration.isOnDemand() || declaration.isStatic()) {
				return null;
			}
		} else if (declaration.isStatic()) {
			final QualifiedName qName = (QualifiedName) name;
			name = qName.getQualifier();
			if (declaration.isOnDemand()) {
				// return name;
			} else if (name.isQualifiedName()) {
				name = ((QualifiedName) name).getQualifier();
			} else {
				return null;
			}
		} else if (!declaration.isOnDemand()) {
			name = ((QualifiedName) name).getQualifier();
		}

		return skipClasses(name);
	}

	// Recurse through the elements of a qualified name from right to left,
	// skipping elements
	// that start with a capital letter. Eg, "my.pkg.Clazz" would become
	// "my.pkg". This is a heuristic to guess the
	// package name as opposed to a type name.
	private Name skipClasses(Name name) {
		while (name instanceof QualifiedName && Character.isUpperCase(((QualifiedName) name).getName()
			.getIdentifier()
			.charAt(0))) {
			name = ((QualifiedName) name).getQualifier();
		}
		return name;
	}

	private Name getPackageFromUndefinedType(IInvocationContext context, IProblemLocation location) {

		ASTNode selectedNode = location.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return null;
		}
		while (selectedNode instanceof Name) {
			selectedNode = selectedNode.getParent();
		}
		Name name = null;
		if (selectedNode instanceof SimpleType) {
			name = ((SimpleType) selectedNode).getName();
			if (!name.isQualifiedName()) {
				return null;
			}
			name = ((QualifiedName) name).getQualifier();
		} else if (selectedNode instanceof NameQualifiedType) {
			name = ((NameQualifiedType) selectedNode).getQualifier();
		}

		if (name == null) {
			return null;
		}

		return skipClasses(name);
	}

	private Name getPackageFromIsClassPathCorrect(IInvocationContext context, IProblemLocation location) {
		String[] args = location.getProblemArguments();
		if (args == null || args.length == 0) {
			return null;
		}
		try {
			Name name = context.getASTRoot()
				.getAST()
				.newName(args[0]);

			if (name.isSimpleName()) {
				return null;
			}

			return skipClasses(((QualifiedName) name).getQualifier());
		} catch (IllegalArgumentException e) {
			logger.logWarning(Strings.format("Illegal type '%s'", args[0]), e);
			return null;
		}
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {
		Set<String> pkgs = new HashSet<>(locations.length * 2 + 1);

		for (IProblemLocation location : locations) {
			Name name;
			switch (location.getProblemId()) {
				case IProblem.IsClassPathCorrect :
					name = getPackageFromIsClassPathCorrect(context, location);
					break;
				case IProblem.ImportNotFound :
					name = getPackageFromImportNotFound(context, location);
					break;
				case IProblem.UndefinedType :
					name = getPackageFromUndefinedType(context, location);
					break;
				default :
					continue;
			}

			if (name == null) {
				continue;
			}

			final String pkg = name.getFullyQualifiedName();
			// Don't suggest adding a bundle to fix missing package in import if
			// current Compilation Unit
			// is already part of that package.
			final PackageDeclaration ourPkg = context.getASTRoot()
				.getPackage();
			if (ourPkg != null && pkg.equals(ourPkg.getName()
				.getFullyQualifiedName())) {
				continue;
			}
			pkgs.add(pkg);
		}
		if (pkgs.isEmpty()) {
			return null;
		}
		return getSuggestions(pkgs, context);
	}

	/**
	 * Converts a package capability into a bundle symbolic name.
	 *
	 * @param cap The capability object describing the package being imported.
	 * @return A user-friendly string with the BSN.
	 */
	public static String capabilityToBSN(Capability cap) {
		Resource res = cap.getResource();
		// Guard against badly behaved repos
		if (res == null) {
			logger.logWarning("Didn't find bundle name for capability " + cap, null);
			return null;
		}
		List<Capability> caps = cap.getResource()
			.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);

		if (!caps.isEmpty()) {
			cap = caps.get(0);
			final Map<String, Object> attributes = cap.getAttributes();
			Object value = attributes.get(IdentityNamespace.IDENTITY_NAMESPACE);
			if (value != null) {
				return value.toString();
			}
		}
		caps = cap.getResource()
			.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		if (!caps.isEmpty()) {
			cap = caps.get(0);
			final Map<String, Object> attributes = cap.getAttributes();
			Object value = attributes.get(BundleNamespace.BUNDLE_NAMESPACE);
			if (value != null) {
				return value.toString();
			}
		}
		logger.logWarning("Didn't find bundle name for capability " + cap, null);
		return null;
	}
}
