package bndtools.refactor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.bndtools.core.xml.LightweightDocument;
import org.bndtools.core.xml.LightweightDocumentBuilder;
import org.bndtools.core.xml.LightweightElement;
import org.bndtools.core.xml.TagLocation;
import org.bndtools.core.xml.XmlSearch;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.builder.BndProjectNature;
import bndtools.utils.TextUtils;

public class AdoptMavenPomProcessor extends RefactoringProcessor {

    private static final String POM_FILE_NAME = "pom.xml";

    private static final String BUNDLE_PACKAGING_EDIT = "<packaging>bundle</packaging>";

    private static final String BUILD_TAG_OPEN = "<build>";
    private static final String BUILD_TAG_CLOSE = "</build>";
    private static final String PLUGINS_TAG_OPEN = "<plugins>";
    private static final String PLUGINS_TAG_CLOSE = "</plugins>";
    private static final String NEWLINE = "\n";

    private static final String TEMPLATE_BND_BND = "-classpath: target/classes";


    private final IFile pomFile;

    private TextEdit packagingEdit = null;
    private TextEdit buildPluginEdit;

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
        RefactoringStatus status = new RefactoringStatus();
        if (!pomFile.isSynchronized(1)) {
            status.addEntry(RefactoringStatus.FATAL, "The POM file is out of synchronisation with the file system.", null, Plugin.PLUGIN_ID, 0);
            return status;
        }
        return status;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        // Check POM
        SubMonitor progress = SubMonitor.convert(pm, 2);
        IDocument document = null;
        try {
            document = acquireDocument(progress.newChild(1, SubMonitor.SUPPRESS_NONE));

            LightweightDocumentBuilder docBuilder = new LightweightDocumentBuilder();
            LightweightDocument tree = docBuilder.build(new StringReader(document.get()));

            packagingEdit = generatePackagingEdit(tree);
            buildPluginEdit = generateBuildPluginEdit(tree);

        } catch (XMLStreamException e) {
            status.addEntry(RefactoringStatus.FATAL, "Processing error: " + e.getMessage(), new FileStatusContext(pomFile, null), Plugin.PLUGIN_ID, 0);
        } catch (IOException e) {
            status.addEntry(RefactoringStatus.FATAL, "Processing error: " + e.getMessage(), new FileStatusContext(pomFile, null), Plugin.PLUGIN_ID, 0);
        } finally {
            if (document != null) releaseDocument(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
        }

        return status;
    }

