package org.bndtools.utils.jdt;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class ASTUtil {
	public static String buildMethodSignature(MethodDeclaration method) {
		StringBuilder builder = new StringBuilder();

		builder.append(method.getName());
		builder.append('(');

		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> params = method.parameters();
		for (Iterator<SingleVariableDeclaration> iter = params.iterator(); iter.hasNext();) {
			String paramType;
			SingleVariableDeclaration param = iter.next();
			ITypeBinding typeBinding = param.getType()
				.resolveBinding();
			if (typeBinding != null)
				paramType = typeBinding.getBinaryName();
			else
				paramType = param.getName()
					.getIdentifier();

			builder.append(paramType);
			if (iter.hasNext())
				builder.append(",");
		}

		builder.append(')');
		return builder.toString();
	}
}
