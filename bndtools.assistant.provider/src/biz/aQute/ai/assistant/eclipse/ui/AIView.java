package biz.aQute.ai.assistant.eclipse.ui;

import static biz.aQute.ai.assistant.eclipse.ui.Util.formatMilliseconds;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;
import biz.aQute.ai.assistant.provider.BndAssistant;
import biz.aQute.ai.assistant.provider.BndAssistant.AssistantView;

public class AIView extends ViewPart implements AssistantView {
	final static IWorkspace			workspace			= ResourcesPlugin.getWorkspace();
	final Updater					updater				= new Updater();
	final Util						util				= new Util(Display.getDefault());
	final List<LogEntry>			log					= new ArrayList<>();

	record LogEntry(long time, String message) {
		public LogEntry(String message) {
			this(System.currentTimeMillis(), message);
		}
	}

	@SuppressWarnings("rawtypes")
	ServiceRegistration	registerService;
	Browser				browser;
	Text				inputText;
	BndAssistant		assistant;
	private Text		statusMessage;

	class Updater implements ProgressListener, StatusTextListener {
		final List<String>	divs		= new CopyOnWriteArrayList<>();
		boolean				cando		= false;
		String				status;

		void update(String html) {
			divs.add(html);
			update();
		}

		void update() {
			util.onDisplay(() -> {
				if (cando) {
					while (!divs.isEmpty()) {
						String html = divs.remove(0);
						String escaped = escapeForJavaScript(html);
						Object s = browser.evaluate("append(" + escaped + ")");
						System.out.println(html);
					}
					browser.execute("window.scrollTo(0, document.body.scrollHeight)");
				}
			});
		}

		@Override
		public void changed(StatusTextEvent event) {
			status = event.text;
		}

		@Override
		public void changed(ProgressEvent event) {
			cando = false;
		}

		@Override
		public void completed(ProgressEvent event) {
			cando = true;
			update();
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		final IToolBarManager toolbarManager = getViewSite().getActionBars()
			.getToolBarManager();

		parent.setEnabled(false);
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);

