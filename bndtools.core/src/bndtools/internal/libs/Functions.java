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
package bndtools.internal.libs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

public class Functions {
	
	public static boolean hasNature(IProject project, String natureId) throws CoreException {
		String[] ids = project.getDescription().getNatureIds();
		for (String id : ids) {
			if(natureId.equals(id)) {
				return true;
			}
		}
		return false;
	}
	
	public static <T> void appendSimpleProperty(StringBuilder builder, String label, Object value) {
		appendSimpleProperty(builder, label, value, new ObjectStringifier());
	}
	
	public static <T> void appendSimpleProperty(StringBuilder builder, String label, T value, Function<? super T, String> stringifier) {
		builder.append(label).append('=').append(stringifier.invoke(value)).append('\n');
	}
	
	public static <T> void appendListProperty(StringBuilder builder, String label, Collection<T> values) {
		appendListProperty(builder, label, values, new ObjectStringifier());
	}
	
	public static <T> void appendListProperty(StringBuilder builder, String label, Collection<T> values, Function<? super T,String> stringifier) {
		if(values.size() > 0) {
			boolean first = true;
			for(Iterator<T> iter = values.iterator(); iter.hasNext(); first = false) {
				if(first) {
					builder.append(label).append('=');
				}
				
				T value = iter.next();
				builder.append(stringifier.invoke(value));
				
				if(iter.hasNext()) {
					// Comma, backslash, newline, indent
					builder.append(",\\\n\t");
				}
			}
			builder.append('\n');
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T adaptAs(Object object, Class<T> clazz) {
		if(clazz.isInstance(object)) {
			return (T) object;
		}
		
		if(object instanceof IAdaptable) {
			T adapted = (T) ((IAdaptable) object).getAdapter(clazz);
			if(adapted != null) {
				return adapted;
			}
		}
		return null;
		//throw new RuntimeException("Object is not an instance of or adaptable to class " + clazz.getName());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> boolean isAdaptable(Object object, Class<T> clazz) {
		if(clazz.isInstance(object)) {
			return true;
		}
		
		if(object instanceof IAdaptable) {
			T adapted = (T) ((IAdaptable) object).getAdapter(clazz);
			return adapted != null;
		}
		
		return false;
	}
	
	public static <T> T notNull(T object, RuntimeException error) {
		if(object == null) {
			throw error;
		}
		return object;
	}
	
	public static <T> boolean equalsWithNull(T o1, T o2) {
		if(o1 == null) {
			return o2 == null;
		}
		return o1.equals(o2);
	}
	
	public static List<String> makeRelative(List<String> fromPath, List<String> toPath) {
		ArrayList<String> copyFrom = new ArrayList<String>(fromPath.size());
		copyFrom.addAll(fromPath);
		
		ArrayList<String> copyTo = new ArrayList<String>(toPath.size());
		copyTo.addAll(toPath);
		
		return makeRelative(copyFrom, copyTo, "..");
	}

	private static <T> List<T> makeRelative(List<T> fromPath, List<T> toPath, T upToken) {
		T fromFirst = fromPath.get(0);
		T toFirst = toPath.get(0);

		List<T> result;
		
		if(equalsWithNull(fromFirst, toFirst)) {
			fromPath.remove(0);
			toPath.remove(0);
			result = makeRelative(fromPath, toPath, upToken);
		} else {
			result = new ArrayList<T>(fromPath.size() + toPath.size());
			int upCount = fromPath.size();
			for(int i=0; i<upCount; i++) {
				result.add(upToken);
			}
			result.addAll(toPath);
		}
		
		return result;
	}

}
