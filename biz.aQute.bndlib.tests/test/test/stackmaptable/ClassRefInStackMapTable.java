package test.stackmaptable;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

// https://github.com/bndtools/bnd/issues/1776

public class ClassRefInStackMapTable {

	public static void main(String[] args) {
		/*
		 * Create an object and assign it to a super-type variable. Note the
		 * super-type is in a different package from the concrete type.
		 */
		SecretKey key = new SecretKeySpec(new byte[] {
			0
		}, "NULL");

		/*
		 * This branch causes the compiler to emit a StackMapTable in the byte
		 * code. The table will refer to the local 'key' and force a reference
		 * to the 'SecretKey' class to appear in the constant pool (which would
		 * not be required without the StackMapTable). Bnd sees the 'SecretKey'
		 * class in the constant pool but assumes its an orphan because it has
		 * no associated entries that refer to it. When it double checks by
		 * crawling the byte code the StackMapTable is ignored. Bnd decides that
		 * 'SecretKey' is an orphan and does not emit and Import-Package
		 * directive for its package. This causes a NoClassDefFoundError at
		 * runtime.
		 */
		if (System.currentTimeMillis() > 0) {
			System.out.println("Toast!");
		}

		System.out.println(key);
	}
}
