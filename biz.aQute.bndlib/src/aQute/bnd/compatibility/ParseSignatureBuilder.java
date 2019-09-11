package aQute.bnd.compatibility;

import java.io.InputStream;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

public class ParseSignatureBuilder {
	final Scope root;

	public ParseSignatureBuilder(Scope root) {
		this.root = root;
	}

	public void add(Jar jar) throws Exception {
		for (Resource r : jar.getResources()
			.values()) {
			try (InputStream in = r.openInputStream()) {
				parse(in);
			}
		}
	}

	public Scope getRoot() {
		return root;
	}

	public void parse(InputStream in) throws Exception {
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, "", null);

			clazz.parseClassFile(in, new ClassDataCollector() {
				Scope	s;
				Scope	enclosing;
				Scope	declaring;

				@Override
				public void classBegin(int access, TypeRef name) {
					s = root.getScope(name.getBinary());
					s.access = Access.modifier(access);
					s.kind = Kind.CLASS;
				}

				@Override
				public void extendsClass(TypeRef name) {
					// s.setBase(new GenericType(name));
				}

				@Override
				public void implementsInterfaces(TypeRef names[]) {
					s.setParameterTypes(convert(names));
				}

				GenericType[] convert(TypeRef names[]) {
					GenericType tss[] = new GenericType[names.length];
					for (int i = 0; i < names.length; i++) {
						// tss[i] = new GenericType(names[i]);
					}
					return tss;
				}

				@Override
				public void method(Clazz.MethodDef defined) {
					String descriptor;
					Kind kind;
					if (defined.isConstructor()) {
						descriptor = ":" + defined.descriptor();
						kind = Kind.CONSTRUCTOR;
					} else {
						descriptor = defined.getName() + ":" + defined.descriptor();
						kind = Kind.METHOD;
					}
					Scope m = s.getScope(descriptor);
					m.access = Access.modifier(defined.getAccess());
					m.kind = kind;
					m.declaring = s;
					s.add(m);
				}

				@Override
				public void field(Clazz.FieldDef defined) {
					String descriptor = defined.getName() + ":" + defined.descriptor();
					Kind kind = Kind.FIELD;
					Scope m = s.getScope(descriptor);
					m.access = Access.modifier(defined.getAccess());
					m.kind = kind;
					m.declaring = s;
					s.add(m);
				}

				@Override
				public void classEnd() {
					if (enclosing != null)
						s.setEnclosing(enclosing);
					if (declaring != null)
						s.setDeclaring(declaring);
				}

				@Override
				public void enclosingMethod(TypeRef cName, String mName, String mDescriptor) {
					enclosing = root.getScope(cName.getBinary());
					if (mName != null) {
						enclosing = enclosing.getScope(Scope.methodIdentity(mName, mDescriptor));
					}
				}

				@Override
				public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName,
					int innerClassAccessFlags) {
					if (outerClass != null && innerClass != null && innerClass.getBinary()
						.equals(s.name))
						declaring = root.getScope(outerClass.getBinary());
				}
			});
		}

	}
}
