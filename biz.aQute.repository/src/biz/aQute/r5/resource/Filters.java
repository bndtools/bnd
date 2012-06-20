package biz.aQute.r5.resource;

import biz.aQute.r5.resource.filters.AndFilter;
import biz.aQute.r5.resource.filters.Filter;
import biz.aQute.r5.resource.filters.NotFilter;
import biz.aQute.r5.resource.filters.Operator;
import biz.aQute.r5.resource.filters.SimpleFilter;
import aQute.libg.version.VersionRange;

public class Filters {
	
	public static Filter fromVersionRange(VersionRange range) {
		return fromVersionRange(range, "version");
	}
	
	public static Filter fromVersionRange(VersionRange range, String versionAttr) {
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
