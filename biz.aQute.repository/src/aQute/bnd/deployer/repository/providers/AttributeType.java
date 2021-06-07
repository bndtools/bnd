package aQute.bnd.deployer.repository.providers;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.strings.Strings;

public class AttributeType {

	public static final AttributeType	STRING				= new AttributeType(false, ScalarType.String);
	public static final AttributeType	STRINGLIST			= new AttributeType(true, ScalarType.String);
	public static final AttributeType	LONG				= new AttributeType(false, ScalarType.Long);
	public static final AttributeType	LONGLIST			= new AttributeType(true, ScalarType.Long);
	public static final AttributeType	DOUBLE				= new AttributeType(false, ScalarType.Double);
	public static final AttributeType	DOUBLELIST			= new AttributeType(true, ScalarType.Double);
	public static final AttributeType	VERSION				= new AttributeType(false, ScalarType.Version);
	public static final AttributeType	VERSIONLIST			= new AttributeType(true, ScalarType.Version);

	public static final AttributeType	DEFAULT				= STRING;

	private static final Pattern		LIST_TYPE_PATTERN	= Pattern.compile("List(\\s*<\\s*(\\w+)\\s*>)?");

	private final boolean				list;
	private final ScalarType			baseType;

	public static AttributeType parseTypeName(String typeName) throws IllegalArgumentException {
		if (typeName == null)
			return DEFAULT;

		typeName = typeName.trim();
		Matcher matcher = LIST_TYPE_PATTERN.matcher(typeName);
		if (matcher.matches()) {
			if (matcher.group(1) == null) {
				return STRINGLIST;
			}
			String scalarTypeName = matcher.group(2);
			ScalarType scalarType = ScalarType.valueOf(scalarTypeName);
			return new AttributeType(true, scalarType);
		}

		ScalarType scalarType = ScalarType.valueOf(typeName);
		return new AttributeType(false, scalarType);
	}

	public AttributeType(boolean list, ScalarType baseType) {
		this.list = list;
		this.baseType = baseType;
	}

	public boolean isList() {
		return list;
	}

	public ScalarType getBaseType() {
		return baseType;
	}

	public Object parseString(String input) {
		if (list) {
			List<Object> list = Strings.splitAsStream(input)
				.map(baseType::parseString)
				.collect(toList());
			return list;
		}

		return baseType.parseString(input);
	}

	@Override
	public String toString() {
		String output;
		if (list) {
			output = "List<" + baseType.toString() + ">";
		} else {
			output = baseType.toString();
		}
		return output;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());
		result = prime * result + (list ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeType other = (AttributeType) obj;
		if (baseType != other.baseType)
			return false;
		if (list != other.list)
			return false;
		return true;
	}

}
