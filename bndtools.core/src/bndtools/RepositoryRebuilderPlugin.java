package bndtools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;
import bndtools.bindex.FileIndexer;

public class RepositoryRebuilderPlugin implements RepositoryListenerPlugin {

    private static final String CDATA = "CDATA";

    public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
        IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
        IFile repoFile = cnfProject.getFile("repository.xml");
        if (!repoFile.exists()) {
            RepositoryIndexerJob job = new RepositoryIndexerJob("Indexing local repository...");
            job.schedule();
        } else {
            try {
                // Open the original index
                InputStream source1 = repoFile.getContents();

                // Generate a temporary index for the newly added file
                File tmpIndexFile = File.createTempFile("repo", ".xml");
                FileIndexer indexer = new FileIndexer(new File[] { file }, "__local_repo__");
                indexer.setOutputFile(tmpIndexFile);
                indexer.initialise(null);

                // Merge into a new index
                FileInputStream source2 = new FileInputStream(tmpIndexFile);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                mergeRepos("Local Repo", buffer, source1, source2);
                repoFile.setContents(new ByteArrayInputStream(buffer.toByteArray()), false, true, null);
            } catch (Exception e) {
                Plugin.logError("Error updating repository index", e);
            }
        }
    }


    public static void mergeRepos(String repoName, OutputStream out, InputStream... ins) throws TransformerConfigurationException, ParserConfigurationException, SAXException, IOException {
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler endHandler = transformerFactory.newTransformerHandler();
        endHandler.setResult(new StreamResult(out));

        openDocument(repoName, endHandler);

        MergeIndexesSAXHandler mergeHandler = new MergeIndexesSAXHandler(endHandler);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        for (InputStream in : ins) {
            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(in, mergeHandler);
        }

        closeDocument(endHandler);
    }

    static void openDocument(String repoName, ContentHandler handler) throws SAXException {
        handler.startDocument();
        AttributesImpl repositoryAttrs = new AttributesImpl();
        repositoryAttrs.addAttribute(null, "lastmodified", "lastmodified", CDATA, Long.toString(System.currentTimeMillis()));
        repositoryAttrs.addAttribute(null, "name", "name", CDATA, repoName);
        handler.startElement(null, "repository", "repository", repositoryAttrs);
    }

    static void closeDocument(ContentHandler handler) throws SAXException {
        handler.endElement(null, "repository", "repository");
        handler.endDocument();
    }

}
