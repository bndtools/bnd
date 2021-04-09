package org.bndtools.core.editors.quickfix;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.result.Result;
import aQute.lib.exceptions.Exceptions;
import bndtools.central.Central;

@Component
public class BuildpathQuickFixProcessor implements IQuickFixProcessor {

	// Useful for giving pretty debug output for problems; if you come across
	// a new problem you can uncomment this and uncomment the println in
	// the default case of hasProblems() to get a human-readable problem
	// type description.
	// static IProblem getProblem(java.lang.reflect.Field f) {
	// try {
	// int problemId = f.getInt(null);
	// return new DummyProblem(problemId, f.getName());
	// } catch (Exception e) {
	// throw Exceptions.duck(e);
	// }
	// }
	//
	// static Map<Integer, IProblem> PROBLEM_TYPES;
	//
	// {
	// PROBLEM_TYPES = new HashMap<>();
	// Stream.of(IProblem.class.getFields())
	// .map(BuildpathQuickFixProcessor::getProblem)
	// .forEach(problem -> {
	// PROBLEM_TYPES.put(problem.getID(), problem);
	// });
	// }
	//
	// // This is just to give nice error feedback
	// static class DummyProblem extends DefaultProblem {
	// public DummyProblem(int id, String message) {
	// super(null, message, id, null, 0, 0, 0, 0, 0);
	// }
	// }
	//
	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		// System.err.println(PROBLEM_TYPES.get(problemId));
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
			case IProblem.TypeArgumentMismatch :
				// System.out.println("TypeArgumentMismatch");
				return true;
			case IProblem.TypeMismatch :
				// System.out.println("TypeMismatch");
				return true;
			case IProblem.UndefinedConstructor :
				// System.out.println("UndefinedConstructor");
				return true;
			case IProblem.UndefinedField :
				// System.out.println("UndefinedField");
				return true;
			case IProblem.UndefinedMethod :
				// System.out.println("UndefinedMethod");
				return true;
			case IProblem.UndefinedType :
				// System.out.println("UndefinedType");
				return true;
			case IProblem.UndefinedName :
				// System.out.println("UndefinedName");
				return true;
			case IProblem.UnresolvedVariable :
				// System.out.println("UnresolvedVariable");
				return true;
			default :
				// System.err.println("Unhandled problem type: " + problemId);
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

	void visitExpressionHierarchy(Expression expression) {
		if (expression == null) {
			return;
		}
		visitBindingHierarchy(expression.resolveTypeBinding());
	}

