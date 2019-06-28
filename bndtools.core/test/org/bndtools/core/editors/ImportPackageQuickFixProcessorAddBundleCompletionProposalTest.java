package org.bndtools.core.editors;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ImportPackageQuickFixProcessorAddBundleCompletionProposalTest {

	private static final String											BUNDLE		= "my.bundle";
	private static final String											REPO1		= "Repo 1";
	private static final String											REPO2		= "Repo 2";
	private static final String											WORKSPACE	= "Workspace Bndtools repo";
	private static final int											RELEVANCE	= 2;

	private ImportPackageQuickFixProcessor.AddBundleCompletionProposal	sut;

	@Before
	public void setUp() {
		setUpWithRepos(REPO1);
	}

	public void setUpWithRepos(String... repos) {
		List<String> r = new ArrayList<>(repos.length);
		for (String repo : repos) {
			r.add(repo);
		}
		sut = new ImportPackageQuickFixProcessor().new AddBundleCompletionProposal(BUNDLE, r, RELEVANCE, null);
	}

	@Test
	public void displayString_returnsDescription() {
		assertThat(sut.getDisplayString())
			.isEqualTo(String.format("Add bundle '%s' to Bnd build path (from %s)", BUNDLE, REPO1));
	}

	@Test
	public void displayString_withTwoRepos_returnsDescription() {
		setUpWithRepos(REPO2, REPO1);
		assertThat(sut.getDisplayString())
			.isEqualTo(String.format("Add bundle '%s' to Bnd build path (from %s + 1 other)", BUNDLE, REPO2));
	}

	@Test
	public void displayString_withThreeRepos_returnsDescription() {
		setUpWithRepos(WORKSPACE, REPO2, REPO1);
		assertThat(sut.getDisplayString())
			.isEqualTo(String.format("Add bundle '%s' to Bnd build path (from %s + 2 others)", BUNDLE, WORKSPACE));
	}

	@Test
	public void getRelevance_returnsRelevance() {
		assertThat(sut.getRelevance()).isEqualTo(RELEVANCE);
	}
}
