/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.utils;


import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import bndtools.Plugin;

public class ServiceUtils {
	public static final <R,S,E extends Throwable> R usingService(Class<S> clazz, ServiceOperation<R,S,E> operation) throws E {
		BundleContext context = Plugin.getDefault().getBundleContext();
		
		ServiceReference reference = context.getServiceReference(clazz.getName());
		if(reference != null) {
			@SuppressWarnings("unchecked")
			S service = (S) context.getService(reference);
			if(service != null) {
				try {
					return operation.execute(service);
				} finally {
					context.ungetService(reference);
				}
			}
		}
		return null;
	}
}
