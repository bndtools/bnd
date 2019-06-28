package aQute.bnd.osgi;

import aQute.bnd.osgi.Clazz.MethodParameter;
import aQute.bnd.osgi.Descriptors.TypeRef;

/**
 * When adding methods to this class, you must also add them to
 * {@link ClassDataCollectors}!
 */
public class ClassDataCollector {

	public void classBegin(int access, TypeRef name) {}

	public boolean classStart(int access, TypeRef className) {
		classBegin(access, className);
		return true;
	}

	public boolean classStart(Clazz c) {
		classBegin(c.getAccess(), c.getClassName());
		return true;
	}

	public void extendsClass(TypeRef zuper) throws Exception {}

	public void implementsInterfaces(TypeRef[] interfaces) throws Exception {}

	public void addReference(TypeRef ref) {}

	public void annotation(Annotation annotation) throws Exception {}

	public void parameter(int p) {}

	public void method(Clazz.MethodDef method) {}

	public void field(Clazz.FieldDef field) {}

	public void classEnd() throws Exception {}

	public void deprecated() throws Exception {}

	/**
	 * The EnclosingMethod attribute
	 *
	 * @param cName The name of the enclosing class, never null. Name is with
	 *            slashes.
	 * @param mName The name of the enclosing method in the class with cName or
	 *            null
	 * @param mDescriptor The descriptor of this type
	 */
	public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {

	}

	/**
	 * The InnerClass attribute
	 *
	 * @param innerClass The name of the inner class (with slashes). Can be
	 *            null.
	 * @param outerClass The name of the outer class (with slashes) Can be null.
	 * @param innerName The name inside the outer class, can be null.
	 * @param innerClassAccessFlags The access flags
	 * @throws Exception
	 */
	public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName, int innerClassAccessFlags)
		throws Exception {}

	public void signature(String signature) {}

	public void constant(Object object) {}

	public void memberEnd() {}

	public void version(int minor, int major) {}

	public void referenceMethod(int access, TypeRef className, String method, String descriptor) {}

	/**
	 * A reference to a type from method or field. The modifiers indicate the
	 * access level of the parent method/field.
	 *
	 * @param typeRef
	 * @param modifiers
	 */
	public void referTo(TypeRef typeRef, int modifiers) {}

	public void annotationDefault(Clazz.MethodDef method) {}

	public void annotationDefault(Clazz.MethodDef method, Object value) {
		annotationDefault(method);
	}

	public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {}

	public void methodParameters(Clazz.MethodDef method, MethodParameter[] parameters) {}
}
