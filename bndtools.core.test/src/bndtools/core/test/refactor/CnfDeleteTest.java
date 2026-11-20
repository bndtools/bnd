package bndtools.core.test.refactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourcesDescriptor;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import aQute.lib.io.IO;

@ResourceLock(bndtools.core.test.utils.ResourceLock.TEST_WORKSPACE)
class CnfDeleteTest {

	@TempDir
	File tmp;

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void deletesGeneratedFilesOnlyWhenDeletingContents(boolean deleteContents) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String name = "cnf-delete-" + UUID.randomUUID();
		IProject project = workspace.getRoot().getProject(name);
		File cnf = new File(tmp, "cnf");
		IProjectDescription description = workspace.newProjectDescription(name);
		description.setLocation(new Path(cnf.getAbsolutePath()));
		project.create(description, null);
		try {
			project.open(null);
			File index = new File(cnf, "ext/eclipse_4.41_platform_p2.index.xml.gz");
			IO.mkdirs(index.getParentFile());
			IO.store("generated index", index);
			File nested = new File(cnf, "cache/nested/generated.bin");
			IO.mkdirs(nested.getParentFile());
			IO.store("generated cache", nested);
			assertThat(project.getFile("ext/eclipse_4.41_platform_p2.index.xml.gz").exists()).isFalse();

			DeleteResourcesDescriptor descriptor = new DeleteResourcesDescriptor();
			descriptor.setResources(new IResource[] {project});
			descriptor.setDeleteContents(deleteContents);
			RefactoringStatus status = new RefactoringStatus();
			Refactoring refactoring = descriptor.createRefactoring(status);
			assertThat(status.hasFatalError()).as(status.toString()).isFalse();
			NullProgressMonitor monitor = new NullProgressMonitor();
			status = refactoring.checkAllConditions(monitor);
			assertThat(status.hasFatalError()).as(status.toString()).isFalse();
			assertThat(project.getFile("ext/eclipse_4.41_platform_p2.index.xml.gz").exists()).isTrue();

			PerformChangeOperation operation = new PerformChangeOperation(refactoring.createChange(monitor));
			workspace.run(operation, monitor);

			assertThat(operation.changeExecuted()).isTrue();
			assertThat(project.exists()).isFalse();
			assertThat(cnf.exists()).isEqualTo(!deleteContents);
			assertThat(index.exists()).isEqualTo(!deleteContents);
			assertThat(nested.exists()).isEqualTo(!deleteContents);
		} finally {
			if (project.exists()) {
				project.delete(true, true, null);
			}
		}
	}
}