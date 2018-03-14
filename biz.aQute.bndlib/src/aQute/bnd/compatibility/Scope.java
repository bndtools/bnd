package aQute.bnd.compatibility;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
	final Map<String, Scope>	children	= new LinkedHashMap<>();

	// class: slashed name
	// field: name ":" typed
	// constructor: ":(" typed* ")" typed
	// method: name ":(" typed* ")" typed
	final String				name;

	Access						access;
	Kind						kind;
	Scope						enclosing;
	Scope						declaring;
	GenericParameter			typeVars[];
	Map<String, String[]>		name2bounds;

	// class: super
	// field: type
	// constructor: void
	// method: return
	GenericType					base;

	// class: interfaces
	// constructor: args
	// method: args
	GenericType[]				parameters;

	// constructor: exceptions
	// method: exceptions
	GenericType[]				exceptions;

	// class: super interfaces*
	// field: type
	// constructor: void arguments*
	// method: return arguments*

	public Scope(Access access, Kind kind, String name) {
		this.access = access;
		this.kind = kind;
		this.name = name;
	}

	Scope getScope(String name) {
		Scope s = children.get(name);
		if (s != null)
			return s;

		s = new Scope(Access.UNKNOWN, Kind.UNKNOWN, name);
		children.put(name, s);
		s.declaring = this;
		return s;
	}

	public void setParameterTypes(GenericType[] convert) {
		this.parameters = convert;
	}

	public void setExceptionTypes(GenericType[] convert) {
		this.exceptions = convert;
	}

	public void setBase(GenericType typeSignature) {
		base = typeSignature;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (typeVars != null && typeVars.length != 0) {
			sb.append("<");
			for (GenericParameter v : typeVars) {
				sb.append(v);
			}
			sb.append(">");
		}
		sb.append(access.toString());
		sb.append(" ");
		sb.append(kind.toString());
		sb.append(" ");
		sb.append(name);
		return sb.toString();
	}

	public void report(Appendable a, int indent) throws IOException {
		for (int i = 0; i < indent; i++)
			a.append("  ");
		a.append(toString());
		a.append("\n");
		for (Scope s : children.values())
			s.report(a, indent + 1);
	}

	public void add(Scope m) {
		children.put(m.name, m);

	}

	public void setDeclaring(Scope declaring) {
		this.declaring = declaring;
	}

	public void setAccess(Access access) {
		this.access = access;
	}

	public void setEnclosing(Scope enclosing) {
		this.enclosing = enclosing;
		if (this.enclosing != null) {
			this.enclosing.add(this);
		}
	}

	public boolean isTop() {
		return enclosing == null;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

	static public String classIdentity(String name2) {
		return name2.replace('.', '/');
	}

	static public String methodIdentity(String name, String descriptor) {
		return name + ":" + descriptor;
	}

	static public String constructorIdentity(String descriptor) {
		return ":" + descriptor;
	}

	static public String fieldIdentity(String name, String descriptor) {
		return name + ":" + descriptor;
	}

	public void cleanRoot() {
		children.entrySet()
			.removeIf(entry -> !entry.getValue()
				.isTop());
	}

	public void prune(EnumSet<Access> level) {
		Iterator<Map.Entry<String, Scope>> i = children.entrySet()
			.iterator();
		while (i.hasNext()) {
			Map.Entry<String, Scope> entry = i.next();
			if (!level.contains(entry.getValue().access))
				i.remove();
			else
				entry.getValue()
					.prune(level);
		}
	}

	public void setGenericParameter(GenericParameter[] typeVars) {
		this.typeVars = typeVars;
	}

}
