package org.bndtools.build.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;

public abstract class AbstractBuildErrorDetailsHandler implements BuildErrorDetailsHandler {

	private static final Map<Code, String> PRIMITIVES_TO_SIGNATURES;

	static {
		Map<Code, String> tmp = new HashMap<>();

		tmp.put(PrimitiveType.VOID, "V");
		tmp.put(PrimitiveType.BOOLEAN, "Z");
		tmp.put(PrimitiveType.BYTE, "B");
		tmp.put(PrimitiveType.SHORT, "S");
		tmp.put(PrimitiveType.CHAR, "C");
		tmp.put(PrimitiveType.INT, "I");
		tmp.put(PrimitiveType.FLOAT, "F");
		tmp.put(PrimitiveType.LONG, "J");
		tmp.put(PrimitiveType.DOUBLE, "D");

		PRIMITIVES_TO_SIGNATURES = Collections.unmodifiableMap(tmp);
	}

	public static final IResource getDefaultResource(IProject project) {
		return getDefaultResource(project, Project.BNDFILE);
	}

	public static final IResource getDefaultResource(IProject project, String name) {
		if ((name == null) || name.isEmpty())
			return project;
		IResource bndFile = project.getFile(name);
		if (bndFile.exists())
			return bndFile;
		return project;
	}

