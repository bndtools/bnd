package test.typeuse;

import java.util.List;
import java.util.Map;

public class TypePath {
	@A
	Map<@B ? extends @C String, @D List<@E Object>>				b;

	@I
	String @F [] @G [] @H []									c;

	@A
	List<@B Comparable<@F Object @C [] @D [] @E []>>			d;

	@C
	OuterE.@B Middle.@A Inner									e;

	OuterF.Middle<@D OuterE.@C Middle>.Inner<@B String @A []>	f;
}
