package org.bndtools.refactor.util;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Base class for when you want to create a state build up of Cursor's. For
 * example used in the ComponentRefactorer to hold where are the annotations
 * are.
 *
 * @param <T> the cursor type
 */
public class DomainBase<T extends ASTNode> {
	public final Cursor<T>	cursor;
	public final T				node;

	protected DomainBase(Cursor<T> cursor) {
		RefactorAssistant assistant = cursor.getAssistant();
		this.cursor = cursor;
		this.node = cursor.getNode()
			.orElse(null);
	}

	public Cursor<T> cursor() {
		return cursor;
	}
}
