package aQute.lib.dtoformatter;

import java.util.function.Function;

public class ItemDescription {
	ItemDescription(String name) {
		this.label = name;
	}

	Function<Object, Object>	member;
	String						label;
	int							maxWidth	= Integer.MAX_VALUE;
	int							minWidth	= 0;
	boolean						self;
	Function<Object, Object>	format;
}