	/**
	 * Obtain an AST for a source file in a project
	 *
	 * @param javaProject
	 * @param className
	 * @return An AST, or null if no source file exists for that class
	 * @throws JavaModelException
	 */
	public static final CompilationUnit createAST(IJavaProject javaProject, String className)
		throws JavaModelException {
		IType type = javaProject.findType(className);
		if (type == null)
			return null;

		final ICompilationUnit cunit = type.getCompilationUnit();
		if (cunit == null)
			return null; // not a source type

		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(cunit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	/**
	 * Create a marker on a Java Type
	 *
	 * @param javaProject
	 * @param className - the fully qualified class name (e.g java.lang.String)
	 * @param markerAttributes
	 * @param hasResolutions - true if the marker will have resolutions
	 * @return Marker Data that can be used to create an {@link IMarker}, or
	 *         null if no location can be found
	 * @throws JavaModelException
	 */
	public static final MarkerData createTypeMarkerData(IJavaProject javaProject, final String className,
		final Map<String, Object> markerAttributes, boolean hasResolutions) throws JavaModelException {

		final CompilationUnit ast = createAST(javaProject, className);

		if (ast == null)
			return null;

		ast.accept(new ASTVisitor() {
			@Override
			public boolean visit(TypeDeclaration typeDecl) {
				ITypeBinding typeBinding = typeDecl.resolveBinding();
				if (typeBinding != null) {
					if (typeBinding.getBinaryName()
						.equals(className)) {
						SimpleName nameNode = typeDecl.getName();
						markerAttributes.put(IMarker.CHAR_START, nameNode.getStartPosition());
						markerAttributes.put(IMarker.CHAR_END, nameNode.getStartPosition() + nameNode.getLength());

						return false;
					}
				}
				return true;
			}
		});

		if (!markerAttributes.containsKey(IMarker.CHAR_START))
			return null;

		return new MarkerData(ast.getJavaElement()
			.getResource(), markerAttributes, hasResolutions);
	}

	/**
	 * Create a marker on a Java Method
	 *
	 * @param javaProject
	 * @param className - the fully qualified class name (e.g java.lang.String)
	 * @param methodName
	 * @param methodSignature - signatures are in "internal form" e.g.
	 *            (Ljava.lang.Integer;[Ljava/lang/String;Z)V
	 * @param markerAttributes - attributes that should be included in the
	 *            marker, typically a message. The start and end points for the
	 *            marker are added by this method.
	 * @param hasResolutions - true if the marker will have resolutions
	 * @return Marker Data that can be used to create an {@link IMarker}, or
	 *         null if no location can be found
	 * @throws JavaModelException
	 */
	public static final MarkerData createMethodMarkerData(IJavaProject javaProject, final String className,
		final String methodName, final String methodSignature, final Map<String, Object> markerAttributes,
		boolean hasResolutions) throws JavaModelException {

		final CompilationUnit ast = createAST(javaProject, className);

		if (ast == null)
			return null;

		ast.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration methodDecl) {
				if (matches(ast, methodDecl, methodName, methodSignature)) {
					// Create the marker attribs here
					markerAttributes.put(IMarker.CHAR_START, methodDecl.getStartPosition());
					markerAttributes.put(IMarker.CHAR_END, methodDecl.getStartPosition() + methodDecl.getLength());
				}

				return false;
			}

			private boolean matches(CompilationUnit ast, MethodDeclaration methodDecl, String methodName,
				String signature) {
				if ("<init>".equals(methodName)) {
					if (!methodDecl.isConstructor()) {
						return false;
					}
				} else if (!methodDecl.getName()
					.getIdentifier()
					.equals(methodName)) {
					return false;
				}

				return getSignature(ast, methodDecl).equals(signature);
			}

			private String getSignature(CompilationUnit ast, MethodDeclaration methodDecl) {
				StringBuilder signatureBuilder = new StringBuilder("(");

				for (@SuppressWarnings("unchecked")
				Iterator<SingleVariableDeclaration> it = methodDecl.parameters()
					.iterator(); it.hasNext();) {
					SingleVariableDeclaration decl = it.next();
					appendType(ast, signatureBuilder, decl.getType(), decl.getExtraDimensions());
				}

				signatureBuilder.append(")");

				appendType(ast, signatureBuilder, methodDecl.getReturnType2(), 0);
				return signatureBuilder.toString();
			}

			private void appendType(CompilationUnit ast, StringBuilder signatureBuilder, Type typeToAdd,
				int extraDimensions) {
				for (int i = 0; i < extraDimensions; i++) {
					signatureBuilder.append('[');
				}
				Type rovingType = typeToAdd;
				if (rovingType == null) {
					// A special return type for constructors, nice one
					// Eclipse...
					signatureBuilder.append("V");
				} else {
					if (rovingType.isArrayType()) {
						ArrayType type = (ArrayType) rovingType;
						int depth = type.getDimensions();
						for (int i = 0; i < depth; i++) {
							signatureBuilder.append('[');
						}
						// We still need to add the array component type, which
						// might be primitive or a reference
						rovingType = type.getElementType();
					}
					// Type erasure means that we should ignore parameters
					if (rovingType.isParameterizedType()) {
						rovingType = ((ParameterizedType) rovingType).getType();
					}

					if (rovingType.isPrimitiveType()) {
						PrimitiveType type = (PrimitiveType) rovingType;
						signatureBuilder.append(PRIMITIVES_TO_SIGNATURES.get(type.getPrimitiveTypeCode()));
					} else if (rovingType.isSimpleType()) {
						SimpleType type = (SimpleType) rovingType;
						String name;
						if (type.getName()
							.isQualifiedName()) {
							name = type.getName()
								.getFullyQualifiedName();
						} else {
							name = getFullyQualifiedNameForSimpleName(ast, type.getName());
						}
						name = name.replace('.', '/');
						signatureBuilder.append("L")
							.append(name)
							.append(";");
					} else if (rovingType.isQualifiedType()) {
						QualifiedType type = (QualifiedType) rovingType;
						String name = type.getQualifier()
							.toString()
							.replace('.', '/') + '/'
							+ type.getName()
								.getFullyQualifiedName()
								.replace('.', '/');
						signatureBuilder.append("L")
							.append(name)
							.append(";");
					} else {
						throw new IllegalArgumentException("We hit an unknown type " + rovingType);
					}
				}
			}

			private String getFullyQualifiedNameForSimpleName(CompilationUnit ast, Name typeName) {
				String name = typeName.getFullyQualifiedName();

				@SuppressWarnings("unchecked")
				List<ImportDeclaration> ids = ast.imports();
				for (ImportDeclaration id : ids) {
					if (id.isStatic())
						continue;
					if (id.isOnDemand()) {
						String packageName = id.getName()
							.getFullyQualifiedName();
						try {
							if (ast.getJavaElement()
								.getJavaProject()
								.findType(packageName + "." + name) != null) {
								name = packageName + '.' + name;
							}
						} catch (JavaModelException e) {}
					} else {
						String importName = id.getName()
							.getFullyQualifiedName();
						if (importName.endsWith("." + name)) {
							name = importName;
							break;
						}
					}
				}

				if (name.indexOf('.') < 0) {
					try {
						if (ast.getJavaElement()
							.getJavaProject()
							.findType(name) == null) {
							name = "java.lang." + name;
						}
					} catch (JavaModelException e) {}
				}
				return name;
			}
		});

