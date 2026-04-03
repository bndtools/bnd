package bndtools.core.test.editors.quickfix;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BaseModelTest extends AbstractBuildpathQuickFixProcessorTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		clearBuildpath();
	}

	@Test
	void test() throws JavaModelException {
		ICompilationUnit icu = pack.createCompilationUnit("Test.java", "package foo;\n", true, null);
		assertThat(icu).isNotNull();
	}

}
