package aQute.maven.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.maven.api.Archive;
import aQute.maven.provider.TrustedChecksums.ArtifactChecksum;

public class TrustedChecksumsTest {
	@Test
	public void testParseValidChecksum() throws Exception {
	    String line = "org.example:artifact:1.0=sha256:abc123def456";
	    ArtifactChecksum ac = ArtifactChecksum.parse(line);
	    assertEquals(Archive.valueOf("org.example:artifact:1.0"), ac.archive());
	    assertEquals("sha256", ac.hashType());
	    assertEquals("abc123def456", ac.hash());
	}

	@Test
	public void testParseValidChecksum2() throws Exception {
		// check if trimming works correctly
		String source = " #comment line \n" + " org.example:artifact:1.0 = sha256 : abc123def456  ";
		List<ArtifactChecksum> list = TrustedChecksums.read(source);
		assertThat(list).hasSize(1);

		ArtifactChecksum ac = list.get(0);
		assertEquals(Archive.valueOf("org.example:artifact:1.0"), ac.archive());
		assertEquals("sha256", ac.hashType());
		assertEquals("abc123def456", ac.hash());
	}

	@Test
	public void testParseInvalidFormat() throws Exception {
	    String invalidLine = "invalid_format_no_equals";
	    assertThrows(IllegalArgumentException.class, () ->
	        ArtifactChecksum.parse(invalidLine)
	    );
	}
}

