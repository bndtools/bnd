package bndtools.editor.exports;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.TextEdit;

public class AddExportProposal implements IJavaCompletionProposal {
	private static final String	ORG_OSGI_ANNOTATION_BUNDLE_EXPORT	= "org.osgi.annotation.bundle.Export";

	final ICompilationUnit		unit;

	public AddExportProposal(ICompilationUnit unit) {
		this.unit = unit;
	}

	boolean isApplicable() {
		if (unit.getElementName()
			.equals("package-info.java")) {
			IPackageDeclaration pd = unit.getPackageDeclaration(null);
			IAnnotation annotation = pd.getAnnotation(ORG_OSGI_ANNOTATION_BUNDLE_EXPORT);
			return annotation == null;
		} else
			return false;
	}

	@Override
	public void apply(IDocument document) {
		try {
			ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
			parser.setSource(unit);
			parser.setResolveBindings(true);

			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
			AST ast = astRoot.getAST();

			ASTRewrite rewriter = ASTRewrite.create(ast);
			PackageDeclaration packageDeclaration = astRoot.getPackage();

			if (packageDeclaration != null) {
				Annotation annotation = ast.newMarkerAnnotation();
				annotation.setTypeName(ast.newSimpleName(ORG_OSGI_ANNOTATION_BUNDLE_EXPORT));
				rewriter.getListRewrite(packageDeclaration, PackageDeclaration.ANNOTATIONS_PROPERTY)
					.insertFirst(annotation, null);

				TextEdit edits = rewriter.rewriteAST();
				unit.applyTextEdit(edits, null);
				unit.getBuffer()
					.setContents(astRoot.toString());
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			// Handle exception
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return null; // Return the new selection after insertion, if applicable
	}

	@Override
	public String getAdditionalProposalInfo() {
		return "Add Export & Version annotation";
	}

	@Override
	public String getDisplayString() {
		return "Add Export annotation";
	}

	@Override
	public Image getImage() {
		return null; // You can return an image to be displayed with the
						// proposal
	}

	@Override
	public IContextInformation getContextInformation() {
		return null; // Context information related to this proposal, if
						// applicable
	}

	@Override
	public int getRelevance() {
		return 1000;
	}
}
