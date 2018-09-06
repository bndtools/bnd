package aQute.bnd.osgi;

import java.util.ArrayList;
import java.util.List;

import org.osgi.util.function.Consumer;

import aQute.bnd.osgi.Descriptors.TypeRef;

class ClassDataCollectorRecorder extends ClassDataCollector {
	private final List<Consumer<ClassDataCollector>>	actions	= new ArrayList<>();
	private boolean										start;

	ClassDataCollectorRecorder() {}

	void play(ClassDataCollector cdc) throws Exception {
		start = true;
		for (Consumer<ClassDataCollector> action : actions) {
			if (!start) {
				break;
			}
			action.accept(cdc);
		}
	}

	ClassDataCollectorRecorder reset() {
		actions.clear();
		return this;
	}

	@Override
	public void classBegin(int access, TypeRef name) {
		actions.add(cdc -> cdc.classBegin(access, name));
	}

	@Override
	public boolean classStart(int access, TypeRef className) {
		actions.add(cdc -> {
			if (!cdc.classStart(access, className)) {
				start = false;
			}
		});
		return true;
	}

	@Override
	public boolean classStart(Clazz c) {
		actions.add(cdc -> {
			if (!cdc.classStart(c)) {
				start = false;
			}
		});
		return true;
	}

	@Override
	public void extendsClass(TypeRef zuper) throws Exception {
		actions.add(cdc -> cdc.extendsClass(zuper));
	}

	@Override
	public void implementsInterfaces(TypeRef[] interfaces) throws Exception {
		actions.add(cdc -> cdc.implementsInterfaces(interfaces));
	}

	@Override
	public void addReference(TypeRef ref) {
		actions.add(cdc -> cdc.addReference(ref));
	}

	@Override
	public void annotation(Annotation annotation) throws Exception {
		actions.add(cdc -> cdc.annotation(annotation));
	}

	@Override
	public void parameter(int p) {
		actions.add(cdc -> cdc.parameter(p));
	}

	@Override
	public void method(Clazz.MethodDef defined) {
		actions.add(cdc -> cdc.method(defined));
	}

	@Override
	public void field(Clazz.FieldDef defined) {
		actions.add(cdc -> cdc.field(defined));
	}

	@Override
	public void classEnd() throws Exception {
		actions.add(ClassDataCollector::classEnd);
	}

	@Override
	public void deprecated() throws Exception {
		actions.add(ClassDataCollector::deprecated);
	}

	@Override
	public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {
		actions.add(cdc -> cdc.enclosingMethod(cName, mName, mDescriptor));
	}

	@Override
	public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName, int innerClassAccessFlags)
		throws Exception {
		actions.add(cdc -> cdc.innerClass(innerClass, outerClass, innerName, innerClassAccessFlags));
	}

	@Override
	public void signature(String signature) {
		actions.add(cdc -> cdc.signature(signature));
	}

	@Override
	public void constant(Object object) {
		actions.add(cdc -> cdc.constant(object));
	}

	@Override
	public void memberEnd() {
		actions.add(ClassDataCollector::memberEnd);
	}

	@Override
	public void version(int minor, int major) {
		actions.add(cdc -> cdc.version(minor, major));
	}

	@Override
	public void referenceMethod(int access, TypeRef className, String method, String descriptor) {
		actions.add(cdc -> cdc.referenceMethod(access, className, method, descriptor));
	}

	@Override
	public void referTo(TypeRef typeRef, int modifiers) {
		actions.add(cdc -> cdc.referTo(typeRef, modifiers));
	}

	@Override
	public void annotationDefault(Clazz.MethodDef last) {
		actions.add(cdc -> cdc.annotationDefault(last));
	}

	@Override
	public void annotationDefault(Clazz.MethodDef last, Object value) {
		actions.add(cdc -> cdc.annotationDefault(last, value));
	}
}
