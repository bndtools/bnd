package org.bndtools.core.ui.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	public void testFeatureMemberLabelShowsPlatform() {
		Attrs attrs = new Attrs();
		attrs.putTyped("id", "org.eclipse.equinox.launcher.cocoa.macosx.x86_64");
		attrs.putTyped("version", "1.2.1200.v20250212-0927");
		attrs.putTyped("bnd.relation", "plugin");
		attrs.putTyped("os", "macosx");
		attrs.putTyped("ws", "cocoa");
		attrs.putTyped("arch", "x86_64");

		Requirement req = CapReqBuilder.getRequirementFrom("bnd.identity", attrs);
		StyledString label = new StyledString();

		R5LabelFormatter.appendRequirementLabel(label, req, true);

		assertTrue(label.getString()
			.startsWith("org.eclipse.equinox.launcher.cocoa.macosx.x86_64 "), label.getString());
		assertTrue(label.getString()
			.endsWith(" [macosx/cocoa/x86_64]"), label.getString());
	}

	@Test
	public void testRequirementWithoutRelationShowsNoPlatform() {
		Attrs attrs = new Attrs();
		attrs.putTyped("id", "org.example");
		attrs.putTyped("os", "macosx");

		Requirement req = CapReqBuilder.getRequirementFrom("bnd.identity", attrs);
		StyledString label = new StyledString();

		R5LabelFormatter.appendRequirementLabel(label, req, true);

		assertEquals("org.example", label.getString());
	}
}
