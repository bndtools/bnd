package aQute.lib.xmldtoparser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Test;

import aQute.lib.io.IO;

public class DomDTOParserTest {

	@Test
	public void testParseT1() throws Exception {
		File f = IO.getFile("testresources/dsxmls/t1.xml");
		ComponentsDTO component = DomDTOParser.parse(ComponentsDTO.class, f);
		assertThat(component).isNotNull();

	}
}
