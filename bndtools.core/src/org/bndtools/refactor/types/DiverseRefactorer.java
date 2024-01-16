package org.bndtools.refactor.types;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.JavaModifier;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.service.component.annotations.Component;

/**
 * Small fry refactorings
 */
@Component
public class DiverseRefactorer extends BaseRefactorer implements IQuickFixProcessor {

	/**
	 * Add the completions available on the current selection for literals and
	 * txt blocks.
	 */
	@Override
	public void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> root,
		IInvocationContext context) {
		root.upTo(SingleVariableDeclaration.class, 2)
			.and(c -> c.upTo(MethodDeclaration.class, 1)
				.filter(MethodDeclaration::isConstructor))
			.forEach((ass, svd) -> {
				String name = makeUniqueName(assistant, svd);
				builder.build("div.constr.final", "Assign to final field " + name, "final", 0,
					() -> addField(ass, svd, name, JavaModifier.FINAL));
			});
	}

	private String makeUniqueName(RefactorAssistant ass, ASTNode svd) {
		String name = ass.getIdentifier(svd);
		int n =1;
		Set<String> fieldNames = ass.getFieldNames(ass.getAncestor(svd, TypeDeclaration.class));
		while( fieldNames.contains(name)) {
			name = ass.getIdentifier(svd) + n++;
		}
		return name;
	}

	private void addField(RefactorAssistant ass, SingleVariableDeclaration svd, String name,
		JavaModifier... modifiers) {

		MethodDeclaration md = (MethodDeclaration) svd.getParent();
		TypeDeclaration td = (TypeDeclaration) md.getParent();

		FieldDeclaration newField = ass.newFieldDeclaration(td, name, svd.getType(), modifiers);
		FieldAccess fieldAccess = ass.newFieldAccess(ass.newThisExpression(), name);
		Assignment assignment = ass.newAssignment(fieldAccess, svd.getName());
		ExpressionStatement es = ass.newExpressionStatement(assignment);
		List<FieldDeclaration> list = ass.stream(td, FieldDeclaration.class)
			.toList();
		if (list.isEmpty()) {
			ass.insert(td, FieldDeclaration.class, newField);
		} else {
			FieldDeclaration lastDeclaration = list.get(list.size() - 1);
			ass.insertAfter(lastDeclaration, newField);
		}
		Block body = md.getBody();
		Optional<Statement> l = ass.stream(body, Statement.class)
			.filter(node -> node instanceof SuperConstructorInvocation || node instanceof ConstructorInvocation)
			.findAny();
		if (l.isPresent()) {
			ass.insertAfter(l.get(), es);
		} else
			ass.insert(md.getBody(), ExpressionStatement.class, es);
	}

}
