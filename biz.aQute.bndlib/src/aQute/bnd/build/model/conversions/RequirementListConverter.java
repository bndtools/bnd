package aQute.bnd.build.model.conversions;

import java.util.Map.Entry;

import org.osgi.resource.Requirement;

import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class RequirementListConverter extends HeaderClauseListConverter<Requirement> {

	public RequirementListConverter() {
		super(new Converter<Requirement, HeaderClause>() {
			@Override
			public Requirement convert(HeaderClause input) {
				if (input == null)
					return null;
				String namespace = input.getName();
				CapReqBuilder builder = new CapReqBuilder(namespace);
				for (Entry<String, String> entry : input.getAttribs()
					.entrySet()) {
					String key = entry.getKey();
					if (key.endsWith(":")) {
						key = key.substring(0, key.length() - 1);
						builder.addDirective(key, entry.getValue());
					} else {
						try {
							builder.addAttribute(key, entry.getValue());
						} catch (Exception e) {
							throw new IllegalArgumentException(e);
						}
					}
				}
				return builder.buildSyntheticRequirement();
			}

			@Override
			public Requirement error(String msg) {
				CapReqBuilder builder = new CapReqBuilder(msg);
				return builder.buildSyntheticRequirement();
			}
		});
	}

}
