package aQute.bnd.build.model.conversions;

import java.util.Map.Entry;

import org.osgi.resource.*;

import aQute.bnd.osgi.resource.*;

public class RequirementFormatter implements Converter<String,Requirement> {

	public String convert(Requirement req) throws IllegalArgumentException {
		StringBuilder builder = new StringBuilder();
		
		builder.append(req.getNamespace());
		
		if (req instanceof RequirementVariable)
			return builder.toString();

		for (Entry<String,String> directive : req.getDirectives().entrySet()) {
			builder.append(';').append(directive.getKey()).append(":='").append(directive.getValue()).append('\'');
		}
		
		for (Entry<String,Object> attribute : req.getAttributes().entrySet()) {
			builder.append(';').append(attribute.getKey()).append("='").append(attribute.getValue()).append('\'');
		}
		
		return builder.toString();
	}

}
