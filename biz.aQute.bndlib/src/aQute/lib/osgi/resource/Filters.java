package aQute.lib.osgi.resource;

import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.NotFilter;
import aQute.libg.filters.Operator;
import aQute.libg.filters.SimpleFilter;
import aQute.libg.version.VersionRange;

public class Filters {
	
	public static Filter fromVersionRange(VersionRange range) {
		return fromVersionRange(range, "version");
	}
	
	public static Filter fromVersionRange(VersionRange range, @SuppressWarnings("unused") String versionAttr) {
		if (range == null)
			return null;
		
		Filter left;
		if (range.includeLow())
			left = new SimpleFilter("version", Operator.GreaterThanOrEqual, range.getLow().toString());
		else
			left = new NotFilter(new SimpleFilter("version", Operator.LessThanOrEqual, range.getLow().toString()));
		
		Filter right;
		if (!range.isRange())
			right = null;
		else if (range.includeHigh())
			right = new SimpleFilter("version", Operator.LessThanOrEqual, range.getHigh().toString());
		else
			right = new NotFilter(new SimpleFilter("version", Operator.GreaterThanOrEqual, range.getHigh().toString()));
		
		Filter result;
		if (right != null)
			result = new AndFilter().addChild(left).addChild(right);
		else
			result = left;
		
		return result;
	}
}
