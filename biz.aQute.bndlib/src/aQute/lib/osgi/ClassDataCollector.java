package aQute.lib.osgi;

import aQute.lib.osgi.Descriptors.TypeRef;

public class ClassDataCollector {
    public void classBegin(int access, TypeRef name) {
    }

    public boolean classStart(int access, TypeRef className) {
        classBegin(access,className);
        return true;
    }

    public void extendsClass(TypeRef zuper) throws Exception {
    }

    public void implementsInterfaces(TypeRef[] interfaces) throws Exception {
    }

    public void addReference(TypeRef ref) {
    }

    public void annotation(Annotation annotation) {
    }

    public void parameter(int p) {
    }

    public void method(Clazz.MethodDef defined) {
    }

    public void field(Clazz.FieldDef defined) {
    }

    public void reference(Clazz.MethodDef referenced) {
    }

    public void reference(Clazz.FieldDef referenced) {
    }

    public void classEnd() throws Exception {
    }

    public void deprecated() throws Exception {
    }


    /**
     * The EnclosingMethod attribute
     * 
     * @param cName The name of the enclosing class, never null. Name is with slashes.
     * @param mName The name of the enclosing method in the class with cName or null
     * @param mDescriptor The descriptor of this type
     */
	public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {
		
	}

	/**
	 * The InnerClass attribute
	 * 
	 * @param innerClass The name of the inner class (with slashes). Can be null.
	 * @param outerClass The name of the outer class (with slashes) Can be null.
	 * @param innerName The name inside the outer class, can be null.
	 * @param modifiers The access flags 
	 * @throws Exception 
	 */
	public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName,
			int innerClassAccessFlags) throws Exception {		
	}

	public void signature(String signature) {
	}

	public void constant(Object object) {
	}

	public void memberEnd() {
	}

	public void version(int minor, int major) {
		// TODO Auto-generated method stub
		
	}

	public void referenceMethod(int access, TypeRef className, String method, String descriptor) {
		// TODO Auto-generated method stub
		
	}

}
