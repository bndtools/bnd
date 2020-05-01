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
				paramType = typeBinding.getName();
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

	/**
	 * Compare the methodName with the signature of this method declaration
	 *
	 * @param methodName a method name, with FQ class names (int, a.b.Foo)
	 * @param methodDecl a AST method declaration
	 * @return true if the methodName matches the methodDecl
	 */
	public static boolean isEqual(String methodName, MethodDeclaration methodDecl) {

		String name = methodDecl.getName()
			.getFullyQualifiedName();
		if (!methodName.startsWith(name))
			return false;

		int l = name.length();
		if (methodName.charAt(l) != '(')
			return false;

		l++;

		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> params = methodDecl.parameters();
		if (params.isEmpty()) {
			return methodName.charAt(l) == ')';
		}

		char delim = ',';
		for (SingleVariableDeclaration param : params) {
			if (delim != ',')
				return false;

			ITypeBinding typeBinding = param.getType()
				.resolveBinding();
			if (typeBinding == null)
				return false;

			String typeName = typeBinding.getQualifiedName();
			int ll = methodName.indexOf(typeName, l);
			if (ll < 0)
				return false;

			l += typeName.length();
			delim = methodName.charAt(l);
			l++;
		}
		if (delim != ')')
			return false;

		return true;
	}
}
