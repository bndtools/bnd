package bndtools.refactor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public class AdoptMavenPomProcessor extends RefactoringProcessor {

    private static final String POM_FILE_NAME = "pom.xml";

    private final IFile pomFile;

    public AdoptMavenPomProcessor(IFile pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public Object[] getElements() {
        return new Object[] { pomFile };
    }

    @Override
    public String getIdentifier() {
        return "org.bndtools.core.refactor.adoptMavenPomProcessor"; //$NON-NLS-1$
    }

    @Override
    public String getProcessorName() {
        return "Add OSGi build to Maven POM";
    }

    @Override
    public boolean isApplicable() throws CoreException {
        return isValidPomFile();
    }

    private boolean isValidPomFile() {
        // Must be named pom.xml and be at the root of a project
        return pomFile != null && POM_FILE_NAME.equals(pomFile.getName()) && pomFile.getParent().getType() == IResource.PROJECT;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        TextFileChange change = new TextFileChange("Add OSGi outputs to POM", pomFile);

        SubMonitor progress = SubMonitor.convert(pm, 100);
        change.getCurrentDocument(progress.newChild0)
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

}
