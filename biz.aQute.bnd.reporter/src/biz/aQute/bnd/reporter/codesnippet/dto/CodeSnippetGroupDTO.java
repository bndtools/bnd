package biz.aQute.bnd.reporter.codesnippet.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A code snippet with multiple steps.
 */
public class CodeSnippetGroupDTO extends CodeSnippetDTO {

	/**
	 * A list of code snippet steps.
	 */
	public List<CodeSnippetProgramDTO> steps = new LinkedList<>();
}
