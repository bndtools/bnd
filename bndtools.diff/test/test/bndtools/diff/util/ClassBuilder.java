/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package test.bndtools.diff.util;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ClassBuilder implements Opcodes {

	public static ClassWriter createInterface(String className) {
		className = className.replace('.', '/');
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_5, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, className, null, "java/lang/Object", null);

		return cw;
	}

	public static void addStaticFinalField(String className, ClassWriter cw, String fieldName, Class<?> type,
			Object data) {
		className = className.replace('.', '/');
		String typeName = Type.getDescriptor(type);

		FieldVisitor fv;

		fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, fieldName, typeName, null, "S1");
		fv.visitEnd();

	}

	public static void addMethod(String className, ClassWriter cw, Class<?> returnType, String methodName,
			Class<?>... params) {

		if (returnType == null) {
			returnType = void.class;
		}

		StringBuilder sb = new StringBuilder("(");
		for (Class<?> param : params) {
			sb.append(Type.getDescriptor(param));
		}
		sb.append(")");
		sb.append(Type.getDescriptor(returnType));

		MethodVisitor mv;
		mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, methodName, sb.toString(), null, null);
		mv.visitCode();

	}

	public static byte[] endClass(String className, ClassWriter cw) {
		cw.visitEnd();
		return cw.toByteArray();

	}
}
