package test.dynamicimport;

import org.objectweb.asm.ClassReader;

public class AsmUser {

	public ClassReader createClassReader(byte[] bytes) {
		return new ClassReader(bytes);
	}
}
