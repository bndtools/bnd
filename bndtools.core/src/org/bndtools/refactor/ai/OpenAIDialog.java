package org.bndtools.refactor.ai;

import static aQute.libg.re.Catalog.dotall;
import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.multiline;
import static aQute.libg.re.Catalog.setAll;
import static aQute.libg.re.Catalog.startOfLine;

import java.util.function.Consumer;

import org.bndtools.refactor.ai.api.Chat;
import org.bndtools.refactor.ai.api.OpenAI;
import org.bndtools.refactor.ai.api.Reply;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import aQute.libg.re.RE;

public class OpenAIDialog extends Dialog {
	final static RE			CODE_BEGIN_P	= g(startOfLine, lit("```java\n"));
	final static RE			CODE_END_P		= g(startOfLine, lit("```"));
	final static RE			CODE_P			= g(multiline(), dotall(), CODE_BEGIN_P, g("code", setAll),
		CODE_END_P);

	final OpenAI			openai;
	String					sourceCode;
	String					modifiedCode;
	Text					promptText;
	Button					submitButton;
	int						version	= 1000;
	Job						active;

	private TextMergeViewer	viewer;
	final Consumer<String>	update;
	private Combo modelCombo;

	public OpenAIDialog(Shell parentShell, String sourceCode, OpenAI openai, Consumer<String> update) {
		super(parentShell);
		this.openai = openai;
		this.sourceCode = sourceCode;
		this.update = update;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout overalllayout = new GridLayout(1, false);
		container.setLayout(overalllayout);

		CompareViewerPane viewerPane = new CompareViewerPane(container, SWT.BORDER);
		GridData viewerPaneGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		viewerPaneGridData.heightHint = 600;
		viewerPaneGridData.widthHint = 1200;
		viewerPane.setLayoutData(viewerPaneGridData);

		CompareConfiguration configuration = new CompareConfiguration();
		configuration.setLeftLabel("Original");
		configuration.setRightLabel("Generated");
		configuration.setRightEditable(true);

		viewer = new TextMergeViewer(viewerPane, SWT.NONE, configuration);
		viewer.setInput(new DiffNode(new JavaNode("Left", sourceCode), new JavaNode("right", "")));
		Control control = viewer.getControl();
		viewerPane.setContent(control);
		viewer.setSelection(() -> true);

		GridLayout controlLayout = new GridLayout(8, true);
		Composite controlArea = WidgetFactory.composite(SWT.NONE)
			.layout(controlLayout)
			.layoutData(new GridData(GridData.FILL_BOTH))
			.create(container);

		modelCombo = new Combo(controlArea, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData modelComboGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		modelComboGridData.horizontalSpan = 7;
		modelCombo.setLayoutData(modelComboGridData);
		modelCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectedIndex = modelCombo.getSelectionIndex();
				String selectedModel = modelCombo.getItem(selectedIndex);
				// Handle the selection of a model here
			}
		});
		for (String model : openai.getModels()) {
			modelCombo.add(model);
		}
		modelCombo.select(0);

		Label comboLabel = new Label(controlArea, SWT.NONE);
		comboLabel.setText("Select Model");
		GridData comboLabelGridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		comboLabel.setLayoutData(comboLabelGridData);

		promptText = new Text(controlArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData promptTextGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		promptTextGridData.horizontalSpan = 7;
		promptTextGridData.heightHint = 100; // Set height for multiple lines
		promptText.setLayoutData(promptTextGridData);
		promptText.setFont(JFaceResources.getTextFont());

		submitButton = new Button(controlArea, SWT.PUSH);
		submitButton.setText("Send");
		submitButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				processPrompt();
			}
		});
		GridData submitButtonData = new GridData(SWT.FILL, SWT.TOP, true, false);
		submitButtonData.horizontalSpan = 1;
		submitButton.setLayoutData(submitButtonData);

		getShell().setDefaultButton(null);
		promptText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.stateMask == SWT.NONE) && (e.keyCode == SWT.CR)) {
					processPrompt();
				}
			}
		});

		refreshCompareEditor(sourceCode, "");
		return container;
	}

	private void refreshCompareEditor(String original, String modified) {
		viewer.setInput(new DiffNode(new JavaNode("Source", original), new JavaNode("Generated", modified)));
		viewer.setSelection(() -> true);
	}

	private void processPrompt() {
		processPrompt(promptText.getText());
	}

	private void processPrompt(String prompt) {
		int current = ++version;
		if (active != null) {
			active.cancel();
			active = null;
		}

		OpenAI.Configuration config = new OpenAI.Configuration();
		config.model = modelCombo.getText();
		Chat chat = openai.createChat(config);
		chat.system("""
			you're a very smart assistant in a bnd Bndtools Eclipse workspace
			with the task to help the user with her source code.
			The user selects a piece of code, could be a whole class, and then
			asks a transformation of the whole. The output you generate must
			contain the Java code in a markdown code format using triple
			back quotes like:
			```java
			class Foo {
			}
			```
			The Java code must be an exact replacement for the source code. For example,
			if the user asks for a method to write, add
			it to the class and output the WHOLE class, not partial, since it
			will replace the whole class in the editor.
			The generated code will directly replace the given source code in the editor
			unless the user explicitly asks for a new construct!
			After the ### is the source code.
			###
			""" + sourceCode);

		refreshCompareEditor(sourceCode, current + ":generating answer ...");
		active = Job.create("openai", mon -> {
			chat.clear("user");
			Reply ask = chat.ask(prompt);
			getShell().getDisplay()
				.asyncExec(() -> {
					StringBuilder comments = new StringBuilder("\n");

					if (current == this.version) {
						modifiedCode = CODE_P.findIn(ask.reply)
							.flatMap(m -> {
								comments.append(ask.reply.substring(0, m.start()));
								comments.append("\n");
								comments.append(ask.reply.substring(m.end()));
								return m.group("code");
							})
							.map(group -> {
								return group.toString();
							})
							.orElse(ask.reply);
						promptText.append(comments.toString());
						this.refreshCompareEditor(sourceCode, modifiedCode);
					}
				});
		});
		active.schedule();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Replace", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}

	@Override
	protected void okPressed() {
		update.accept(modifiedCode);
		super.okPressed();
	}
}
