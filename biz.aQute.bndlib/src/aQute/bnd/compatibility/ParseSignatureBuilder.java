package aQute.bnd.compatibility;

import java.io.*;

import aQute.lib.osgi.*;

public class ParseSignatureBuilder {
	final Scope			root;
	
	public ParseSignatureBuilder(Scope root) {
		this.root = root;
	}
	
	public void add( Jar jar ) throws Exception {
		for ( Resource r : jar.getResources().values()) {
			InputStream in = r.openInputStream();
			try {
				parse(in);
			} finally {
				in.close();
			}
		}
	}
	
	public Scope getRoot() { return root; }
	
	
	public void parse(InputStream in) throws Exception {
		Analyzer analyzer = new Analyzer();
		Clazz clazz = new Clazz(analyzer, "", null);
		
		clazz.parseClassFile(in, new ClassDataCollector() {
			Scope	s;
			Scope	enclosing;
			Scope	declaring;

			public void classBegin(int access, String name) {
				s = root.getScope(Scope.classIdentity(name));
				s.access = Access.modifier(access);
				s.kind = Kind.CLASS;
			}

			public void extendsClass(String name) {
//				s.setBase(new GenericType(name));
			}

			public void implementsInterfaces(String names[]) {
				s.setParameterTypes(convert(names));
			}

			GenericType[] convert(String names[]) {
				GenericType tss[] = new GenericType[names.length];
				for (int i = 0; i < names.length; i++) {
//					tss[i] = new GenericType(names[i]);
				}
				return tss;
			}

			public void method(Clazz.MethodDef defined) {
				String descriptor;
				Kind kind;
				if (defined.isConstructor()) {
					descriptor = ":" + defined.getDescriptor();
					kind = Kind.CONSTRUCTOR;
				} else {
					descriptor = defined.getName() + ":" + defined.getDescriptor();
					kind = Kind.METHOD;
				}
				Scope m = s.getScope(descriptor);
				m.access = Access.modifier(defined.getAccess());
				m.kind = kind;
				m.declaring = s;
				s.add(m);
			}

			public void field(Clazz.FieldDef defined) {
				String descriptor = defined.getName() + ":" + defined.getDescriptor();
				Kind kind = Kind.FIELD;
				Scope m = s.getScope(descriptor);
				m.access = Access.modifier(defined.getAccess());
				m.kind = kind;
				m.declaring = s;
				s.add(m);
			}

			public void classEnd() {
				if (enclosing != null)
					s.setEnclosing( enclosing );
				if (declaring != null)
					s.setDeclaring( declaring );				
			}

			public void enclosingMethod(String cName, String mName, String mDescriptor) {
				enclosing = root.getScope(Scope.classIdentity(cName));
				if (mName != null) {
					enclosing = enclosing.getScope(Scope.methodIdentity(mName, mDescriptor));
				}
			}

			public void innerClass(String innerClass, String outerClass, String innerName,
					int innerClassAccessFlags) {
				if (outerClass != null && innerClass != null && innerClass.equals(s.name))
					declaring = root.getScope(Scope.classIdentity(outerClass));
			}
		});
		
		
	}
}

