package aQute.bnd.apidiff;

import java.util.*;

import aQute.bnd.service.apidiff.*;
import aQute.libg.version.*;

public abstract class Def<T extends Def<T>> {
	final Type type;
	final Version version;
	final String name;
	
	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		return version;
	}

	public Map<String, ? extends Def> getChildren() {
		return null;
	}
	
	public boolean isAddMajor() {
		return false;
	}

	public Delta compare(T other) {
		return null;
	}
	
	Def(Type type, String name, Version version) {
		this.type = type;
		this.name = name;
		this.version=version;
	}
}
