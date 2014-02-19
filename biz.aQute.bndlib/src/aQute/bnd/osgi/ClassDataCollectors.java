package aQute.bnd.osgi;

import java.io.*;
import java.util.*;

import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.service.reporter.*;

/**
 * This class dispatches class data collectors. Over time more and more code was
 * looking for annotations and other stuff. In the early days, the parser tried
 * to not do full parsing to minimize the cost but basically we are now parsing
 * more than necessary because different places began parsing on their own.
 */
class ClassDataCollectors extends ClassDataCollector implements Closeable {
	final List<ClassDataCollector>	delegates	= new ArrayList<ClassDataCollector>();
	final List<ClassDataCollector>	shortlist	= new ArrayList<ClassDataCollector>();
	final Reporter					reporter;

	public ClassDataCollectors(Analyzer analyzer) {
		this.reporter = analyzer;
	}

	public void with(Clazz clazz, ClassDataCollector cd) throws Exception {
		delegates.add(cd);
		try {
			clazz.parseClassFileWithCollector(this);
		}
		finally {
			delegates.remove(cd);
		}
	}

	public void parse(Clazz clazz) throws Exception {
		clazz.parseClassFileWithCollector(this);
	}

	@Override
	public void classBegin(int access, TypeRef name) {
		for (ClassDataCollector cd : delegates)
			try {
				cd.classBegin(access, name);
			}
			catch (Exception e) {
				reporter.error("Fail to class classBegin on %s", cd);
			}
	}

	@Override
	public boolean classStart(int access, TypeRef className) {
		boolean start = false;
		for (ClassDataCollector cd : delegates)
			try {
				if (cd.classStart(access, className)) {
					shortlist.add(cd);
					start = true;
				}
			}
			catch (Exception e) {
				reporter.error("Fail to class classStart on %s", cd);
			}
		return start;
	}

	@Override
	public boolean classStart(Clazz clazz) {
		boolean start = false;
		for (ClassDataCollector cd : delegates)
			try {
				if (cd.classStart(clazz)) {
					shortlist.add(cd);
					start = true;
				}
			}
			catch (Exception e) {
				reporter.error("Fail to class classStart on %s", cd);
			}
		return start;
	}

	@Override
	public void extendsClass(TypeRef zuper) throws Exception {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.extendsClass(zuper);
			}
			catch (Exception e) {
				reporter.error("Fail to class extendsClass on %s", cd);
			}
	}

	@Override
	public void implementsInterfaces(TypeRef[] interfaces) throws Exception {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.implementsInterfaces(interfaces);
			}
			catch (Exception e) {
				reporter.error("Fail to class implementsInterfaces on %s", cd);
			}
	}

	@Override
	public void addReference(TypeRef ref) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.addReference(ref);
			}
			catch (Exception e) {
				reporter.error("Fail to class addReference on %s", cd);
			}
	}

	@Override
	public void annotation(Annotation annotation) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.annotation(annotation);
			}
			catch (Exception e) {
				reporter.error("Fail to class annotation on %s", cd);
			}
	}

	@Override
	public void parameter(int p) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.parameter(p);
			}
			catch (Exception e) {
				reporter.error("Fail to class parameter on %s", cd);
			}
	}

	@Override
	public void method(MethodDef defined) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.method(defined);
			}
			catch (Exception e) {
				reporter.error("Fail to call method on %s", cd);
			}
	}

	@Override
	public void field(FieldDef defined) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.field(defined);
			}
			catch (Exception e) {
				reporter.error("Fail to call field on %s", cd);
			}
	}

	@Override
	public void classEnd() throws Exception {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.classEnd();
			}
			catch (Exception e) {
				reporter.error("Fail to call classEnd on %s", cd);
			}
		shortlist.clear();
	}

	@Override
	public void deprecated() throws Exception {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.deprecated();
			}
			catch (Exception e) {
				reporter.error("Fail to call deprecated on %s", cd);
			}
	}

	@Override
	public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.enclosingMethod(cName, mName, mDescriptor);
			}
			catch (Exception e) {
				reporter.error("Fail to call enclosingMethod on %s", cd);
			}
	}

	@Override
	public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName, int innerClassAccessFlags)
			throws Exception {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.innerClass(innerClass, outerClass, innerName, innerClassAccessFlags);
			}
			catch (Exception e) {
				reporter.error("Fail to call innerClass on %s", cd);
			}
	}

	@Override
	public void signature(String signature) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.signature(signature);
			}
			catch (Exception e) {
				reporter.error("Fail to call innerClass on %s", cd);
			}
	}

	@Override
	public void constant(Object object) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.constant(object);
			}
			catch (Exception e) {
				reporter.error("Fail to call constant on %s", cd);
			}
	}

	@Override
	public void memberEnd() {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.memberEnd();
			}
			catch (Exception e) {
				reporter.error("Fail to call memberEnd on %s", cd);
			}
	}

	@Override
	public void version(int minor, int major) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.version(minor, major);
			}
			catch (Exception e) {
				reporter.error("Fail to call version on %s", cd);
			}
	}

	@Override
	public void referenceMethod(int access, TypeRef className, String method, String descriptor) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.referenceMethod(access, className, method, descriptor);
			}
			catch (Exception e) {
				reporter.error("Fail to call referenceMethod on %s", cd);
			}
	}

	@Override
	public void referTo(TypeRef typeRef, int modifiers) {
		for (ClassDataCollector cd : shortlist)
			try {
				cd.referTo(typeRef, modifiers);
			}
			catch (Exception e) {
				reporter.error("Fail to call referTo on %s", cd);
			}
	}

	public void close() {
		for (ClassDataCollector cd : delegates)
			try {
				if (cd instanceof Closeable)
					((Closeable) cd).close();
			}
			catch (Exception e) {
				reporter.error("Fail to call close on %s", cd);
			}
		delegates.clear();
		shortlist.clear();
	}

	public void add(ClassDataCollector cd) {
		delegates.add(cd);
	}

}
