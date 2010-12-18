package bndtools.refactor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import bndtools.Plugin;
import bndtools.utils.FileUtils;

public class PkgRenameParticipant extends RenameParticipant {

    private IPackageFragment pkgFragment;

    @Override
    protected boolean initialize(Object element) {
        this.pkgFragment = (IPackageFragment) element;
        return true;
    }

    @Override
    public String getName() {
        return "Bnd descriptor participant";
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        IProject project = pkgFragment.getResource().getProject();
        final String newName = getArguments().getNewName();

        final CompositeChange compositeChange = new CompositeChange("Update package references in Bnd files");
        IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
            public boolean visit(IResourceProxy proxy) throws CoreException {
                if (proxy.getType() == IResource.FOLDER || proxy.getType() == IResource.PROJECT)
                    return true;
                if (proxy.getType() == IResource.FILE && proxy.getName().toLowerCase().endsWith(".bnd")) {
                    try {
                        IFile bndFile = (IFile) proxy.requestResource();
                        List<IRegion> matches = findMatches(bndFile);
                        if (!matches.isEmpty()) {
                            MultiTextEdit edit = new MultiTextEdit();
                            for (IRegion match : matches) {
                                edit.addChild(new ReplaceEdit(match.getOffset(), match.getLength(), newName));
                            }
                            TextFileChange change = new TextFileChange("Replace package references", bndFile);
                            change.setEdit(edit);
                            compositeChange.add(change);
                        }
                    } catch (IOException e) {
                        Plugin.logError("Error searching for package references in " + proxy.getName(), e);
                    } catch (CoreException e) {
                        Plugin.logError("Error searching for package references in " + proxy.getName(), e);
                    }
                }
                return false;
            }
        };
        project.accept(visitor, IContainer.NONE);

        if (compositeChange.getChildren().length > 0)
            return compositeChange;

        return null;
    }

    private List<IRegion> findMatches(IFile bndFile) throws IOException, CoreException {
        String pkgName = pkgFragment.getElementName();
        String regExp = pkgName.replace(".", "\\.");
        Pattern pattern = Pattern.compile(regExp);

        String text = FileUtils.readFully(bndFile).get();
        Matcher matcher = pattern.matcher(text);

        List<IRegion> matches = new LinkedList<IRegion>();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Skip matches beginning with dot
            if (text.charAt(start) != '.')
                matches.add(new Region(start, end - start));
        }
        return matches;
    }

}
