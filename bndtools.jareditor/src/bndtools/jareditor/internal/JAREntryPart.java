package bndtools.jareditor.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class JAREntryPart extends AbstractFormPart implements IPartSelectionListener {

    private static final String DEFAULT_CHARSET = "UTF-8";

    private final IEditorPart editor;

    private Text text;
    protected ZipEntry zipEntry = null;
    private Job displayJob = null;

    protected boolean showAsText = true;
    protected final String[] charsets;
    protected int selectedCharset;

    public JAREntryPart(IEditorPart editor, Composite composite, FormToolkit toolkit) {
        this.editor = editor;

        SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
        charsets = new String[charsetMap.size()];
        int i = 0;
        for (Iterator<String> iter = charsetMap.keySet().iterator(); iter.hasNext(); i++) {
            charsets[i] = iter.next();
        }
        setSelectedCharset(DEFAULT_CHARSET);

        createContent(composite, toolkit);
    }

    private void createContent(Composite parent, FormToolkit toolkit) {
        Section textSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        textSection.setText("Entry Content");
        Composite textComposite = toolkit.createComposite(textSection);
        text = toolkit.createText(textComposite, "", SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        text.setFont(JFaceResources.getTextFont());
        textSection.setClient(textComposite);

        Section encodingSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        encodingSection.setText("Display Options");
        Composite encodingPanel = toolkit.createComposite(encodingSection);
        encodingSection.setClient(encodingPanel);
        toolkit.createLabel(encodingPanel, "Show As:");
        final Button btnText = toolkit.createButton(encodingPanel, "Text", SWT.RADIO);
        btnText.setSelection(showAsText);
        Button btnBinary = toolkit.createButton(encodingPanel, "Binary (hex)", SWT.RADIO);
        btnBinary.setSelection(!showAsText);
        toolkit.createLabel(encodingPanel, "Text Encoding:");
        final Combo encodingCombo = new Combo(encodingPanel, SWT.READ_ONLY);
        encodingCombo.setEnabled(showAsText);

        // INITIALISE
        encodingCombo.setItems(charsets);
        encodingCombo.select(selectedCharset);

        // LISTENERS
        encodingSection.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                getManagedForm().reflow(true);
            }
        });
        SelectionListener radioListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showAsText = btnText.getSelection();
                encodingCombo.setEnabled(showAsText);
                loadContent();
            }
        };
        btnText.addSelectionListener(radioListener);
        btnBinary.addSelectionListener(radioListener);
        encodingCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedCharset = encodingCombo.getSelectionIndex();
                loadContent();
            }
        });

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, false);
        parent.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        textSection.setLayoutData(gd);

        layout = new GridLayout(1, false);
        textComposite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        text.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        encodingSection.setLayoutData(gd);
        encodingSection.setLayout(new FillLayout());
        encodingPanel.setLayout(new GridLayout(3, false));
        encodingCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
    }

    public void selectionChanged(IFormPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();

            ZipEntry entry = null;
            if (element instanceof ZipEntry)
                entry = (ZipEntry) element;
            else if (element instanceof ZipTreeNode)
                entry = ((ZipTreeNode) element).getZipEntry();

            this.zipEntry = entry;
        } else {
            this.zipEntry = null;
        }
        loadContent();
    }

    private final void setSelectedCharset(String selectedCharsetName) {
        for (int i = 0; i < charsets.length; i++) {
            if (charsets[i].equals(selectedCharsetName)) {
                selectedCharset = i;
                return;
            }
        }
        throw new IllegalArgumentException("Unknown charset name: " + selectedCharsetName);
    }

    protected void loadContent() {
        if (displayJob != null && displayJob.getState() != Job.NONE)
            displayJob.cancel();

        if (zipEntry != null && !zipEntry.isDirectory()) {
            IEditorInput input = editor.getEditorInput();
            final Display display = text.getDisplay();
            final URI uri;

            if (input instanceof IFileEditorInput) {
                uri = ((IFileEditorInput) input).getFile().getLocationURI();
            } else if (input instanceof IURIEditorInput) {
                uri = ((IURIEditorInput) input).getURI();
            } else {
                uri = null;
            }

            if (uri != null) {
                displayJob = new Job("Load zip content") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        File ioFile = new File(uri);
                        ZipFile zipFile = null;
                        try {
                            zipFile = new ZipFile(ioFile);
                            final StringWriter writer = new StringWriter();
                            if (showAsText)
                                readAsText(zipFile, zipEntry, charsets[selectedCharset], writer, 1024 * 20, monitor);
                            else
                                readAsHex(zipFile, zipEntry, writer, 1024 * 20, 2, monitor);

                            display.asyncExec(new Runnable() {
                                public void run() {
                                    setContent(writer.toString());
                                }
                            });

                            return Status.OK_STATUS;
                        } catch (IOException e) {
                            Status status = new Status(IStatus.ERROR, PluginConstants.PLUGIN_ID, 0, "I/O error reading JAR file contents", e);
                            // ErrorDialog.openError(getManagedForm().getForm().getShell(), "Error", null, status);
                            return status;
                        } finally {
                            try {
                                if (zipFile != null)
                                    zipFile.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                };
                displayJob.schedule();
            }
        } else {
            setContent("");
        }
    }

    protected void setContent(String content) {
        if (text != null && !text.isDisposed())
            text.setText(content);
    }

    private static final String pseudo[] = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"
    };

    private static SubMonitor createProgressMonitor(ZipEntry entry, long limit, IProgressMonitor monitor) {
        SubMonitor progress;
        long size = entry.getSize();
        if (size == -1) {
            progress = SubMonitor.convert(monitor);
        } else {
            long ticks = (limit == -1) ? size : Math.min(size, limit);
            progress = SubMonitor.convert(monitor, (int) ticks);
        }
        return progress;
    }

    protected static void readAsText(ZipFile zipFile, ZipEntry entry, String encoding, Writer out, long limit, IProgressMonitor monitor) throws IOException {
        SubMonitor progress = createProgressMonitor(entry, limit, monitor);

        boolean limitReached = false;
        InputStream stream = zipFile.getInputStream(entry);
        try {
            long total = 0;

            byte[] buffer = new byte[1024];
            while (true) {
                if (progress.isCanceled())
                    return;
                int bytesRead = stream.read(buffer, 0, 1024);
                if (bytesRead < 0)
                    break;
                String string = new String(buffer, 0, bytesRead, encoding);
                out.write(string);

                total += bytesRead;
                progress.worked(bytesRead);
                if (limit >= 0 && total >= limit) {
                    limitReached = true;
                    return;
                }
            }
        } finally {
            if (limitReached) {
                out.write("\nLimit of " + (limit >> 10) + "Kb reached, the rest of the entry is not shown.");
            }

            stream.close();
        }
    }

    private static char byteToChar(byte b) {
        if ((b < 32) || (b == 127)) {
            return '.';
        }

        return (char) b;
    }

    protected static void readAsHex(ZipFile zipFile, ZipEntry entry, Writer out, long limit, int groupsOf8BytesPerLine, IProgressMonitor monitor) throws IOException {
        SubMonitor progress = createProgressMonitor(entry, limit, monitor);

        boolean limitReached = false;
        long offsetInFile = 0;
        int bytesPerLine = groupsOf8BytesPerLine * 8;
        int asciiPosition = 0;
        char[] asciiBuffer = new char[bytesPerLine + (2 * (groupsOf8BytesPerLine - 1))];
        int bytePosition = 0;
        byte[] buffer = new byte[1024];

        InputStream stream = zipFile.getInputStream(entry);
        try {
            long total = 0;

            while (true) {
                if (progress.isCanceled())
                    return;
                int bytesRead = stream.read(buffer, 0, 1024);
                if (bytesRead < 0)
                    break;

                for (int i = 0; i < bytesRead; i++) {
                    if (bytePosition == 0) {
                        String s = String.format("0x%04x ", offsetInFile);
                        out.write(s);
                        offsetInFile += bytesPerLine;
                    }

                    asciiBuffer[asciiPosition] = byteToChar(buffer[i]);
                    asciiPosition++;

                    out.write(pseudo[(buffer[i] & 0xf0) >>> 4]); // Convert to a string character
                    out.write(pseudo[(buffer[i] & 0x0f)]); // convert the nibble to a String Character
                    out.write(' ');
                    bytePosition++;

                    /* do a linebreak after the required number of bytes */
                    if (bytePosition >= bytesPerLine) {
                        out.write(' ');
                        out.write(asciiBuffer);
                        out.write('\n');
                        asciiPosition = 0;
                        bytePosition = 0;
                    }

                    /* put 2 extra spaces between bytes */
                    if ((bytePosition > 0) && (bytePosition % 8 == 0)) {
                        asciiBuffer[asciiPosition++] = ' ';
                        asciiBuffer[asciiPosition++] = ' ';
                        out.write(' ');
                    }
                }

                total += bytesRead;
                progress.worked(bytesRead);
                if (limit >= 0 && total >= limit) {
                    limitReached = true;
                    return;
                }
            }
        } finally {
            if (bytePosition > 0) {
                while (bytePosition < bytesPerLine) {
                    out.write("   ");
                    bytePosition++;

                    /* put 2 extra spaces between bytes */
                    if ((bytePosition > 0) && (bytePosition % 8 == 0)) {
                        out.write(' ');
                    }
                }
                out.write(asciiBuffer, 0, asciiPosition);
            }

            if (limitReached) {
                out.write("\nLimit of " + (limit >> 10) + "Kb reached, the rest of the entry is not shown.");
            }

            stream.close();
        }
    }
}
