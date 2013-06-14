package bndtools.refactor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.workspace.FileUtils;
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
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;


public class PkgRenameParticipant extends RenameParticipant implements ISharableParticipant {
    private static final ILogger logger = Logger.getLogger(PkgRenameParticipant.class);

    private final Map<IPackageFragment,RenameArguments> pkgFragments = new HashMap<IPackageFragment,RenameArguments>();
    private String changeTitle = null;

    @Override
    protected boolean initialize(Object element) {
        IPackageFragment pkgFragment = (IPackageFragment) element;
        RenameArguments args = getArguments();
        pkgFragments.put(pkgFragment, args);

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

    public void addElement(Object element, RefactoringArguments arguments) {
        this.pkgFragments.put((IPackageFragment) element, (RenameArguments) arguments);
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
        final Map<IFile,TextChange> fileChanges = new HashMap<IFile,TextChange>();

        IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
            public boolean visit(IResourceProxy proxy) throws CoreException {
                if ((proxy.getType() == IResource.FOLDER) || (proxy.getType() == IResource.PROJECT)) {
                    return true;
                }

                if (!((proxy.getType() == IResource.FILE) && proxy.getName().toLowerCase().endsWith(".bnd"))) {
                    return false;
                }

                /* we're dealing with a *.bnd file */

                /* get the proxied file */
                IFile resource = (IFile) proxy.requestResource();

                /* read the file as a single string */
                String bndFileText = null;
                try {
                    bndFileText = FileUtils.readFully(resource).get();
                } catch (Exception e) {
                    String str = "Could not read file " + proxy.getName();
                    logger.logError(str, e);
                    throw new OperationCanceledException(str);
                }

                /*
                 * get the previous change for this file if it exists, or otherwise create a new change for it, but do
                 * not store it yet: wait until we know if there are actually changes in the file
                 */
                TextChange fileChange = getTextChange(resource);
                final boolean fileChangeIsNew = (fileChange == null);
                if (fileChange == null) {
                    fileChange = new TextFileChange(proxy.getName(), resource);
                    fileChange.setEdit(new MultiTextEdit());
                }
                TextEdit rootEdit = fileChange.getEdit();

                /* loop over all renames to perform */
                for (Map.Entry<IPackageFragment,RenameArguments> entry : pkgFragments.entrySet()) {
                    IPackageFragment pkgFragment = entry.getKey();
                    RenameArguments arguments = entry.getValue();

                    final String oldName = pkgFragment.getElementName();
                    final String newName = arguments.getNewName();

                    Pattern pattern = Pattern.compile(
                    /* match start boundary */"(^|" + grammarSeparator + ")" +
                    /* match itself / package name */"(" + Pattern.quote(oldName) + ")" +
                    /* match end boundary */"(" + grammarSeparator + "|" + Pattern.quote(".*") + "|" + Pattern.quote("\\") + "|$)");

                    /* find all matches to replace and add them to the root edit */
                    Matcher matcher = pattern.matcher(bndFileText);
                    while (matcher.find()) {
                        rootEdit.addChild(new ReplaceEdit(matcher.start(2), matcher.group(2).length(), newName));
                    }

                    pattern = Pattern.compile(
                    /* match start boundary */"(^|" + grammarSeparator + ")" +
                    /* match bundle activator */"(Bundle-Activator\\s*:\\s*)" +
                    /* match itself / package name */"(" + Pattern.quote(oldName) + ")" +
                    /* match class name */"(\\.[^\\.]+)" +
                    /* match end boundary */"(" + grammarSeparator + "|" + Pattern.quote("\\") + "|$)");

                    /* find all matches to replace and add them to the root edit */
                    matcher = pattern.matcher(bndFileText);
                    while (matcher.find()) {
                        rootEdit.addChild(new ReplaceEdit(matcher.start(3), matcher.group(3).length(), newName));
                    }
                }

                /*
                 * only store the changes when no changes were stored before for this file and when there are actually
                 * changes.
                 */
                if (fileChangeIsNew && rootEdit.hasChildren()) {
                    fileChanges.put(resource, fileChange);
                }

                return false;
            }
        };

        /* determine which projects have to be visited */
        Set<IProject> projectsToVisit = new HashSet<IProject>();
        for (IPackageFragment pkgFragment : pkgFragments.keySet()) {
            projectsToVisit.add(pkgFragment.getResource().getProject());
            for (IProject projectToVisit : pkgFragment.getResource().getProject().getReferencingProjects()) {
                projectsToVisit.add(projectToVisit);
            }
            for (IProject projectToVisit : pkgFragment.getResource().getProject().getReferencedProjects()) {
                projectsToVisit.add(projectToVisit);
            }
        }

        /* visit the projects */
        for (IProject projectToVisit : projectsToVisit) {
            projectToVisit.accept(visitor, IContainer.NONE);
        }

        if (fileChanges.isEmpty()) {
            /* no changes at all */
            return null;
        }

        /* build a composite change with all changes */
        CompositeChange cs = new CompositeChange(changeTitle);
        for (TextChange fileChange : fileChanges.values()) {
            cs.add(fileChange);
        }

        return cs;
    }
}
