package aQute.bnd.build.model.conversions;

import org.osgi.resource.Capability;

public class CapabilityFormatter extends CapReqFormatter implements Converter<String, Capability> {

	@Override
	public String convert(Capability cap) {
		return convert(cap.getNamespace(), cap.getDirectives(), cap.getAttributes());
	}
}