		Combo comboMenu = new Combo(parent, 0);
		comboMenu.setItems(new String[] {
			"gpt-3.5-turbo", "gpt-4"
		});
		comboMenu.select(0);
		comboMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selection = comboMenu.getItem(comboMenu.getSelectionIndex());
			}
		});

		browser = new Browser(parent, SWT.NONE);
		GridData browserData = new GridData(SWT.FILL, SWT.FILL, true, true);
		browserData.horizontalSpan = 2;
		browser.setLayoutData(browserData);
		browser.setJavascriptEnabled(true);

		Combo instructionsCombo = new Combo(parent, SWT.READ_ONLY);
		instructionsCombo.setItems(new String[] {
			"Instruction 1", "Instruction 2", "Instruction 3"
		});
		instructionsCombo.select(0);
		instructionsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selection = instructionsCombo.getItem(instructionsCombo.getSelectionIndex());
			}
		});

		Combo topicCombo = new Combo(parent, SWT.READ_ONLY);
		topicCombo.setItems(new String[] {
			"Topic 1", "Topic 2", "Topic 3"
		});
		topicCombo.select(0);
		topicCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selection = topicCombo.getItem(topicCombo.getSelectionIndex());
			}
		});

		Font editorFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);

		// Text box for providing question/command to AI agent
		inputText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		inputText.setFont(editorFont); // Set the font to the Text widget
		GridData inputTextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		inputTextData.heightHint = inputText.getLineHeight() * 6;
		inputText.setLayoutData(inputTextData);
		inputText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					if ((e.stateMask & SWT.SHIFT) == 0) {
						e.doit = false;
						send();
					}
				}
			}

		});

		{
			Composite buttons = new Composite(parent, SWT.NONE);
			buttons.setLayout(new GridLayout(1, false));
			buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			{
				Button sendButton = new Button(buttons, SWT.PUSH);
				sendButton.setText("Send");
				sendButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				sendButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String input = inputText.getText();
						inputText.setText("");
						updater.update(input);
						send();
					}
				});
			}
		}
		statusMessage = new Text(parent, SWT.BORDER);
		GridData statusMessageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusMessageData.horizontalSpan = 2;
		statusMessage.setLayoutData(statusMessageData);
		statusMessage.setFont(editorFont);
		statusMessage.setBackground(null);

		parent.pack();
		browser.addProgressListener(updater);
		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				if (event.location.toLowerCase()
					.startsWith("http")) {
					Program.launch(event.location);
					event.doit = false;
				}
			}

			@Override
			public void changed(LocationEvent event) {}
		});

		toolbarManager.add(new Action("Show log") {
			@Override
			public void run() {
				sendLog();
			}
		});
		toolbarManager.add(new Action("Clear log") {
			@Override
			public void run() {
				log.clear();
			}
		});
		toolbarManager.add(new Action("Clear Browser") {
			@Override
			public void run() {
				clear();
			}
		});
		toolbarManager.add(new Action("Show DOM") {
			@Override
			public void run() {
				browser.evaluate("displayDOM()\n");
			}
		});
		clear();

		BundleContext context = FrameworkUtil.getBundle(getClass())
			.getBundleContext();
		registerService = context.registerService(AssistantView.class, this, null);

	}

	public void clear() {
		Job load = Job.create("load html prolog", mon -> {
			try {
				URL url = getClass().getResource("/html/prolog.html");
				String content = IO.collect(url);
				Display.getDefault()
					.asyncExec(() -> {
						browser.setText(content);
					});
				updater.update("""
					<h1>Assistant for Bndtools</h1>
					""");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		load.schedule();
	}

	private void send() {
		String input = inputText.getText();
		inputText.setText("");
		if (!input.isBlank())
			try {
				util.job("send", () -> {
					return assistant.send(input);
				}, s -> {
					statusMessage.setText(s);
				});
			} catch (Exception e) {
				status("failure " + Exceptions.toString(e));
			} finally {
				try {
					workspace.getRoot()
						.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
				status("done");
			}

	}

	public static String escapeForJavaScript(String input) {
		StringBuilder result = new StringBuilder();
		result.append("\"");
		for (char ch : input.toCharArray()) {
			switch (ch) {
				case '\'' :
				case '\"' :
				case '\\' :
					result.append('\\')
						.append(ch); // Add escape character before special
										// character
					break;
				case '\n' :
					result.append("\\n"); // Newline escape sequence
					break;
				case '\r' :
					result.append("\\r"); // Carriage return escape sequence
					break;
				case '\t' :
					result.append("\\t"); // Tab escape sequence
					break;
				case '\b' :
					result.append("\\b"); // Backspace escape sequence
					break;
				case '\f' :
					result.append("\\f"); // Form feed escape sequence
					break;
				default :
					result.append(ch); // Append character as is
					break;
			}
		}
		result.append("\"");
		return result.toString();
	}

	@Override
	public void setFocus() {
		inputText.setFocus();
	}

	@Override
	public void dispose() {
		if (registerService != null)
			registerService.unregister();
		super.dispose();
	}

	@Override
	public void bind(BndAssistant domain) {
		this.assistant = domain;
		Display.getDefault()
			.asyncExec(() -> {
				browser.getParent()
					.setEnabled(domain != null);
			});
	}

	@Override
	public void received(String html) {
		updater.update(html);
	}

	@Override
	public void status(String statusMessage) {
		log.add(new LogEntry(statusMessage));
		util.onDisplay(() -> {
			this.statusMessage.setText(statusMessage);
		});
	}

	void sendLog() {
		try (Formatter sb = new Formatter()) {
			sb.format("""
				<table class='log'>
					<thead>
						<th>Delta</th>
						<th>Message</th>
					</thead>
					<tbody>
				""");
			long start = 0;
			for (LogEntry e : log) {
				long delta;
				if (start == 0) {
					start = e.time;
					delta = 0;
				} else {
					delta = e.time - start;
				}

				sb.format("""
					<tr>
					  <td>%s</td>
					  <td>%s</td>
					</tr>
					""", formatMilliseconds(delta), e.message);
			}
			sb.format("""
				   </tbody>
				</table>
				""");
			updater.update(sb.toString());
		}
	}

	@Override
	public void refresh(File file) {
		File dir = file.getParentFile();
		if (!dir.isDirectory())
			return;

		Job job = Job.create("refresh " + file, mon -> {
			IContainer container = workspace.getRoot()
				.getContainerForLocation(Path.fromOSString(dir.getAbsolutePath()));
			if (container != null) {
				try {
					container.refreshLocal(IResource.DEPTH_ONE, mon);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		});
		job.schedule(100);
	}

}