    private IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        manager.connect(pomFile.getFullPath(), LocationKind.IFILE, pm);
        ITextFileBuffer buffer = manager.getTextFileBuffer(pomFile.getFullPath(), LocationKind.IFILE);
        return buffer.getDocument();
    }

    private void releaseDocument(IProgressMonitor pm) throws CoreException {
        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        manager.disconnect(pomFile.getFullPath(), LocationKind.IFILE, pm);
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        SubMonitor progress = SubMonitor.convert(pm, 1);
        CompositeChange rootChange = new CompositeChange("Add OSGi/Bndtools support to Maven POM");

        // Create bnd.bnd
        IFile bndFile = pomFile.getProject().getFile(Project.BNDFILE);
        bndFile.refreshLocal(0, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
        if (!bndFile.exists())
            rootChange.add(new CreateTextFileChange(bndFile.getFullPath(), TEMPLATE_BND_BND, "UTF-8", "text"));

        // Edit POM
        TextFileChange pomChange = new TextFileChange("Add OSGi build settings to POM", pomFile);
        MultiTextEdit edits = new MultiTextEdit();
        if (packagingEdit != null)
            edits.addChild(packagingEdit);
        if (buildPluginEdit != null)
            edits.addChild(buildPluginEdit);
        pomChange.setEdit(edits);
        rootChange.add(pomChange);

        // Add project nature
        rootChange.add(new AddProjectNatureChange(pomFile.getProject(), BndProjectNature.NATURE_ID, "Bnd"));

        return rootChange;
    }

    TextEdit generatePackagingEdit(LightweightDocument document) throws XMLStreamException, CoreException {
        TextEdit result;
        XmlSearch elementSearch = new XmlSearch();
        LightweightElement packagingElem = elementSearch.searchElement(document, new String[] { "project", "packaging" });
        if (packagingElem == null) {
            LightweightElement versionElem = elementSearch.searchElement(document, new String[] { "project", "version" });
            if (versionElem == null)
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Invalid POM format", null));

            int insertPoint = versionElem.getLocation().getClose().getEnd().getCharacterOffset();
            String indentStr = getIndentFromTag(versionElem.getLocation().getOpen());
            String insertionText = "\n" + indentStr + BUNDLE_PACKAGING_EDIT;

            result = new InsertEdit(insertPoint, insertionText);
        } else {
            int start = packagingElem.getLocation().getOpen().getStart().getCharacterOffset();
            int end = packagingElem.getLocation().getClose().getEnd().getCharacterOffset();

            result = new ReplaceEdit(start, end - start, BUNDLE_PACKAGING_EDIT);
        }
        return result;
    }

    TextEdit generateBuildPluginEdit(LightweightDocument document) throws XMLStreamException, IOException {
        TextEdit result = null;

        XmlSearch elementSearch = new XmlSearch();
        LightweightElement buildElem = elementSearch.searchElement(document, new String[] { "project", "build" });
        if (buildElem == null) {
            result = generateEntireBuildElement(document);
        } else {
            LightweightElement pluginsElem = elementSearch.searchElement(buildElem, new String[] { "plugins" });
            if (pluginsElem == null) {
                result = generatePluginsElement(buildElem);
            }
        }

        return result;
    }

    TextEdit generateEntireBuildElement(LightweightDocument document) throws IOException {
        Location insertionPoint = document.getRootElement().getLocation().getClose().getStart();

        // Try to work out the indent based on the last non-root element (if any)
        String indent;
        List<LightweightElement> elements = document.getRootElement().getChildren();
        if (!elements.isEmpty()) {
            LightweightElement lastElement = elements.get(elements.size() - 1);
            int indentChars = lastElement.getLocation().getOpen().getStart().getColumnNumber() - 1;
            indent = TextUtils.generateIndent(indentChars);
        } else {
            indent = "\t";
        }

        // Generate the indented insertion
        StringBuffer buffer = new StringBuffer();
        buffer.append(indent).append(BUILD_TAG_OPEN).append(NEWLINE);
        buffer.append(indent).append(indent).append(PLUGINS_TAG_OPEN).append(NEWLINE);

        Reader reader = new InputStreamReader(getClass().getResourceAsStream("maven-bundle-plugin-fragment.txt"));
        TextUtils.indentText(indent + indent + indent, indent, reader, buffer);
        buffer.append(NEWLINE);

        buffer.append(indent).append(indent).append(PLUGINS_TAG_CLOSE).append(NEWLINE);
        buffer.append(indent).append(BUILD_TAG_CLOSE).append(NEWLINE);

        return new InsertEdit(insertionPoint.getCharacterOffset(), buffer.toString());
    }

    TextEdit generatePluginsElement(LightweightElement buildElem) throws IOException {
        Location insertionPoint = buildElem.getLocation().getClose().getStart();

        String indent;
        if (!buildElem.getChildren().isEmpty()) {
            // Work out the indent based on last child
            List<LightweightElement> children = buildElem.getChildren();
            LightweightElement lastChild = children.get(children.size() - 1);
            int spaces = lastChild.getLocation().getOpen().getStart().getColumnNumber() - 1;
            indent = TextUtils.generateIndent(spaces);
        } else {
            // Indent is 2X indent level of <build> element
            int spaces = (buildElem.getLocation().getOpen().getStart().getColumnNumber() - 1) * 2;
            indent = TextUtils.generateIndent(spaces);
        }

        // Generate the indented insertion
        StringBuffer buffer = new StringBuffer();
        buffer.append(indent).append(PLUGINS_TAG_OPEN).append(NEWLINE);

        Reader reader = new InputStreamReader(getClass().getResourceAsStream("maven-bundle-plugin-fragment.txt"));
        TextUtils.indentText(indent + indent + indent, indent, reader, buffer);
        buffer.append(NEWLINE);

        buffer.append(indent).append(PLUGINS_TAG_CLOSE).append(NEWLINE);

        return new InsertEdit(insertionPoint.getCharacterOffset(), buffer.toString());
    }

    String getIndentFromTag(TagLocation tag) {
        String indentStr;
        int spaces = tag.getStart().getColumnNumber() - 1;
        if (spaces > 0) {
            indentStr = TextUtils.generateIndent(spaces);
        } else if (spaces == 0) {
            indentStr = "";
        } else {
            indentStr = "\t";
        }
        return indentStr;
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
        return new RefactoringParticipant[0];
    }

}
