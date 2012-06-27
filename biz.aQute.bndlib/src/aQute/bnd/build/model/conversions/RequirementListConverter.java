package aQute.bnd.build.model.conversions;

import java.util.Map.Entry;

import org.osgi.resource.Requirement;

import aQute.lib.osgi.resource.CapReqBuilder;
import aQute.libg.header.Attrs;
import aQute.libg.tuple.Pair;

public class RequirementListConverter extends ClauseListConverter<Requirement> {

	public RequirementListConverter() {
		super(new Converter<Requirement,Pair<String,Attrs>>() {
			public Requirement convert(Pair<String,Attrs> input) throws IllegalArgumentException {
				String namespace = input.getFirst();
				CapReqBuilder builder = new CapReqBuilder(namespace);
				for (Entry<String,String> entry : input.getSecond().entrySet()) {
					String key = entry.getKey();
					if (key.endsWith(":")) {
						key = key.substring(0, key.length() - 1);
						builder.addDirective(key, entry.getValue());
					} else {
						builder.addAttribute(key, entry.getValue());
					}
				}
				return builder.buildSyntheticRequirement();
			}
		});
	}

}
