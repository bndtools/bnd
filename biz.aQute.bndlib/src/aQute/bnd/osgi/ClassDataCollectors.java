package aQute.bnd.osgi;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.MethodParameter;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.service.reporter.Reporter;

/**
 * This class dispatches class data collectors. Over time more and more code was
 * looking for annotations and other stuff. In the early days, the parser tried
 * to not do full parsing to minimize the cost but basically we are now parsing
 * more than necessary because different places began parsing on their own.
 */
class ClassDataCollectors implements Closeable {
	final List<ClassDataCollector>	delegates	= new ArrayList<>();
	final Reporter					reporter;

	ClassDataCollectors(Reporter reporter) {
		this.reporter = reporter;
	}

	void add(ClassDataCollector cd) {
		delegates.add(cd);
	}

	void parse(Clazz clazz) throws Exception {
		clazz.parseClassFileWithCollector(new Collectors(clazz));
	}

	void with(Clazz clazz, ClassDataCollector cd) throws Exception {
		delegates.add(cd);
		try {
			parse(clazz);
		} finally {
			delegates.remove(cd);
		}
	}

	@Override
	public void close() {
		for (ClassDataCollector cd : delegates) {
			try {
				if (cd instanceof Closeable)
					((Closeable) cd).close();
			} catch (Exception e) {
				reporter.exception(e, "Failure on call close[%s]", cd);
			}
		}
		delegates.clear();
	}

	private class Collectors extends ClassDataCollector {
		private final Clazz						clazz;
		private final List<ClassDataCollector>	shortlist;

		Collectors(Clazz clazz) {
			this.clazz = clazz;
			this.shortlist = new ArrayList<>(delegates);
		}

		@Override
		public void classBegin(int access, TypeRef name) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.classBegin(access, name);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call classBegin[%s] for %s", clazz, cd);
				}
			}
		}

		@Override
		public boolean classStart(int access, TypeRef className) {
			boolean start = false;
			for (Iterator<ClassDataCollector> iter = shortlist.iterator(); iter.hasNext();) {
				ClassDataCollector cd = iter.next();
				try {
					if (cd.classStart(access, className)) {
						start = true;
					} else {
						iter.remove();
					}
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call classStart[%s]", clazz, cd);
				}
			}
			return start;
		}

		@Override
		public boolean classStart(Clazz clazz) {
			boolean start = false;
			for (Iterator<ClassDataCollector> iter = shortlist.iterator(); iter.hasNext();) {
				ClassDataCollector cd = iter.next();
				try {
					if (cd.classStart(clazz)) {
						start = true;
					} else {
						iter.remove();
					}
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call classStart[%s]", clazz, cd);
				}
			}
			return start;
		}

		@Override
		public void extendsClass(TypeRef zuper) throws Exception {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.extendsClass(zuper);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call extendsClass[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void implementsInterfaces(TypeRef[] interfaces) throws Exception {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.implementsInterfaces(interfaces);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call implementsInterfaces[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void addReference(TypeRef ref) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.addReference(ref);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call addReference[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void annotation(Annotation annotation) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.annotation(annotation);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call annotation[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void parameter(int p) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.parameter(p);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call parameter[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void method(MethodDef method) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.method(method);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call method[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void field(FieldDef field) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.field(field);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call field[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void classEnd() throws Exception {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.classEnd();
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call classEnd[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void deprecated() throws Exception {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.deprecated();
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call deprecated[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.enclosingMethod(cName, mName, mDescriptor);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call enclosingMethod[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName, int innerClassAccessFlags)
			throws Exception {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.innerClass(innerClass, outerClass, innerName, innerClassAccessFlags);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call innerClass[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void signature(String signature) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.signature(signature);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call signature[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void constant(Object object) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.constant(object);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call constant[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void memberEnd() {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.memberEnd();
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call memberEnd[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void version(int minor, int major) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.version(minor, major);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call version[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void referenceMethod(int access, TypeRef className, String method, String descriptor) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.referenceMethod(access, className, method, descriptor);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call referenceMethod[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void referTo(TypeRef typeRef, int modifiers) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.referTo(typeRef, modifiers);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call referTo[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void annotationDefault(Clazz.MethodDef method) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.annotationDefault(method);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call annotationDefault[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void annotationDefault(Clazz.MethodDef method, Object value) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.annotationDefault(method, value);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call annotationDefault[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.typeuse(target_type, target_index, target_info, type_path);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call typeuse[%s]", clazz, cd);
				}
			}
		}

		@Override
		public void methodParameters(Clazz.MethodDef method, MethodParameter[] parameters) {
			for (ClassDataCollector cd : shortlist) {
				try {
					cd.methodParameters(method, parameters);
				} catch (Exception e) {
					reporter.exception(e, "Failure for %s on call methodParameters[%s]", clazz, cd);
				}
			}
		}
	}
}
