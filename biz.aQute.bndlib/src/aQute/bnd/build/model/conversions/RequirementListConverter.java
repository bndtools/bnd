package aQute.bnd.build.model.conversions;

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
				try {
					CapReqBuilder builder = CapReqBuilder.createCapReqBuilder(input.getName(), input.getAttribs());
					return builder.buildSyntheticRequirement();
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
			}

			@Override
			public Requirement error(String msg) {
				CapReqBuilder builder = new CapReqBuilder(msg);
				return builder.buildSyntheticRequirement();
			}
		});
	}

}
