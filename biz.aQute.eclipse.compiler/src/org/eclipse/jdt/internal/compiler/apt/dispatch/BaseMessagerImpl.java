/*******************************************************************************
 * Copyright (c) 2007 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    wharley@bea.com - derived base class from BatchMessagerImpl
 *    
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.apt.dispatch;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.apt.model.ExecutableElementImpl;
import org.eclipse.jdt.internal.compiler.apt.model.TypeElementImpl;
import org.eclipse.jdt.internal.compiler.apt.model.VariableElementImpl;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.Util;

public class BaseMessagerImpl {

	static final String[] NO_ARGUMENTS = new String[0];

	/**
	 * Create a CategorizedProblem that can be reported to an ICompilerRequestor, etc.
	 * 
	 * @param e the element against which to report the message.  If the element is not
	 * in the set of elements being compiled in the current round, the reference context
	 * and filename will be set to null.
	 * @return
	 */
	public static AptProblem createProblem(Kind kind, CharSequence msg, Element e) {
		ReferenceContext referenceContext = null;
		int startPosition = 0;
		int endPosition = 0;
		if (e != null) {
			switch(e.getKind()) {
				case ANNOTATION_TYPE :
				case INTERFACE :
				case CLASS :
				case ENUM :
					TypeElementImpl typeElementImpl = (TypeElementImpl) e;
					Binding typeBinding = typeElementImpl._binding;
					if (typeBinding instanceof SourceTypeBinding) {
						SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) typeBinding;
						TypeDeclaration typeDeclaration = (TypeDeclaration) sourceTypeBinding.scope.referenceContext();
						referenceContext = typeDeclaration;
						startPosition = typeDeclaration.sourceStart;
						endPosition = typeDeclaration.sourceEnd;
					}
					break;
				case PACKAGE :
					// nothing to do: there is no reference context for a package
					break;
				case CONSTRUCTOR :
				case METHOD :
					ExecutableElementImpl executableElementImpl = (ExecutableElementImpl) e;
					Binding binding = executableElementImpl._binding;
					if (binding instanceof MethodBinding) {
						MethodBinding methodBinding = (MethodBinding) binding;
						AbstractMethodDeclaration sourceMethod = methodBinding.sourceMethod();
						if (sourceMethod != null) {
							referenceContext = sourceMethod;
							startPosition = sourceMethod.sourceStart;
							endPosition = sourceMethod.sourceEnd;
						}
					}
					break;
				case ENUM_CONSTANT :
					break;
				case EXCEPTION_PARAMETER :
					break;
				case FIELD :
					VariableElementImpl variableElementImpl = (VariableElementImpl) e;
					binding = variableElementImpl._binding;
					if (binding instanceof FieldBinding) {
						FieldBinding fieldBinding = (FieldBinding) binding;
						FieldDeclaration fieldDeclaration = fieldBinding.sourceField();
						if (fieldDeclaration != null) {
							ReferenceBinding declaringClass = fieldBinding.declaringClass;
							if (declaringClass instanceof SourceTypeBinding) {
								SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) declaringClass;
								TypeDeclaration typeDeclaration = (TypeDeclaration) sourceTypeBinding.scope.referenceContext();
								referenceContext = typeDeclaration;
							}
							startPosition = fieldDeclaration.sourceStart;
							endPosition = fieldDeclaration.sourceEnd;
						}
					}
					break;
				case INSTANCE_INIT :
				case STATIC_INIT :
					break;
				case LOCAL_VARIABLE :
					break;
				case PARAMETER :
					break;
				case TYPE_PARAMETER :
			}
		}
		StringBuilder builder = new StringBuilder();
		if (msg != null) {
			builder.append(msg);
		}
		int lineNumber = 0;
		int columnNumber = 1;
		char[] fileName = null;
		if (referenceContext != null) {
			CompilationResult result = referenceContext.compilationResult();
			fileName = result.fileName;
			int[] lineEnds = null;
			lineNumber = startPosition >= 0
					? Util.getLineNumber(startPosition, lineEnds = result.getLineSeparatorPositions(), 0, lineEnds.length-1)
					: 0;
			columnNumber = startPosition >= 0
					? Util.searchColumnNumber(result.getLineSeparatorPositions(), lineNumber,startPosition)
					: 0;
		}
		int severity;
		switch(kind) {
			case ERROR :
				severity = ProblemSeverities.Error;
				break;
			default :
				// There is no "INFO" equivalent in JDT
				severity = ProblemSeverities.Warning;
				break;
		}
		return new AptProblem(
				referenceContext,
				fileName,
				String.valueOf(builder),
				0,
				NO_ARGUMENTS,
				severity,
				startPosition,
				endPosition,
				lineNumber,
				columnNumber);
	}

	public BaseMessagerImpl() {
		super();
	}

}