	/**
	 * Traverses the hierarchy of the given type binding looking for incomplete
	 * types that might be the cause of a "hierarchy is inconsistent" error.
	 *
	 * @param binding the type binding corresponding to the type with the
	 *            inconsistent hierarchy.
	 */
	void visitBindingHierarchy(ITypeBinding binding) {
		try {
			if (binding == null) {
				return;
			}
			String qualifiedName = binding.getQualifiedName();

			if (qualifiedName.startsWith("java.")) {
				return;
			}
			// Prevent infinite recursion
			if (!visited.add(binding)) {
				return;
			}
			// A "recovered" type binding indicates the type has not been
			// fully resolved - usually because it's not on the classpath.
			if (binding.isRecovered()) {
				addProposals(binding);
			}
			visitBindingHierarchy(binding.getSuperclass());
			for (ITypeBinding iface : binding.getInterfaces()) {
				visitBindingHierarchy(iface);
			}
			for (ITypeBinding typeParam : binding.getTypeArguments()) {
				visitBindingHierarchy(typeParam);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void visitType(Type node) {
		if (node == null) {
			return;
		}
		visitBindingHierarchy(node.resolveBinding());
	}

	void visitTypeDeclaration(TypeDeclaration node) {
		visitType(node.getSuperclassType());
		@SuppressWarnings("unchecked")
		List<Type> superInterfaceTypes = node.superInterfaceTypes();
		for (Type iface : superInterfaceTypes) {
			visitType(iface);
		}
	}

	void visitEnclosingTypeDeclaration(ASTNode node) {
		if (node == null) {
			return;
		}
		TypeDeclaration td = findFirstParentOfType(node, TypeDeclaration.class);
		if (td != null) {
			visitTypeDeclaration(td);
			visitEnclosingTypeDeclaration(td.getParent());
		}
	}

	void visitNodeAncestry(ASTNode node) throws Exception {
		while (node != null) {
			if (node instanceof Type) {
				visitType((Type) node);
				break;
			}
			if (node instanceof TypeDeclaration) {
				visitTypeDeclaration((TypeDeclaration) node);
				break;
			}
			node = node.getParent();
		}
	}

	<N extends ASTNode> N findFirstParentOfType(ASTNode node, Class<N> type) {
		while (node != null) {
			if (type.isAssignableFrom(node.getClass())) {
				@SuppressWarnings("unchecked")
				N retval = (N) node;
				return retval;
			}
			node = node.getParent();
		}
		return null;
	}

	Project								project;
	IInvocationContext					context;
	private boolean						test;
	private Workspace					workspace;
	Map<BundleId, Map<String, Boolean>>	proposals;
	Set<ITypeBinding>					visited;
	IProblemLocation					location;
	final ASTVisitor					TYPE_VISITOR	= new ASTVisitor() {
															@Override
															public void preVisit(ASTNode node) {
																if (node instanceof Type) {
																	ITypeBinding binding = ((Type) node)
																		.resolveBinding();
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
		// System.err.println("getCorrections: " + Stream.of(locations)
		// .map(IProblemLocation::getProblemId)
		// .map(PROBLEM_TYPES::get)
		// .map(Object::toString)
		// .collect(Collectors.joining("\n")));
		try {
			this.context = context;
			proposals = new HashMap<>();
			visited = new HashSet<>();

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
					case IProblem.TypeArgumentMismatch :
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
					case IProblem.UndefinedConstructor : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						// FIXME: This one probably shouldn't traverse down the
						// inheritance hierarchy - it should only check the
						// immediate superclass, as constructors are not
						// virtual.
						visitEnclosingTypeDeclaration(node);
						continue;
					}
					case IProblem.UndefinedField : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						ASTNode parent = node.getParent();
						if (parent != null) {
							switch (parent.getNodeType()) {
								case ASTNode.QUALIFIED_NAME : {
									QualifiedName n = (QualifiedName) parent;
									IBinding binding = n.getQualifier()
										.resolveBinding();
									if (binding instanceof IVariableBinding) {
										visitBindingHierarchy(((IVariableBinding) binding).getType());
									}
									break;
								}
								case ASTNode.FIELD_ACCESS : {
									FieldAccess access = (FieldAccess) parent;
									visitExpressionHierarchy(access.getExpression());
									break;
								}
								case ASTNode.SUPER_FIELD_ACCESS : {
									visitEnclosingTypeDeclaration(parent);
									// AbstractTypeDeclaration type =
									// findFirstParentOfType(parent,
									// AbstractTypeDeclaration.class);
									// visitBindingHierarchy(type.resolveBinding());
									break;
								}
							}
						}
						continue;
					}
					case IProblem.UndefinedMethod : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						ASTNode parent = node.getParent();
						UNDEFINED_METHOD: while (parent != null) {
							switch (parent.getNodeType()) {
								case ASTNode.METHOD_INVOCATION : {
									// This is the bit to the left of the "."
									Expression leftOfTheDot = ((MethodInvocation) parent).getExpression();
									if (leftOfTheDot == null) {
										// "this." is implied if there's nothing
										// there
										visitEnclosingTypeDeclaration(parent);
									} else {
										visitExpressionHierarchy(leftOfTheDot);
									}
									break UNDEFINED_METHOD;
								}
								case ASTNode.SUPER_METHOD_INVOCATION : {
									SuperMethodInvocation invocation = (SuperMethodInvocation) parent;
									// Qualifier is if the super class
									// invocation is qualified, eg
									// to disambiguate in the case of multiple
									// inheritance.
									Name qualifier = invocation.getQualifier();
									if (qualifier == null) {
										TypeDeclaration type = findFirstParentOfType(parent, TypeDeclaration.class);
										visitBindingHierarchy(type.getSuperclassType()
											.resolveBinding());
									} else {
										visitBindingHierarchy(qualifier.resolveTypeBinding());
									}
									break UNDEFINED_METHOD;
								}
							}
							parent = parent.getParent();
						}
						continue;
					}
					case IProblem.ParameterMismatch : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						MethodInvocation invocation = findFirstParentOfType(node, MethodInvocation.class);
						@SuppressWarnings("unchecked")
						List<Expression> args = invocation.arguments();
						args.forEach(this::visitExpressionHierarchy);
						visitExpressionHierarchy(invocation.getExpression());
						continue;
					}
					case IProblem.ImportNotFound : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						ImportDeclaration importDec = findFirstParentOfType(node, ImportDeclaration.class);
						if (importDec != null) {
							Name name = importDec.getName();
							if (importDec.isStatic() && !importDec.isOnDemand()) {
								// It should be a QualifiedName unless there is
								// an error in Eclipse...
								if (name instanceof QualifiedName) {
									addProposals(((QualifiedName) name).getQualifier()
										.getFullyQualifiedName());
								}
							} else if (importDec.isOnDemand() || name instanceof QualifiedName) {
								addProposals(name.getFullyQualifiedName());
							}
							// Don't make any suggestions for a SimpleName as it
							// is in the default package, which can't be
							// exported by any bundle.
						}
						continue;
					}
					// An undefined name can be:
					// 1. a static reference to a method of a missing type, eg
					// FrameworkUtil.getBundleContext()
					// if FrameworkUtil is not on the classpath.
					// 2. a reference to the field of a missing superclass
					case IProblem.UndefinedName : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						visitEnclosingTypeDeclaration(node);
						String partialClassName = getPartialClassName(node);
						if (partialClassName != null) {
							addProposals(partialClassName);
						}
						continue;
					}
					// An unresolved variable can be:
					// 1. a static reference to a field of a missing type;
					// 2. a reference to a field of a missing supertype
					case IProblem.UnresolvedVariable : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						visitEnclosingTypeDeclaration(node);
						addProposals(getCoveringName(node));
						continue;
					}
					case IProblem.UndefinedType : {
						ASTNode node = location.getCoveredNode(context.getASTRoot());
						if (node instanceof Name) {
							while (node != null && node instanceof Name) {
								node = node.getParent();
							}
							if (node != null) {
								if (node instanceof Type) {
									addProposalsForType((Type) node);
								} else if (node instanceof Annotation) {
									addProposals(((Annotation) node).getTypeName());
								}
							}
						} else if (node instanceof TypeLiteral) {
							TypeLiteral tl = (TypeLiteral) node;
							addProposalsForType(tl.getType());
							// } else {
							// String[] arguments =
							// location.getProblemArguments();
							// if (arguments != null && arguments.length > 0) {
							// addProposals(arguments[0]);
							// }
						}
						continue;
					}
					case IProblem.IsClassPathCorrect : {
						String partialClassName = null;
						// The original implementation used to query the AST
						// first for the FQN. However, I found that the error
						// can occur on a Name which is a field reference, not
						// only on a Name that is a type reference. Trying to
						// treat such a name as a type name will give the wrong
						// results. On the other hand, the problem argument
						// seems consistently to be the type name, so we try to
						// use that first.
						if (location.getProblemArguments().length > 0) {
							partialClassName = location.getProblemArguments()[0];
						}
						if (partialClassName == null) {
							partialClassName = getPartialClassName(location.getCoveringNode(context.getASTRoot()));
						}
						if (partialClassName != null) {
							addProposals(partialClassName);
						}
						continue;
					}
				}
			}

			if (proposals.isEmpty()) {
				return null;
			}

			Set<String> buildpath = getBundleIds(project.getBuildpath()).stream()
				.map(BundleId::getBsn)
				.collect(Collectors.toSet());
			Set<String> testpath = test ? getBundleIds(project.getTestpath()).stream()
				.map(BundleId::getBsn)
				.collect(Collectors.toSet()) : Collections.emptySet();

			Stream<AddBundleCompletionProposal> results;

			if (test) {
				Set<String> testBundlesAdded = new HashSet<>();
				results = proposals.entrySet()
					.stream()
					.filter(entry -> !testpath.contains(entry.getKey()
						.getBsn()))
					.filter(entry -> testBundlesAdded.add(entry.getKey()
						.getBsn()))
					.map(
						entry -> new AddBundleCompletionProposal(entry.getKey(), entry.getValue(), 15 + entry.getValue()
							.size(), context, project, Constants.TESTPATH));
			} else {
				results = Stream.empty();
			}

			Set<String> bundlesAdded = new HashSet<>();
			IJavaCompletionProposal[] retval = Stream.concat(results, proposals.entrySet()
				.stream()
				.filter(entry -> !buildpath.contains(entry.getKey()
					.getBsn()))
				.filter(entry -> bundlesAdded.add(entry.getKey()
					.getBsn()))
				.map(entry -> new AddBundleCompletionProposal(entry.getKey(), entry.getValue(), 14 + entry.getValue()
					.size(), context, project, Constants.BUILDPATH)))
				.toArray(IJavaCompletionProposal[]::new);

			return retval.length == 0 ? null : retval;
		} catch (

		Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void addProposalsForType(Type type) throws CoreException, Exception {
		if (type == null) {
			return;
		}
		switch (type.getNodeType()) {
			case ASTNode.NAME_QUALIFIED_TYPE :
				NameQualifiedType nqt = (NameQualifiedType) type;
				addProposals(nqt.getQualifier()
					.getFullyQualifiedName()
					.toString(),
					nqt.getName()
						.toString());
				addProposals(nqt.getQualifier());
				return;
			case ASTNode.QUALIFIED_TYPE :
				QualifiedType qt = (QualifiedType) type;
				addProposalsForType(qt.getQualifier());
				return;
			case ASTNode.SIMPLE_TYPE :
				SimpleType st = (SimpleType) type;
				addProposals(st.getName());
		}
	}

	private void addProposals(ITypeBinding typeBinding) throws CoreException, Exception {
		if (typeBinding == null) {
			throw new NullPointerException();
		}
		final ITypeBinding erasure = typeBinding.getErasure();
		final StringBuilder className = new StringBuilder(128);
		getClassName(erasure, className);
		if (erasure.getPackage() != null) {
			final String packageName = erasure.getPackage()
				.getName();
			// If it is a reference to an undefined type that belongs to the
			// current package, then chances are it is *actually* an unqualified
			// reference to a class that has not been imported or is not on the
			// classpath. In this case, we skip the package filter and fall
			// through to the search based on class name only.
			//
			// This implementation is not perfect. It is possible that the
			// source may include a fully-qualified reference to the class in
			// the same package, in which case we should include the package in
			// the search (or maybe even skip the search altogether, as it
			// should already be on the build path). An optimisation for another
			// day.
			//
			// Note that ICompilationUnit.getPackageDeclaration(String) doesn't
			// quite seem to behave as advertised - it can actually create a
			// package definitition. Hence this alternative that streams over
			// the available package declarations instead.
			if (!Stream.of(context.getCompilationUnit()
				.getPackageDeclarations())
				.filter(Objects::nonNull)
				.map(IPackageDeclaration::getElementName)
				.anyMatch(packageName::equals)) {
				doAddProposals(workspace.search(packageName, className.toString()), true);
				return;
			}
		}
		addProposals(className.toString());
	}

	private void getClassName(ITypeBinding typeBinding, StringBuilder buffer) {
		ITypeBinding parent = typeBinding.getDeclaringClass();
		if (parent != null) {
			getClassName(parent, buffer);
			buffer.append('.');
		}
		buffer.append(typeBinding.getName());
	}

	private void addProposals(Name name) throws CoreException, Exception {
		if (name == null) {
			return;
		}
		if (name instanceof SimpleName) {
			addProposals(null, name.toString());
		} else if (name instanceof QualifiedName) {
			QualifiedName qualified = (QualifiedName) name;
			Name qualifier = qualified.getQualifier();
			addProposals(qualifier.toString(), qualified.getName()
				.toString());
			addProposals(qualifier);
		}
	}

	private void addProposals(String partialClassName) throws CoreException, Exception {
		boolean doImport = Descriptors.determine(partialClassName)
			.map(sa -> sa[0] == null)
			.orElse(false);
		doAddProposals(workspace.search(partialClassName), doImport);
	}

	private void addProposals(String packageName, String className) throws CoreException, Exception {
		doAddProposals(workspace.search(packageName, className), packageName == null || packageName.length() == 0);
	}

	private void doAddProposals(Result<Map<String, List<BundleId>>, String> wrappedResult, boolean doImport)
		throws CoreException, Exception {
		try (ProjectBuilder pb = new ProjectBuilder(project)) {

			if (test)
				pb.includeTestpath();

			Map<String, List<BundleId>> result = wrappedResult
				.orElseThrow(s -> new CoreException(new Status(IStatus.ERROR, "bndtools.core.services", s)));

			result.entrySet()
				.stream()
				.filter(e -> !isOnBuildOrTestPath(pb, e.getKey()))
				.forEach(e -> {
					for (BundleId id : e.getValue()) {
						proposals.computeIfAbsent(id, newBundleId -> new HashMap<>())
							.merge(e.getKey(), doImport, (oldVal, newVal) -> oldVal || newVal);
					}
				});
		}
	}

	private boolean isOnBuildOrTestPath(ProjectBuilder pb, String fqn) {
		try {
			TypeRef type = pb.getTypeRefFromFQN(fqn);
			if (type == null)
				return false;
			return pb.findResource(type.getPath()) != null;
		} catch (Exception e1) {
			return false;
		}
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

	private Name getCoveringName(ASTNode node) {
		Name name = null;
		while (node instanceof Name) {
			name = (Name) node;
			node = node.getParent();
		}

		return name;
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
		Name name = getCoveringName(node);
		return name == null ? null : name.toString();
	}
}
