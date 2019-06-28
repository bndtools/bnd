package aQute.lib.dtoformatter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class GroupDescription {
	String								title;
	final Map<String, ItemDescription>	items		= new LinkedHashMap<>();
	String								separator	= ",";
	String								prefix		= "[";
	String								suffix		= "]";
	public Function<Object, String>		format;
}
