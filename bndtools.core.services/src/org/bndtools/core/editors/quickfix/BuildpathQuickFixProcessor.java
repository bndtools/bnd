package org.bndtools.core.editors.quickfix;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Descriptors;
import aQute.lib.exceptions.Exceptions;
import bndtools.central.Central;

@Component
public class BuildpathQuickFixProcessor implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.HierarchyHasProblems :
				// System.out.println("HierarchyHasProblems");
				return true;
			case IProblem.IsClassPathCorrect :
				// System.out.println("IsClassPathCorrect");
				return true;
			case IProblem.ImportNotFound :
				// System.out.println("ImportNotFound");
				return true;
			case IProblem.ParameterMismatch :
				// System.out.println("ParameterMismatch");
				return true;
			case IProblem.TypeMismatch :
				// System.out.println("TypeMismatch");
				return true;
			case IProblem.UndefinedType :
				// System.out.println("UndefinedType");
				return true;
			default :
				return false;
		}
	}

	// void dumpBindingHierarchy(ITypeBinding binding) throws Exception {
	// dumpBindingHierarchy("", binding);
	// }
	//
	// void dumpBindingHierarchy(String indent, ITypeBinding binding) throws
	// Exception {
	// if (binding == null) {
	// System.err.println("type: <null>");
	// return;
	// }
	// System.err.println(indent + "type: " + binding.getQualifiedName());
	// System.err.println(indent + "complete?: " + binding.isRecovered());
	// if (binding.isRecovered()) {
	// addProposals(binding.getQualifiedName());
	// }
	// System.err.println(indent + "javaElement: " + binding.getJavaElement());
	// System.err.println(indent + "superclass: ");
	// dumpBindingHierarchy(indent + " ", binding.getSuperclass());
	// System.err.println(indent + "implemented interfaces: ");
	// for (ITypeBinding iface : binding.getInterfaces()) {
	// dumpBindingHierarchy(indent + " ", iface);
	// }
	// }

	/**
	 * Traverses the hierarchy of the given type binding looking for incomplete
	 * types that might be the cause of a "hierarchy is inconsistent" error.
	 *
	 * @param binding the type binding corresponding to the type with the
	 *            inconsistent hierarchy.
	 */
	void visitBindingHierarchy(ITypeBinding binding) {
		try {
			if (binding == null || binding.getQualifiedName()
				.startsWith("java.")) {
				return;
			}
			// A "recovered" type binding indicates the type has not been
			// fully resolved - usually because it's not on the classpath.
			if (binding.isRecovered()) {
				addProposals(binding.getQualifiedName());
			}
			visitBindingHierarchy(binding.getSuperclass());
			for (ITypeBinding iface : binding.getInterfaces()) {
				visitBindingHierarchy(iface);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void visitNodeAncestry(ASTNode node) throws Exception {
		while (node != null) {
			if (node instanceof Type) {
				visitBindingHierarchy(((Type) node).resolveBinding());
				break;
			}
			if (node instanceof AbstractTypeDeclaration) {
				visitBindingHierarchy(((AbstractTypeDeclaration) node).resolveBinding());
				break;
			}
			node = node.getParent();
		}
	}

	@SuppressWarnings("unchecked")
	<N extends ASTNode> N findFirstParentOfType(ASTNode node, Class<N> type) {
		while (node != null) {
			if (type.isAssignableFrom(node.getClass())) {
				return (N) node;
			}
			node = node.getParent();
		}
		return null;
	}

	Project							project;
	IInvocationContext				context;
	private boolean					test;
	private Workspace				workspace;
	List<IJavaCompletionProposal>	proposals;
	IProblemLocation				location;
	final ASTVisitor				TYPE_VISITOR	= new ASTVisitor() {
														@Override
														public void preVisit(ASTNode node) {
															if (node instanceof Type) {
																ITypeBinding binding = ((Type) node).resolveBinding();
																try {
																	visitBindingHierarchy(binding);
																} catch (Exception e) {
																	throw Exceptions.duck(e);
																}
															}
														}
													};

	// This implementation is not thread safe. I'm fairly sure that Eclipse will
	// not call this from multiple threads at the same time so that should be
	// ok.
	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {
		try {
			this.context = context;
			proposals = new ArrayList<>();

			ICompilationUnit compUnit = context.getCompilationUnit();
			IJavaProject java = compUnit.getJavaProject();
			if (java == null)
				return null;

			project = Central.getProject(java.getProject());
			if (project == null)
				return null;

			test = isInDir(project.getTestSrc(), compUnit.getResource());
			workspace = project.getWorkspace();

			for (IProblemLocation location : locations) {
				this.location = location;
				switch (location.getProblemId()) {
					case IProblem.HierarchyHasProblems : {
						// This error often doesn't directly give us information
						// as
						// to the cause of the problem. It is caused when you
						// reference a type that is on the classpath, but which
						// depends on another type which is not on the
						// classpath.
						// Traverse the type hierarchy looking for incomplete
						// bindings which indicate types not on the class path,
						// and
						// add those as suggestions.
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						visitNodeAncestry(node);
						continue;
					}
					case IProblem.TypeMismatch : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						node.accept(TYPE_VISITOR);
						continue;
					}
					case IProblem.ParameterMismatch : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						MethodInvocation invocation = findFirstParentOfType(node, MethodInvocation.class);
						List<Expression> args = invocation.arguments();
						args.forEach(arg -> visitBindingHierarchy(arg.resolveTypeBinding()));
						continue;
					}
					case IProblem.ImportNotFound : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						ImportDeclaration importDec = findFirstParentOfType(node, ImportDeclaration.class);
						if (importDec != null) {
							Name name = importDec.getName();
							String partialClassName = null;
							if (importDec.isStatic() && !importDec.isOnDemand()) {
								// It should be a QualifiedName unless there is
								// an error in Eclipse...
								if (name instanceof QualifiedName) {
									partialClassName = ((QualifiedName) name).getQualifier()
										.getFullyQualifiedName();
								}
							} else {
								partialClassName = name.getFullyQualifiedName();
							}
							if (partialClassName != null) {
								addProposals(partialClassName);
							}
						}
						continue;
					}
					case IProblem.IsClassPathCorrect :
					case IProblem.UndefinedType : {
						String partialClassName = getPartialClassName(location.getCoveringNode(context.getASTRoot()));
						if (partialClassName == null && location.getProblemArguments().length > 0)
							partialClassName = location.getProblemArguments()[0];

						if (partialClassName == null)
							continue;

						addProposals(partialClassName);
						continue;
					}
				}
			}
			return proposals.isEmpty() ? null : proposals.toArray(new IJavaCompletionProposal[0]);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void addProposals(String partialClassName) throws CoreException, Exception {
		boolean doImport = Descriptors.determine(partialClassName)
			.map(sa -> sa[0] == null)
			.orElse(false);

		Map<String, List<BundleId>> result = workspace.search(partialClassName)
			.orElseThrow(s -> new CoreException(new Status(IStatus.ERROR, "bndtools.core.services", s)));

		Set<BundleId> buildpath = getBundleIds(project.getBuildpath());
		Set<BundleId> testpath = test ? getBundleIds(project.getTestpath()) : Collections.emptySet();

		result.entrySet()
			.forEach(e -> {
				for (BundleId id : e.getValue()) {

					if (test && !testpath.contains(id) && !buildpath.contains(id))
						proposals.add(propose(e.getKey(), id, context, location, project, "-testpath", doImport));

					if (!buildpath.contains(id))
						proposals.add(propose(e.getKey(), id, context, location, project, "-buildpath", doImport));

				}
			});
	}

	private boolean isInDir(File dir, IResource resource) {
		if (resource == null || dir == null)
			return false;

		IPath location = resource.getLocation();
		if (location != null) {
			File file = location.toFile();
			return isInDir(dir, file);
		}
		return false;
	}

	private boolean isInDir(File dir, File file) {
		Path d = dir.toPath();
		Path f = file.toPath();
		return f.startsWith(d);
	}

	private Set<BundleId> getBundleIds(Collection<Container> collection) throws Exception {
		return collection.stream()
			.filter(c -> c.getError() == null)
			.map(Container::getBundleId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	private IJavaCompletionProposal propose(String proposalString, BundleId bundle, IInvocationContext context,
		IProblemLocation location, Project project, String type, boolean doImport) {

		return new AddBundleCompletionProposal(proposalString, bundle, 15, context, project, type, doImport);
	}

	/**
	 * Find out the fully qualified name of either a package or a type or an
	 * import declaration
	 *
	 * @param node the AST node that is causing the problem
	 * @return a partial fully qualified class name or null if this cannot be
	 *         established
	 */
	private String getPartialClassName(ASTNode node) {
		Name name = null;
		while (node instanceof Name) {
			name = (Name) node;
			node = node.getParent();
		}

		if (name == null)
			return null;
		return name.toString();
	}
}
