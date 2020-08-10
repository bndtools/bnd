package org.bndtools.core.resolve.ui;

import org.bndtools.core.resolve.ResolutionResult;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class ResolutionCanceledPanel {

	private final Image				clipboardImg	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/page_copy.png")
		.createImage();
	private final Image				treeViewImg		= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tree_mode.gif")
		.createImage();
	private final Image				flatViewImg		= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/flat_mode.gif")
		.createImage();

	private Composite				composite;

	private Text		processingLog;
	private Section		sectProcessingLog;
	private Section			sectProcessingLogFile;
	private Button			button;
	private ResolutionResult	resolutionResult;

	public ResolutionCanceledPanel() {
	}

	public void createControl(final Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		composite = toolkit.createComposite(parent);

		composite.setLayout(new GridLayout(1, false));
		GridData gd;

		sectProcessingLog = toolkit.createSection(composite,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		sectProcessingLog.setText("Processing Log:");

		processingLog = toolkit.createText(sectProcessingLog, "",
			SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		sectProcessingLog.setClient(processingLog);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 600;
		gd.heightHint = 300;
		sectProcessingLog.setLayoutData(gd);

		sectProcessingLogFile = toolkit.createSection(composite,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		sectProcessingLogFile.setText("Processing Logfile:");
		Composite composite = toolkit.createComposite(sectProcessingLogFile);
		sectProcessingLogFile.setClient(composite);

		button = new Button(composite, SWT.CHECK);
		button.setText("Keep Resolver log file after cancel");
		button.setToolTipText("This is still a temporary file and will be deleted when Eclipse closes.");

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resolutionResult.getLogger()
					.setKeepLogFileTillExit(button.getSelection());
			}
		});

		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 600;
		gd.heightHint = 100;
		sectProcessingLogFile.setLayoutData(gd);
	}

	public Control getControl() {
		return composite;
	}

	public void setInput(ResolutionResult resolutionResult) {
		if (composite == null)
			throw new IllegalStateException("Control not created");
		else if (composite.isDisposed())
			throw new IllegalStateException("Control already disposed");

		processingLog.setText(resolutionResult.getLog());
		this.resolutionResult = resolutionResult;
	}

	public void dispose() {
		clipboardImg.dispose();
		treeViewImg.dispose();
		flatViewImg.dispose();
	}
}
