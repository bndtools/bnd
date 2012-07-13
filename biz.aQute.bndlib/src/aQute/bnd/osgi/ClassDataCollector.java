package aQute.bnd.osgi;

import aQute.bnd.osgi.Descriptors.TypeRef;

public class ClassDataCollector {
	public void classBegin(@SuppressWarnings("unused") int access, @SuppressWarnings("unused") TypeRef name) {}

	public boolean classStart(int access, TypeRef className) {
		classBegin(access, className);
		return true;
	}

	public void extendsClass(@SuppressWarnings("unused") TypeRef zuper) throws Exception {}

	public void implementsInterfaces(@SuppressWarnings("unused") TypeRef[] interfaces) throws Exception {}

	public void addReference(@SuppressWarnings("unused") TypeRef ref) {}

	public void annotation(@SuppressWarnings("unused") Annotation annotation) {}

	public void parameter(@SuppressWarnings("unused") int p) {}

	public void method(@SuppressWarnings("unused") Clazz.MethodDef defined) {}

	public void field(@SuppressWarnings("unused") Clazz.FieldDef defined) {}

	public void reference(@SuppressWarnings("unused") Clazz.MethodDef referenced) {}

	public void reference(@SuppressWarnings("unused") Clazz.FieldDef referenced) {}

	public void classEnd() throws Exception {}

	public void deprecated() throws Exception {}

	/**
	 * The EnclosingMethod attribute
	 * 
	 * @param cName
	 *            The name of the enclosing class, never null. Name is with
	 *            slashes.
	 * @param mName
	 *            The name of the enclosing method in the class with cName or
	 *            null
	 * @param mDescriptor
	 *            The descriptor of this type
	 */
	public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {

	}

	/**
	 * The InnerClass attribute
	 * 
	 * @param innerClass
	 *            The name of the inner class (with slashes). Can be null.
	 * @param outerClass
	 *            The name of the outer class (with slashes) Can be null.
	 * @param innerName
	 *            The name inside the outer class, can be null.
	 * @param modifiers
	 *            The access flags
	 * @throws Exception
	 */
	public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName, @SuppressWarnings("unused") int innerClassAccessFlags)
			throws Exception {}

	public void signature(@SuppressWarnings("unused") String signature) {}

	public void constant(@SuppressWarnings("unused") Object object) {}

	public void memberEnd() {}

	public void version(@SuppressWarnings("unused") int minor, @SuppressWarnings("unused") int major) {
		// TODO Auto-generated method stub

	}

	public void referenceMethod(@SuppressWarnings("unused")
	int access, @SuppressWarnings("unused")
	TypeRef className, @SuppressWarnings("unused")
	String method, @SuppressWarnings("unused") String descriptor) {
		// TODO Auto-generated method stub

	}

}
