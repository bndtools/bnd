/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.jareditor.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class JARContentDetailsPage extends AbstractFormPart implements IDetailsPage {
	
	private static final String DEFAULT_CHARSET = "UTF-8";

	private final String[] charsets;
	private int selectedCharset;
	private boolean showAsText = true;
	private ZipEntry zipEntry = null;
	
    private Composite parentComposite;
    private Font textFont;

	private Text text;
	private Object formInput;

	public JARContentDetailsPage() {
		SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
		charsets = new String[charsetMap.size()];
		int i=0; for (Iterator<String> iter = charsetMap.keySet().iterator(); iter.hasNext(); i++) {
			charsets[i] = iter.next();
		}
		setSelectedCharset(DEFAULT_CHARSET);
	}
	
	public void setShowAsText(boolean showAsText) {
		this.showAsText = showAsText;
	}
	
	public final void setSelectedCharset(String selectedCharsetName) {
		for(int i=0; i<charsets.length; i++) {
			if(charsets[i].equals(selectedCharsetName)) {
				selectedCharset = i;
				return;
			}
		}
		throw new IllegalArgumentException("Unknown charset name: " + selectedCharsetName);
	}

	public void createContents(Composite parent) {
        this.parentComposite = parent;

        FontDescriptor fd = JFaceResources.getTextFontDescriptor();
        fd = fd.setHeight(9);
        this.textFont = fd.createFont(parent.getDisplay());

        FormToolkit toolkit = getManagedForm().getToolkit();

        // Create controls
        Section encodingSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
        encodingSection.setText("Display Options");
        encodingSection.setExpanded(false);
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

        Section textSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.CLIENT_INDENT);
        textSection.setText("Entry Content");
        text = toolkit.createText(textSection, "", SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
        text.setFont(textFont);
        textSection.setClient(text);

		// Populate with data
		encodingCombo.setItems(charsets);
		encodingCombo.select(selectedCharset);
		
		// Listeners
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
	
		// Layout
        TableWrapLayout tl;
        TableWrapData td;

        tl = new TableWrapLayout();
        parent.setLayout(tl);

        td = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB);
        td.grabHorizontal = true;
        td.grabVertical = true;
        textSection.setLayoutData(td);

        td = new TableWrapData(TableWrapData.FILL, TableWrapData.FILL);
        td.grabHorizontal = true;
        encodingSection.setLayoutData(td);

        // tl = new TableWrapLayout();
        // tl.bottomMargin = 0;
        // tl.topMargin = 0;
        // tl.leftMargin = 0;
        // tl.rightMargin = 0;
        // textSection.setLayout(tl);
        //
        // td = new TableWrapData(TableWrapData.FILL_GRAB,
        // TableWrapData.FILL_GRAB);
        // td.maxWidth = 100;
        // td.maxHeight = 100;
        // td.heightHint = 100;
        // text.setLayoutData(td);
        // text.setSize(100, 100);

        encodingSection.setLayout(new FillLayout());
        encodingPanel.setLayout(new GridLayout(3, false));
        encodingCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if(selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			
			ZipEntry entry = null;
			if(element instanceof ZipEntry)
				entry = (ZipEntry) element;
			else if(element instanceof ZipTreeNode)
				entry = ((ZipTreeNode) element).getZipEntry();
			
			this.zipEntry = entry;
		} else {
			this.zipEntry = null;
		}
		loadContent();
	}
	
	private void loadContent() {
		String content = "";
		if(zipEntry != null && !zipEntry.isDirectory()) {
			IFile file = ((IFileEditorInput) formInput).getFile();
			File ioFile = new File(file.getLocationURI());
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(ioFile);
				
				InputStream stream = zipFile.getInputStream(zipEntry);
				StringWriter writer = new StringWriter();
				
				if(showAsText)
					readAsText(stream, charsets[selectedCharset], writer);
				else
					readAsHex(stream, writer);
				
				content = writer.toString();
			} catch (IOException e) {
				Status status = new Status(IStatus.ERROR, Constants.PLUGIN_ID, 0, "I/O error reading JAR file contents", e);
				ErrorDialog.openError(getManagedForm().getForm().getShell(), "Error", null, status);
			} finally {
				try {
					if(zipFile != null) zipFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
        text.setText(content);
        text.setFont(textFont);
        parentComposite.layout(true, true);
	}
	
	private static final String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
	
	private static void readAsText(InputStream in, String encoding, Writer out) throws IOException {
		InputStreamReader reader = new InputStreamReader(in, encoding);
		
		char[] buffer = new char[1024];
		while(true) {
			int read = reader.read(buffer, 0, 1024);
			if(read < 0) break;
			out.write(buffer, 0, read);
		}
	}
	
	private static void readAsHex(InputStream in, Writer out) throws IOException {
		byte[] buffer = new byte[1024];
		int charsWritten = 0;
		while(true) {
			int bytesRead = in.read(buffer, 0, 1024);
			if(bytesRead < 0) break;
			
			byte ch;
			for(int i=0; i<bytesRead; i++) {
				ch = (byte) (buffer[i] & 0xf0); // strip off high nibble
				ch = (byte) (ch >>> 4); // shift bits down
				ch = (byte) (ch & 0x0f);
				out.write(pseudo[(int) ch]); // Convert to a string character
				ch = (byte) (buffer[i] & 0x0F); // Strip off low nibble
				out.write(pseudo[(int) ch]); // convert the nibble to a String Character
				out.write(' ');
				charsWritten += 3;
				if(charsWritten % 75 == 0) out.write('\n');
			}
		}
	}
	
	@Override
	public boolean setFormInput(Object input) {
		this.formInput = input;
		return false;
	}
	
    @Override
    public void dispose() {
        super.dispose();
        if (textFont != null && !textFont.isDisposed())
            textFont.dispose();
    }
}
