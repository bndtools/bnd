package aQute.lib.osgi;

public class ClassDataCollector {
    public void classBegin(int access, String name) {
    }

    public boolean classStart(int access, String name) {
        classBegin(access,name);
        return true;
    }

    public void extendsClass(String name) {
    }

    public void implementsInterfaces(String name[]) {
    }

    public void addReference(String token) {
    }

    public void annotation(Annotation annotation) {
    }

    public void parameter(int p) {
    }

    public void method(Clazz.MethodDef defined) {
        if (defined.isConstructor())
            constructor(defined.access, defined.descriptor);
        else
            method(defined.access, defined.name, defined.descriptor);
    }

    public void field(Clazz.FieldDef defined) {
        field(defined.access, defined.name, defined.descriptor);
    }

    public void reference(Clazz.MethodDef referenced) {
    }

    public void reference(Clazz.FieldDef referenced) {
    }

    public void classEnd() {
    }

    @Deprecated // Will really be removed!
    public void field(int access, String name, String descriptor) {
    }

    @Deprecated // Will really be removed!
    public void constructor(int access, String descriptor) {
    }

    @Deprecated // Will really be removed!
    public void method(int access, String name, String descriptor) {
    }

}