		if (!markerAttributes.containsKey(IMarker.CHAR_START))
			return null;

		return new MarkerData(ast.getJavaElement()
			.getResource(), markerAttributes, hasResolutions);
	}

	/**
	 * Create a marker on a Java Method
	 *
	 * @param javaProject
	 * @param className - the fully qualified class name (e.g java.lang.String)
	 * @param markerAttributes - attributes that should be included in the
	 *            marker, typically a message. The start and end points for the
	 *            marker are added by this method.
	 * @param hasResolutions - true if the marker will have resolutions
	 * @return Marker Data that can be used to create an {@link IMarker}, or
	 *         null if no location can be found
	 * @throws JavaModelException
	 */
	public static final MarkerData createFieldMarkerData(IJavaProject javaProject, final String className,
		final String fieldName, final Map<String, Object> markerAttributes, boolean hasResolutions)
		throws JavaModelException {

		final CompilationUnit ast = createAST(javaProject, className);

		if (ast == null)
			return null;

		ast.accept(new ASTVisitor() {
			@Override
			public boolean visit(FieldDeclaration fieldDecl) {
				if (matches(ast, fieldDecl, fieldName)) {
					// Create the marker attribs here
					markerAttributes.put(IMarker.CHAR_START, fieldDecl.getStartPosition());
					markerAttributes.put(IMarker.CHAR_END, fieldDecl.getStartPosition() + fieldDecl.getLength());
				}

				return false;
			}

			private boolean matches(@SuppressWarnings("unused") CompilationUnit ast, FieldDeclaration fieldDecl,
				String fieldName) {
				@SuppressWarnings("unchecked")
				List<VariableDeclarationFragment> list = (List<VariableDeclarationFragment>) fieldDecl
					.getStructuralProperty(FieldDeclaration.FRAGMENTS_PROPERTY);
				for (VariableDeclarationFragment vdf : list) {
					if (fieldName.equals(vdf.getName()
						.toString())) {
						return true;
					}
				}
				return false;
			}
		});

		if (!markerAttributes.containsKey(IMarker.CHAR_START))
			return null;

		return new MarkerData(ast.getJavaElement()
			.getResource(), markerAttributes, hasResolutions);
	}

	@Override
	public List<IMarkerResolution> getResolutions(IMarker marker) {
		return Collections.emptyList();
	}

	@Override
	public List<ICompletionProposal> getProposals(IMarker marker) {
		return Collections.emptyList();
	}

	/**
	 * Bridge method. The actual parameter should have been Processor since we
	 * can have Builder, Workspace, Project. etc. If it is a project, we defer
	 * to the old method. Otherwise we allow others to override this method.
	 */
	@Override
	public List<MarkerData> generateMarkerData(IProject project, Processor model, Location location) throws Exception {
		if (model instanceof Project)
			return generateMarkerData(project, (Project) model, location);

		return Collections.emptyList();
	}

	@Override
	public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
		return Collections.emptyList();
	}

}
