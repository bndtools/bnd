package org.bndtools.core.ui.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.viewers.StyledString;
import org.junit.jupiter.api.Test;
import org.osgi.resource.Requirement;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class R5LabelFormatterTest {

	@Test
	public void testIdentityFeatureLabelShowsExactVersion() {
		Attrs attrs = new Attrs();
		attrs.putTyped("id", "org.eclipse.rcp");
		attrs.putTyped("type", "org.eclipse.update.feature");
		attrs.putTyped("version", "4.37.0.v20250905-0730");

		Requirement req = CapReqBuilder.getRequirementFrom("bnd.identity", attrs);
		StyledString label = new StyledString();

		R5LabelFormatter.appendRequirementLabel(label, req, true);

		assertEquals("org.eclipse.rcp 4.37.0.v20250905-0730", label.getString());
	}

	@Test
	public void testIdentityFeatureLabelShowsVersionRange() {
		Attrs attrs = new Attrs();
		attrs.putTyped("id", "org.eclipse.rcp");
		attrs.putTyped("type", "org.eclipse.update.feature");
		attrs.putTyped("version", "[4.37.0.v20250905-0730,4.38.0)");

		Requirement req = CapReqBuilder.getRequirementFrom("bnd.identity", attrs);
		StyledString label = new StyledString();

		R5LabelFormatter.appendRequirementLabel(label, req, true);

		assertEquals("org.eclipse.rcp [4.37.0.v20250905-0730,4.38.0)", label.getString());
	}
}
