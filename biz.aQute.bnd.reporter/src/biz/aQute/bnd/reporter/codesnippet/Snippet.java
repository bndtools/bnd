package biz.aQute.bnd.reporter.codesnippet;

import java.util.Objects;

import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetGroupDTO;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetProgramDTO;

class Snippet {

	private final String				_groupName;
	private final CodeSnippetGroupDTO	_groupDto;

	private final String				_parentGroup;
	private final CodeSnippetProgramDTO	_programDto;

	public Snippet(final String groupName, final CodeSnippetGroupDTO dto) {
		this(Objects.requireNonNull(groupName, "groupName"), Objects.requireNonNull(dto, "dto"), null, null);
	}

	public Snippet(final String parentName, final CodeSnippetProgramDTO dto) {
		this(null, null, parentName, Objects.requireNonNull(dto, "dto"));
	}

	public Snippet(final CodeSnippetProgramDTO dto) {
		this(null, null, null, Objects.requireNonNull(dto, "dto"));
	}

	private Snippet(final String groupName, final CodeSnippetGroupDTO groupDto, final String parentGroup,
		final CodeSnippetProgramDTO programDto) {
		super();
		_groupName = groupName;
		_groupDto = groupDto;
		_parentGroup = parentGroup;
		_programDto = programDto;
	}

	public String getGroupName() {
		return _groupName;
	}

	public String getParentGroup() {
		return _parentGroup;
	}

	public CodeSnippetProgramDTO getCodeSnippetProgram() {
		return _programDto;
	}

	public CodeSnippetGroupDTO getCodeSnippetGroup() {
		return _groupDto;
	}
}
