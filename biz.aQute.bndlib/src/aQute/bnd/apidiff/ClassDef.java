package aQute.bnd.apidiff;

import java.lang.reflect.*;
import java.util.*;

import aQute.bnd.annotation.*;
import aQute.bnd.service.apidiff.*;
import aQute.bnd.service.apidiff.Type;
import aQute.lib.osgi.*;

public class ClassDef extends Def {
	final Clazz clazz;
	final Map<String, Def>	members	= new HashMap<String, Def>();
	boolean	consumerType;

	ClassDef(Clazz clazz) throws Exception {
		super(Type.CLASS, clazz.getFQN(), null);
		this.clazz = clazz;
		clazz.parseClassFileWithCollector(new ClassDataCollector() {
			MemberDef	lastMember;
			boolean		memberEnd;

			@Override public void method(Clazz.MethodDef defined) {
				if (Modifier.isProtected(defined.access) || Modifier.isPublic(defined.access)) {
					lastMember = new MemberDef(Type.METHOD, defined.name, defined.descriptor);
					members.put(defined.name + ":" + defined.descriptor, lastMember);
				}
			}

			@Override public void field(Clazz.FieldDef defined) {
				if (Modifier.isProtected(defined.access) || Modifier.isPublic(defined.access)) {
					lastMember = new MemberDef(Type.FIELD, defined.name, defined.descriptor);
					members.put(defined.name + ":" + defined.descriptor, lastMember);
				}
			}

			@Override public void extendsClass(String name) {
				members.put("<extends>",
						new MemberDef(Type.EXTENDS, Clazz.objectDescriptorToFQN(name), name));
			}

			@Override public void implementsInterfaces(String names[]) {
				// TODO is interface reordering important for binary
				// compatibility??
				for (String name : names) {
					String fqn = Clazz.objectDescriptorToFQN(name);
					members.put("<implements>." + fqn, new MemberDef(Type.EXTENDS, fqn, name));
				}
			}

			@Override public void annotation(Annotation annotation) {
				if (memberEnd) {
					members.put("<annotation>." + annotation.getName(), new AnnotationDef(
							annotation));
					String name = Clazz.objectDescriptorToFQN(annotation.getName());
					if (ConsumerType.class.getName().equals(name)){
						consumerType = true;
					}
				} else if (lastMember != null)
					lastMember.annotations.put(annotation.getName(), new AnnotationDef(annotation));
			}

			@Override public void memberEnd() {
				memberEnd = true;
			}
		});		
	}

	@Override public boolean isAddMajor() {
		return consumerType && clazz.isInterface();
	}
	
	@Override 
	public Map<String, Def> getChildren() {
		return members;
	}

	class AnnotationPropertyDef extends Def<AnnotationPropertyDef> {
		final String value;
		
		AnnotationPropertyDef(String name, String value) {
			super(Type.PROPERTY, name, null);
			this.value = value;
		}
		
		public Delta compare(AnnotationPropertyDef other ) {
			if ( value.equals(other.value))
				return Delta.UNCHANGED;
			else
				return Delta.CHANGED;
		}
	}
	
	class AnnotationDef extends Def<AnnotationDef> {
		final Annotation	annotation;
		final Map<String, AnnotationPropertyDef> properties = new HashMap<String, ClassDef.AnnotationPropertyDef>();
		AnnotationDef(Annotation annotation) {
			super(Type.ANNOTATION, annotation.getName(), null);
			this.annotation = annotation;
			for (String key : annotation.keySet() ) {
				properties.put(key, new AnnotationPropertyDef(key,""+annotation.get(key)));
			}
		}
		
		@Override public Map<String, AnnotationPropertyDef> getChildren() {
			return properties;
		}
	}

	class MemberDef extends Def<MemberDef> {
		final String						descriptor;
		final Map<String, AnnotationDef>	annotations	= new HashMap<String, AnnotationDef>();

		MemberDef(Type type, String name, String descriptor) {
			super(type, name, null);
			this.descriptor = descriptor;
		}

		@Override public Map<String, AnnotationDef> getChildren() {
			return annotations;
		}

	}


}
