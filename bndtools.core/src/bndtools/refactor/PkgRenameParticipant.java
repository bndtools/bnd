package bndtools.refactor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import bndtools.Plugin;
import bndtools.utils.FileUtils;

public class PkgRenameParticipant extends RenameParticipant {
    private IPackageFragment pkgFragment;
    private String changeTitle = null;

    @Override
    protected boolean initialize(Object element) {
        this.pkgFragment = (IPackageFragment) element;
        RenameArguments args = getArguments();

        StringBuilder sb = new StringBuilder(256);
        sb.append("Bndtools: rename package '");
        sb.append(pkgFragment.getElementName());
        sb.append("' ");
        if (((RenamePackageProcessor) this.getProcessor()).getRenameSubpackages())
            sb.append("and subpackages ");
        sb.append("to '");
        sb.append(args.getNewName());
        sb.append("'");
        changeTitle = sb.toString();

        return true;
    }

    @Override
    public String getName() {
        return "Bndtools Package Rename Participant";
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
        return new RefactoringStatus();
    }

    private static final String grammarSeparator = "[\\s,\"';]";

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        final String oldName = pkgFragment.getElementName();
        final String newName = getArguments().getNewName();
        final Pattern pattern = Pattern.compile(
        /* match start boundary */"(^|" + grammarSeparator + ")" +
        /* match itself / package name */"(" + Pattern.quote(oldName) + ")" +
        /* match end boundary */"(" + grammarSeparator + "|" + Pattern.quote(".*") + "|" + Pattern.quote("\\") + "|$)");

        final Map<IFile, TextChange> fileChanges = new HashMap<IFile, TextChange>();

        IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
            public boolean visit(IResourceProxy proxy) throws CoreException {
                if ((proxy.getType() == IResource.FOLDER) || (proxy.getType() == IResource.PROJECT))
                    return true;
                if ((proxy.getType() == IResource.FILE) && proxy.getName().toLowerCase().endsWith(".bnd")) {
                    /* we're dealing with a *.bnd file */
                    IFile resource = (IFile) proxy.requestResource();

                    /* read the file */
                    String bndFileText = null;
                    try {
                        bndFileText = FileUtils.readFully(resource).get();
                    } catch (Exception e) {
                        String str = "Could not read file " + proxy.getName();
                        Plugin.logError(str, e);
                        throw new OperationCanceledException(str);
                    }

                    /* see if there are matches, if not: return */
                    Matcher matcher = pattern.matcher(bndFileText);
                    if (!matcher.find()) {
                        return false;
                    }

                    /*
                     * get the previous change for this file if it exists, or
                     * otherwise create a new change for it
                     */
                    TextChange fileChange = getTextChange(resource);
                    TextEdit rootEdit = null;
                    if (fileChange == null) {
                        fileChange = new TextFileChange(proxy.getName(), resource);
                        rootEdit = new MultiTextEdit();
                        fileChange.setEdit(rootEdit);
                        fileChanges.put(resource, fileChange);
                    } else {
                        rootEdit = fileChange.getEdit();
                    }

                    /* find all matches to replace and add them to the root edit */
                    matcher.reset();
                    while (matcher.find()) {
                        rootEdit.addChild(new ReplaceEdit(matcher.start(2), matcher.group(2).length(), newName));
                    }
                }
                return false;
            }
        };

        Set<IProject> visitedProjects = new HashSet<IProject>();

        IProject containingProject = pkgFragment.getResource().getProject();
        containingProject.accept(visitor, IContainer.NONE);
        visitedProjects.add(containingProject);

        for (IProject project : pkgFragment.getResource().getProject().getReferencingProjects()) {
            project.accept(visitor, IContainer.NONE);
            visitedProjects.add(project);
        }

        for (IProject project : pkgFragment.getResource().getProject().getReferencedProjects()) {
            if (!visitedProjects.contains(project)) {
                project.accept(visitor, IContainer.NONE);
                visitedProjects.add(project);
            }
        }

        if (fileChanges.isEmpty())
            return null;

        CompositeChange cs = new CompositeChange(changeTitle);
        for (TextChange fileChange : fileChanges.values()) {
            cs.add(fileChange);
        }

        return cs;
    }
}
