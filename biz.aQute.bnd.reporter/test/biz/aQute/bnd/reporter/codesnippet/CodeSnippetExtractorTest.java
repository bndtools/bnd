package biz.aQute.bnd.reporter.codesnippet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetDTO;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetGroupDTO;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetProgramDTO;

public class CodeSnippetExtractorTest {

	@Test
	public void testExtractor() {
		final CodeSnippetExtractor extractor = new CodeSnippetExtractor();

		final List<CodeSnippetDTO> actual = extractor
			.extract(new File("test/biz/aQute/bnd/reporter/codesnippet/examples").getPath());

		assertEquals(3, actual.size());
		assertEquals("SingleFirst", actual.get(0).id);
		assertTrue(actual.get(0) instanceof CodeSnippetProgramDTO);
		assertEquals("GroupSecond", actual.get(1).id);
		assertTrue(actual.get(1) instanceof CodeSnippetGroupDTO);

		final CodeSnippetGroupDTO g = (CodeSnippetGroupDTO) actual.get(1);
		assertEquals(3, g.steps.size());
		assertEquals("ChildFirst", g.steps.get(0).id);
		assertEquals("GroupSecond1", g.steps.get(1).id);
		assertEquals("GXChildSecond", g.steps.get(2).id);

		assertEquals("SingleThird", actual.get(2).id);
		assertTrue(actual.get(2) instanceof CodeSnippetProgramDTO);
	}
}
