package bndtools.wizards.newworkspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class NewWorkspaceWizardTest {

	@Test
	void plainTextGlobMatchesSubstring() {
		Pattern pattern = Pattern.compile(NewWorkspaceWizard.NewWorkspaceWizardPage.globToRegex("eclipse"),
			Pattern.CASE_INSENSITIVE);

		assertThat(pattern.matcher("Eclipse Workspace").matches()).isTrue();
		assertThat(pattern.matcher("workspace").matches()).isFalse();
	}

	@Test
	void wildcardGlobMatchesExpectedText() {
		Pattern pattern = Pattern.compile(NewWorkspaceWizard.NewWorkspaceWizardPage.globToRegex("*eclipse?"),
			Pattern.CASE_INSENSITIVE);

		assertThat(pattern.matcher("bndtools eclipseX").matches()).isTrue();
		assertThat(pattern.matcher("bndtools eclipse").matches()).isFalse();
	}

	@Test
	void regexCharactersRemainLiteral() {
		Pattern pattern = Pattern.compile(NewWorkspaceWizard.NewWorkspaceWizardPage.globToRegex("a[b].c"),
			Pattern.CASE_INSENSITIVE);

		assertThat(pattern.matcher("template a[b].c").matches()).isTrue();
		assertThat(pattern.matcher("template abc").matches()).isFalse();
	}
}
