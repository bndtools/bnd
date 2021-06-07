package bndtools.jareditor.internal;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Iterator;
import java.util.SortedMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.hex.Hex;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.io.LimitedInputStream;
import aQute.lib.strings.Strings;

public class JARTreeEntryPart extends AbstractFormPart implements IPartSelectionListener {
	private static final int	READ_LIMIT		= 1000000;

	private static final String	DEFAULT_CHARSET	= "UTF-8";

	enum Show {
		Auto,
		Text,
		Hex
	}

	private Text			text;
	private IResource		resource	= null;
	private Show			showAs		= Show.Auto;
	private final String[]	charsets;
	private int				selectedCharset;
	private boolean			limitRead	= true;
	private Text			size;
	private Text			lastModified;

	public JARTreeEntryPart(IEditorPart editor, Composite composite, FormToolkit toolkit) {

		SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
		charsets = new String[charsetMap.size()];
		int i = 0;
		for (Iterator<String> iter = charsetMap.keySet()
			.iterator(); iter.hasNext(); i++) {
			charsets[i] = iter.next();
		}
		setSelectedCharset(DEFAULT_CHARSET);
		createContent(composite, toolkit);
	}

	private void createContent(Composite parent, FormToolkit toolkit) {
		Section textSection = toolkit.createSection(parent,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		textSection.setText("Entry Content");
		Composite textComposite = toolkit.createComposite(textSection);
		text = toolkit.createText(textComposite, "", SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		text.setFont(JFaceResources.getTextFont());
		textSection.setClient(textComposite);

		Section encodingSection = toolkit.createSection(parent,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		encodingSection.setText("Display Options");
		Composite encodingPanel = toolkit.createComposite(encodingSection);
		encodingSection.setClient(encodingPanel);

		toolkit.createLabel(encodingPanel, "Size");
		size = new Text(encodingPanel, SWT.READ_ONLY);

		toolkit.createLabel(encodingPanel, "Last Modfied");
		lastModified = new Text(encodingPanel, SWT.READ_ONLY);

		toolkit.createLabel(encodingPanel, "Show As:");
		final Button btnAuto = toolkit.createButton(encodingPanel, "Auto", SWT.RADIO);
		final Button btnText = toolkit.createButton(encodingPanel, "Text", SWT.RADIO);
		final Button btnBinary = toolkit.createButton(encodingPanel, "Binary (hex)", SWT.RADIO);

		btnAuto.setSelection(showAs == Show.Auto);
		btnText.setSelection(showAs == Show.Text);
		btnBinary.setSelection(showAs == Show.Hex);

		toolkit.createLabel(encodingPanel, "Text Encoding:");
		final Combo encodingCombo = new Combo(encodingPanel, SWT.READ_ONLY);
		encodingCombo.setEnabled(showAs == Show.Text);

		// INITIALISE
		encodingCombo.setItems(charsets);
		encodingCombo.select(selectedCharset);

		final Button btnLimit = toolkit.createButton(encodingPanel, "Limit", SWT.CHECK);
		btnLimit.setSelection(limitRead);

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
				if (btnAuto.getSelection())
					showAs = Show.Auto;
				else if (btnText.getSelection())
					showAs = Show.Text;
				else if (btnBinary.getSelection())
					showAs = Show.Hex;
				else {
					assert false : "At least one of Auto, Text, or Hex, should have been selected";
				}

				limitRead = btnLimit.getSelection();

				encodingCombo.setEnabled(showAs == Show.Text);
				update();
			}
		};
		btnAuto.addSelectionListener(radioListener);
		btnText.addSelectionListener(radioListener);
		btnBinary.addSelectionListener(radioListener);
		btnLimit.addSelectionListener(radioListener);

		encodingCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedCharset = encodingCombo.getSelectionIndex();
				update();
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

		gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		size.setLayoutData(gd);
		lastModified.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		encodingSection.setLayoutData(gd);
		encodingSection.setLayout(new FillLayout());
		encodingPanel.setLayout(new GridLayout(4, true));
		encodingCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();

			this.resource = ((IResource) element);
		} else {
			this.resource = null;
		}
		update();
	}

	private void update() {
		size.setText("");
		lastModified.setText("");
		if (resource instanceof IFile) {
			IFile node = (IFile) resource;
			JAREditor.background("Loading " + resource.getName(), mon -> {
				try (InputStream in = limitRead ? new LimitedInputStream(node.getContents(), READ_LIMIT)
					: node.getContents()) {
					return IO.copy(in, new ByteBufferOutputStream())
						.toByteBuffer();
				}
			}, this::setContent);
		} else {
			setContent("");
		}
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

	private void setContent(String content) {
		if (text != null && !text.isDisposed())
			text.setText(content);
	}

	private void setContent(ByteBuffer data) throws CoreException {
		if (resource instanceof IFile) {
			URI locationURI = resource.getLocationURI();
			if (locationURI != null) {
				try {
					IFileStore store = EFS.getStore(locationURI);
					if (store != null) {
						IFileInfo fetchInfo = store.fetchInfo();
						if (fetchInfo != null) {
							long size = fetchInfo.getLength();
							if (size < 0) {
								this.size.setText("Unknown");
							} else {
								this.size.setText(Strings.toString(size, "b"));
							}
							long modified = fetchInfo.getLastModified();
							Instant instant = Instant.ofEpochMilli(modified);
							this.lastModified.setText(instant.toString());
						}
					}
				} catch (CoreException e) {
					// ignore
				}
			}
		}

		boolean limited = limitRead && (data.remaining() == READ_LIMIT);

		Show show = showAs;
		if (show == Show.Auto) {
			show = Hex.isBinary(data) ? Show.Hex : Show.Text;
		}

		String content;
		switch (show) {
			case Text :
				try {
					Charset charset = Charset.forName(charsets[selectedCharset]);
					content = IO.decode(data, charset)
						.toString();
				} catch (Exception e) {
					assert false : "All in memort ops";
					content = "";
				}
				break;
			case Auto :
			case Hex :
			default :
				content = Hex.format(data);
				break;
		}

		if (limited)
			content += "\n\nLimited to " + READ_LIMIT;

		setContent(content);
	}

}
