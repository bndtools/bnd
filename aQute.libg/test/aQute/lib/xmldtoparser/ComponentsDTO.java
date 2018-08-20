package aQute.lib.xmldtoparser;

import java.util.List;

public class ComponentsDTO {

	public List<TComponent> component;

	static public class TComponent {
		public List<TProperty>		property;
		public List<TProperties>	properties;
		public TService				service;
		public List<TReference>		reference;
		public TImplementation		implementation;
		public boolean				enabled;
		public String				name;
		public String				factory;
		public boolean				immediate;
		@XmlAttribute(name = "configuration-policy")
		public TConfigurationPolicy	configurationPolicy	= TConfigurationPolicy.optional;
		public String				activate			= "activate";
		public String				deactivate			= "deactivate";
		public String				modified			= "modified";
		@XmlAttribute(name = "configuration-pid")
		public String				configurationPid;

	}

	static public class TImplementation {
		@XmlAttribute(name = "class")
		public String clazz;
	}

	static public class TProperty {
		public String		name;
		public String		value;
		public TJavaTypes	type	= TJavaTypes.String;
		public String		_content;

	}

	static public class TProperties {
		public String entry;
	}

	static public class TService {
		public boolean			servicefactory	= false;
		public List<TProvide>	provide;
	}

	static public class TProvide {
		@XmlAttribute(name = "interface")
		public String interface_;
	}

	static public class TReference {
		public String			name;
		@XmlAttribute(name = "interface")
		public String			interface_;
		public TCardinality		cardinality		= TCardinality.mandatory;
		public TPolicy			policy			= TPolicy.static_;
		@XmlAttribute(name = "policy-option")
		public TPolicyOption	policyOption	= TPolicyOption.reluctant;
		public String			target;
		public String			bind;
		public String			unbind;
		public String			updated;
	}

	public enum TJavaTypes {
		String,
		Long,
		Double,
		Float,
		Integer,
		Byte,
		Character,
		Boolean,
		Short;
	}

	public enum TCardinality {
		@XmlAttribute(name = "0..1")
		optional, // 0..1
		@XmlAttribute(name = "0..n")
		multiple, // 0..n
		@XmlAttribute(name = "1..1")
		mandatory, // 1..1
		@XmlAttribute(name = "1..n")
		atLeastOne, // 1..n
	}

	public enum TPolicy {
		@XmlAttribute(name = "static")
		static_,
		dynamic
	}

	public enum TPolicyOption {
		reluctant,
		greedy
	}

	public enum TConfigurationPolicy {
		optional,
		require,
		ignore
	}
}
