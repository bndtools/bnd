package org.bndtools.build.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.build.Project;

public abstract class AbstractBuildErrorDetailsHandler implements BuildErrorDetailsHandler {

    private static final Map<Code,String> PRIMITIVES_TO_SIGNATURES;

    static {
        Map<Code,String> tmp = new HashMap<PrimitiveType.Code,String>();

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
        IResource resource;
        IFile bndFile = project.getFile(Project.BNDFILE);
        if (bndFile == null || !bndFile.exists())
            resource = project;
        else
            resource = bndFile;
        return resource;
    }

    /**
     * Obtain an AST for a source file in a project
     * 
     * @param javaProject
     * @param className
     * @return An AST, or null if no source file exists for that class
     * @throws JavaModelException
     */
    public static final CompilationUnit createAST(IJavaProject javaProject, String className) throws JavaModelException {
        IType type = javaProject.findType(className);
        if (type == null)
            return null;

        final ICompilationUnit cunit = type.getCompilationUnit();
        if (cunit == null)
            return null; // not a source type

        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(cunit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    /**
     * Create a marker on a Java Type
     * 
     * @param javaProject
     * @param className
     *            - the fully qualified class name (e.g java.lang.String)
     * @param markerAttributes
     * @param hasResolutions
     *            - true if the marker will have resolutions
     * @return Marker Data that can be used to create an {@link IMarker}, or null if no location can be found
     * @throws JavaModelException
     */
    public static final MarkerData createTypeMarkerData(IJavaProject javaProject, final String className, final Map<String,Object> markerAttributes, boolean hasResolutions) throws JavaModelException {

        final CompilationUnit ast = createAST(javaProject, className);

        if (ast == null)
            return null;

        ast.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration typeDecl) {
                ITypeBinding typeBinding = typeDecl.resolveBinding();
                if (typeBinding != null) {
                    if (typeBinding.getBinaryName().equals(className)) {
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

        return new MarkerData(ast.getJavaElement().getResource(), markerAttributes, hasResolutions);
    }

    /**
     * Create a marker on a Java Method
     * 
     * @param javaProject
     * @param className
     *            - the fully qualified class name (e.g java.lang.String)
     * @param methodName
     * @param methodSignature
     *            - signatures are in "internal form" e.g. (Ljava.lang.Integer;[Ljava/lang/String;Z)V
     * @param markerAttributes
     *            - attributes that should be included in the marker, typically a message. The start and end points for
     *            the marker are added by this method.
     * @param hasResolutions
     *            - true if the marker will have resolutions
     * @return Marker Data that can be used to create an {@link IMarker}, or null if no location can be found
     * @throws JavaModelException
     */
    public static final MarkerData createMethodMarkerData(IJavaProject javaProject, final String className, final String methodName, final String methodSignature, final Map<String,Object> markerAttributes, boolean hasResolutions)
            throws JavaModelException {

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

            private boolean matches(CompilationUnit ast, MethodDeclaration methodDecl, String methodName, String signature) {
                if ("<init>".equals(methodName)) {
                    if (!methodDecl.isConstructor()) {
                        return false;
                    }
                } else if (!methodDecl.getName().getIdentifier().equals(methodName)) {
                    return false;
                }

                return getSignature(ast, methodDecl).equals(signature);
            }

            private String getSignature(CompilationUnit ast, MethodDeclaration methodDecl) {
                StringBuilder signatureBuilder = new StringBuilder("(");

                for (@SuppressWarnings("unchecked")
                Iterator<SingleVariableDeclaration> it = methodDecl.parameters().iterator(); it.hasNext();) {
                    SingleVariableDeclaration decl = it.next();
                    appendType(ast, signatureBuilder, decl.getType(), decl.getExtraDimensions());
                }

                signatureBuilder.append(")");

                appendType(ast, signatureBuilder, methodDecl.getReturnType2(), 0);
                return signatureBuilder.toString();
            }

            private void appendType(CompilationUnit ast, StringBuilder signatureBuilder, Type typeToAdd, int extraDimensions) {
                for (int i = 0; i < extraDimensions; i++) {
                    signatureBuilder.append('[');
                }
                if (typeToAdd == null) {
                    //A special return type for constructors, nice one Eclipse...
                    signatureBuilder.append("V");
                } else {
                    if (typeToAdd.isArrayType()) {
                        ArrayType type = (ArrayType) typeToAdd;
                        int depth = type.getDimensions();
                        for (int i = 0; i < depth; i++) {
                            signatureBuilder.append('[');
                        }
                        //We still need to add the array component type, which might be primitive or a reference
                        typeToAdd = type.getElementType();
                    }

                    if (typeToAdd.isPrimitiveType()) {
                        PrimitiveType type = (PrimitiveType) typeToAdd;
                        signatureBuilder.append(PRIMITIVES_TO_SIGNATURES.get(type.getPrimitiveTypeCode()));
                    } else if (typeToAdd.isSimpleType()) {
                        SimpleType type = (SimpleType) typeToAdd;
                        String name;
                        if (type.getName().isQualifiedName()) {
                            name = type.getName().getFullyQualifiedName();
                        } else {
                            name = getFullyQualifiedNameForSimpleName(ast, type.getName());
                        }
                        name = name.replace('.', '/');
                        signatureBuilder.append("L").append(name).append(";");
                    } else if (typeToAdd.isQualifiedType()) {
                        QualifiedType type = (QualifiedType) typeToAdd;
                        String name = type.getQualifier().toString().replace('.', '/') + '/' + type.getName().getFullyQualifiedName().replace('.', '/');
                        signatureBuilder.append("L").append(name).append(";");
                    } else {
                        throw new IllegalArgumentException("We hit an unknown type " + typeToAdd);
                    }
                }
            }

            private String getFullyQualifiedNameForSimpleName(CompilationUnit ast, Name typeName) {
                String name = typeName.getFullyQualifiedName();

                for (ImportDeclaration id : (List<ImportDeclaration>) ast.imports()) {
                    if (id.isStatic())
                        continue;
                    if (id.isOnDemand()) {
                        String packageName = id.getName().getFullyQualifiedName();
                        try {
                            if (ast.getJavaElement().getJavaProject().findType(packageName + "." + name) != null) {
                                name = packageName + '.' + name;
                            }
                        } catch (JavaModelException e) {}
                    } else {
                        String importName = id.getName().getFullyQualifiedName();
                        if (importName.endsWith("." + name)) {
                            name = importName;
                            break;
                        }
                    }
                }

                if (name.indexOf('.') < 0) {
                    try {
                        if (ast.getJavaElement().getJavaProject().findType(name) == null) {
                            name = "java.lang." + name;
                        }
                    } catch (JavaModelException e) {}
                }
                return name;
            }
        });

        if (!markerAttributes.containsKey(IMarker.CHAR_START))
            return null;

        return new MarkerData(ast.getJavaElement().getResource(), markerAttributes, hasResolutions);
    }

    @Override
    public List<IMarkerResolution> getResolutions(IMarker marker) {
        return Collections.emptyList();
    }

    @Override
    public List<ICompletionProposal> getProposals(IMarker marker) {
        return Collections.emptyList();
    }

}
