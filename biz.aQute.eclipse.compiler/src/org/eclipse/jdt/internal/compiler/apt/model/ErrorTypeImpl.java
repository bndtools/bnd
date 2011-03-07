/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.apt.model;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import org.eclipse.jdt.internal.compiler.apt.dispatch.BaseProcessingEnvImpl;

/**
 * Implementation of the {@link ErrorType} interface.
 */
public class ErrorTypeImpl extends DeclaredTypeImpl implements ErrorType {

	/* package */ ErrorTypeImpl(BaseProcessingEnvImpl env) {
		super(env, null);
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.DeclaredType#asElement()
	 */
	@Override
	public Element asElement() {
		return this._env.getFactory().newElement(null);
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.DeclaredType#getEnclosingType()
	 */
	@Override
	public TypeMirror getEnclosingType() {
		return NoTypeImpl.NO_TYPE_NONE;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.DeclaredType#getTypeArguments()
	 */
	@Override
	public List<? extends TypeMirror> getTypeArguments() {
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.TypeMirror#accept(javax.lang.model.type.TypeVisitor, java.lang.Object)
	 */
	@Override
	public <R, P> R accept(TypeVisitor<R, P> v, P p) {
		return v.visitError(this, p);
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.TypeMirror#getKind()
	 */
	@Override
	public TypeKind getKind() {
		return TypeKind.ERROR;
	}
	
	@Override
	public String toString() {
		return "<any>"; //$NON-NLS-1$
	}
}
