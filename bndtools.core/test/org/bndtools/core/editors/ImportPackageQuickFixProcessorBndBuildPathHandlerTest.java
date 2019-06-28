package org.bndtools.core.editors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bndtools.core.editors.FakeFile.contentsOf;
import static org.bndtools.core.editors.FakeFile.fakeFile;
import static org.bndtools.core.editors.FakeFile.setContents;
import static org.bndtools.core.editors.ImportPackageQuickFixProcessorTest.DO_NOT_CALL;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.namespace.AbstractWiringNamespace;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class ImportPackageQuickFixProcessorBndBuildPathHandlerTest {

	private ImportPackageQuickFixProcessor.BndBuildPathHandler	sut;

	private List<VersionedClause>								bundles;
	private IFile												fakeFile;
	private final String										BSN	= "my.self.bundle";

	void setBuildPath(List<VersionedClause> bundles) {
		StringBuilder builder = new StringBuilder();
		builder.append("\n-buildpath: \\\n");

		Iterator<VersionedClause> it = bundles.iterator();
		VersionedClause value = it.next();
		while (it.hasNext()) {
			builder.append('\t')
				.append(value)
				.append(",\\\n");
			value = it.next();
		}
		builder.append('\t')
			.append(value);
		setContents(fakeFile, builder.toString());
	}

	@Before
	public void setUp() {
		fakeFile = fakeFile();
		bundles = new ArrayList<>();
		bundles.add(new VersionedClause("my.test.bundle", new Attrs()));
		Attrs attrs = new Attrs();
		attrs.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, "1.2.3");
		bundles.add(new VersionedClause("my.second.bundle", attrs));
		setBuildPath(bundles);

		IProject eclipse = mock(IProject.class, DO_NOT_CALL);
		doReturn(fakeFile).when(eclipse)
			.getFile(Project.BNDFILE);
		doReturn(BSN).when(eclipse)
			.getName();
		IJavaProject jProject = mock(IJavaProject.class, DO_NOT_CALL);
		doReturn(eclipse).when(jProject)
			.getProject();
		doReturn(BSN).when(jProject)
			.getElementName();
		ICompilationUnit unit = mock(ICompilationUnit.class, DO_NOT_CALL);
		doReturn(jProject).when(unit)
			.getJavaProject();
		IInvocationContext context = mock(IInvocationContext.class, DO_NOT_CALL);
		doReturn(unit).when(context)
			.getCompilationUnit();

		sut = new ImportPackageQuickFixProcessor.BndBuildPathHandler(context) {
			@Override
			Workspace getWorkspace() throws Exception {
				return null;
			}
		};
	}

	@Test
	public void getFile_shouldReturnFakeFile() {
		assertThat(sut.getBndFile()).isSameAs(fakeFile);
	}

	@Test
	public void newSUT_doesntLoadModel() throws Exception {
		Field fieldObj = ImportPackageQuickFixProcessor.BndBuildPathHandler.class.getDeclaredField("bndFile");
		fieldObj.setAccessible(true);

		assertThat(fieldObj.get(sut)).as("value")
			.isNull();
	}

	@Test
	public void buildPath_onEmptyFile_shouldReturnEmptyList() throws Exception {
		setContents(fakeFile, "");
		assertThat(sut.getBuildPath()).isEmpty();
	}

	@Test
	public void getBuildPath_containsElements() throws Exception {
		assertThat(sut.getBuildPath()).isEqualTo(bundles);
	}

	@Test
	public void containsBundle_returnsTrue_forContainedBundles() throws Exception {
		for (VersionedClause bundle : bundles) {
			assertThat(sut.containsBundle(bundle.getName())).as("bundle:[" + bundle + "]")
				.isTrue();
		}
	}

	@Test
	public void containsBundle_returnsFalse_forNonContainedBundles() throws Exception {
		for (String bundle : new String[] {
			"some.other.bundle", "something.else"
		}) {
			assertThat(sut.containsBundle(bundle)).as("bundle:[" + bundle + "]")
				.isFalse();
		}
	}

	@Test
	public void containsBundle_returnsTrue_whenBundleIsSelf() throws Exception {
		assertThat(sut.containsBundle(BSN)).isTrue();
	}

	@Test
	public void addBundle_addsBundle() throws Exception {
		assertThat(contentsOf(fakeFile)).doesNotContain("some.other.bundle");
		String init = contentsOf(fakeFile);
		sut.addBundle("some.other.bundle");
		String after = contentsOf(fakeFile);
		assertThat(after).isEqualTo(init + ",\\\n\tsome.other.bundle");
	}
}
