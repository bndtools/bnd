/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Neil Bartlett - initial API and implementation
 ******************************************************************************/
package aQute.junit.runtime;

/**
 * Represents an operation against a service of type <strong>S</strong> yielding
 * a result of type <strong>R</strong>
 * 
 * @author Neil Bartlett
 * 
 * @param <S>
 *            The service type
 * @param <R>
 *            The result type
 */
public interface Operation<S, R> {
	R perform(S service) throws Exception;
}