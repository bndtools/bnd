package aQute.bnd.build.model.conversions;

import org.osgi.resource.Requirement;

public class RequirementFormatter extends CapReqFormatter implements Converter<String, Requirement> {

	@Override
	public String convert(Requirement req) {
		return convert(req.getNamespace(), req.getDirectives(), req.getAttributes());
	}
}
