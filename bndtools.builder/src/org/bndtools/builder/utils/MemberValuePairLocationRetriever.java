package org.bndtools.builder.utils;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.osgi.util.function.Predicate;

/**
 * Visitor that will "visit" an ASTNode and its children until it finds the
 * expected MemberValue pair to retain its location in the compilation unit
 * source code
 */
public class MemberValuePairLocationRetriever extends ASTVisitor {

	private final IAnnotation		javaAnnotation;
	private final Predicate<String>	annotationNameMatch;
	private final String			memberName;

	private ISourceRange			locatedSourceRange	= null;

	/**
	 * Constructor
	 */
	public MemberValuePairLocationRetriever(final IAnnotation javaAnnotation,
		final Predicate<String> annotationNameMatch, final String memberName) {
		this.javaAnnotation = javaAnnotation;
		this.annotationNameMatch = annotationNameMatch;
		this.memberName = memberName;
	}

	public ISourceRange getMemberValuePairSourceRange() {
		return locatedSourceRange;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.TYPE);
		if (ancestor != null && ancestor.exists() && ancestor.getElementName()
			.equals(node.getName()
				.getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.TYPE);
		if (ancestor != null && ancestor.exists() && ancestor.getElementName()
			.equals(node.getName()
				.getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	@Override
	public boolean visit(VariableDeclarationFragment node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.FIELD);
		if (ancestor != null && ancestor.exists() && ancestor.getElementName()
			.equals(node.getName()
				.getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.METHOD);
		if (ancestor != null && ancestor.exists() && ancestor.getElementName()
			.equals(node.getName()
				.getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleMemberAnnotation)
	 */
	@Override
	public boolean visit(SingleMemberAnnotation node) {
		final IAnnotationBinding annotationBinding = node.resolveAnnotationBinding();
		if (annotationBinding != null) {
			final String nodeName = annotationBinding.getAnnotationType()
				.getQualifiedName();
			boolean match;
			try {
				match = this.annotationNameMatch.test(nodeName);
			} catch (Exception e) {
				match = false;
			}
			if (match) {
				this.locatedSourceRange = new SourceRange(node.getValue()
					.getStartPosition(),
					node.getValue()
						.getLength());
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.NormalAnnotation)
	 */
	@Override
	public boolean visit(NormalAnnotation node) {
		final IJavaElement ancestor = javaAnnotation.getAncestor(IJavaElement.ANNOTATION);
		if (ancestor != null && ancestor.exists() && ancestor.getElementName()
			.equals(node.getTypeName()
				.getFullyQualifiedName())) {
			// keep searching
			return true;
		}
		// wrong path, stop searching from this branch of the AST
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MemberValuePair)
	 */
	@Override
	public boolean visit(MemberValuePair node) {
		if (node.getName()
			.getFullyQualifiedName()
			.equals(memberName)) {
			this.locatedSourceRange = new SourceRange(node.getStartPosition(), node.getLength());
		}
		// no need to drill down from here anyway
		return false;
	}

}
