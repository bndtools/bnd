package bndtools.jareditor.internal;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import aQute.bnd.osgi.Jar;
import aQute.bnd.print.JarPrinter;

public class JARPrintPage extends FormPage {

	public static final Image	IMAGE_CLOSE				= Plugin.getInstance()
		.getImageRegistry()
		.get(Plugin.IMG_CLOSE);
	public static final Image	IMAGE_NEXT				= Plugin.getInstance()
		.getImageRegistry()
		.get(Plugin.IMG_NEXT);
	public static final Image	IMAGE_NEXT_DISABLED		= Plugin.getInstance()
		.getImageRegistry()
		.get(Plugin.IMG_NEXT_DISABLED);
	public static final Image	IMAGE_PREVIOUS			= Plugin.getInstance()
		.getImageRegistry()
		.get(Plugin.IMG_PREVIOUS);
	public static final Image	IMAGE_PREVIOUS_DISABLED	= Plugin.getInstance()
		.getImageRegistry()
		.get(Plugin.IMG_PREVIOUS_DISABLED);

	private StyledText	styledText;
	private URI			uri;
	private boolean		loading;
	private int					highlightIndex			= -1;
	private IAction				findAction;
	private IAction				nextAction;
	private IAction				previousAction;

	public JARPrintPage(FormEditor formEditor, String id, String title) {
		super(formEditor, id, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm scrolledForm = managedForm.getForm();
		Form form = scrolledForm.getForm();
		Composite header = form.getHead();
		header.setLayout(GridLayoutFactory.createFrom(new GridLayout(1, true))
			.margins(0, 0)
			.create());

		Composite findParent = toolkit.createComposite(header, SWT.BORDER);
		findParent.setLayout(GridLayoutFactory.createFrom(new GridLayout(6, false))
			.margins(5, 5)
			.create());
		findParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		findParent.setBackgroundMode(SWT.INHERIT_FORCE);

		addChangeListener(event -> {
			if (event instanceof HighlightEvent)
				findParent.layout(true);
		});

		toolkit.setBorderStyle(SWT.NULL);
		Text findText = toolkit.createText(findParent, "", SWT.SINGLE);
		findText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		findText.setMessage("Find...");
		findText.addModifyListener(e -> fireChangeEvent(new FindTextEvent(findText.getText())));
		findText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == 13) { // enter key
					int numHighlights = styledText.getStyleRanges().length;

					if (highlightIndex == numHighlights - 1) {
						highlightIndex = -1; // loop back from beginning
					}

					nextAction.run();
				}
			}
		});

		addChangeListener(event -> {
			if (event instanceof FindTextEvent)
				highlightText((String) event.data);
		});

		Label highlightStatus = toolkit.createLabel(findParent, "");
		highlightStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		addChangeListener(e -> {
			if (e instanceof HighlightEvent) {
				HighlightEvent event = (HighlightEvent) e;

				String text = event.findText;
				StyleRange[] highlights = event.highlights;
				int index = event.index;

				if (highlights.length > 0) {
					highlightStatus.setText(MessageFormat.format("{0}/{1}", index + 1, highlights.length));
				} else {
					if (text.isEmpty()) {
						highlightStatus.setText("");
					} else {
						highlightStatus.setText("0/0");
					}
				}
			}
		});

		Label separator = toolkit.createSeparator(findParent, SWT.SEPARATOR);
		GridData sepData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		sepData.heightHint = findText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		separator.setLayoutData(sepData);

		Label nextButton = toolkit.createLabel(findParent, "", SWT.BORDER);
		nextButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		nextButton.setImage(IMAGE_NEXT_DISABLED);
		nextButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				nextAction.run();
			}
		});

		addChangeListener(e -> {
			if (e instanceof HighlightEvent) {
				HighlightEvent event = (HighlightEvent) e;

				Image image;

				if (event.highlights.length > 0 && event.index < event.highlights.length - 1) {
					image = IMAGE_NEXT;
				} else {
					image = IMAGE_NEXT_DISABLED;
				}

				nextButton.setImage(image);
			}
		});

		Label previousButton = toolkit.createLabel(findParent, "", SWT.BORDER);
		previousButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		previousButton.setImage(IMAGE_PREVIOUS_DISABLED);
		previousButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				previousAction.run();
			}
		});

		addChangeListener(e -> {
			if (e instanceof HighlightEvent) {
				HighlightEvent event = (HighlightEvent) e;

				Image image;

				if (event.highlights.length > 0 && event.index - 1 >= 0) {
					image = IMAGE_PREVIOUS;
				} else {
					image = IMAGE_PREVIOUS_DISABLED;
				}

				previousButton.setImage(image);
			}
		});

		Label closeButton = toolkit.createLabel(findParent, "", SWT.BORDER);
		closeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		closeButton.setImage(IMAGE_CLOSE);
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				form.setHeadClient(null);
				// save off the old findText to use as default for next time
				findText.setData(findText.getText());
				findText.setText("");
				styledText.setFocus();
				form.layout();
			}
		});

		Composite body = form.getBody();
		StackLayout stackLayout = new StackLayout() {
			@Override
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				Point size = super.computeSize(composite, wHint, hHint, flushCache);
				// hack to make sure styledText never grows outside of body
				size.y = SWT.DEFAULT;
				return size;
			}
		};
		stackLayout.marginHeight = stackLayout.marginWidth = 5;
		body.setLayout(stackLayout);

		styledText = new StyledText(body, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		stackLayout.topControl = styledText;
		styledText.setEditable(false);
		styledText.setFont(JFaceResources.getTextFont());
		styledText.addModifyListener(e -> {
			String find = findText.getText();
			if (find.length() > 0)
				highlightText(find);
		});

		findAction = new Action() {
			@Override
			public void run() {
				form.setHeadClient(findParent);
				if (findText.getData() instanceof String)
					findText.setText((String) findText.getData());
				findText.setFocus();
			}
		};

		nextAction = new Action() {
			@Override
			public void run() {
				StyleRange[] highlights = styledText.getStyleRanges();

				if (highlights.length <= 1 || highlightIndex == highlights.length - 1)
					return;

				nextHighlight(findText.getText(), highlights);
			}
		};

		previousAction = new Action() {
			@Override
			public void run() {
				StyleRange[] highlights = styledText.getStyleRanges();

				if (highlights.length <= 1 || highlightIndex == 0)
					return;

				previousHighlight(findText.getText(), highlights);
			}
		};
	}

	private void nextHighlight(String findText, StyleRange[] highlights) {
		int next = highlightIndex == highlights.length - 1 ? highlightIndex : highlightIndex + 1;
		revealRange(highlights, next);
		fireChangeEvent(new HighlightEvent(findText, highlights, next));
	}

	private void previousHighlight(String findText, StyleRange[] highlights) {
		int previous = highlightIndex == 0 ? 0 : highlightIndex - 1;
		revealRange(highlights, previous);
		fireChangeEvent(new HighlightEvent(findText, highlights, previous));
	}

	private void highlightText(String findText) {
		String currentText = styledText.getText();
		StyleRange[] currentHighlights = styledText.getStyleRanges();

		List<Integer> hits = findOccurances(currentText, findText);

		Color highlightColor = styledText.getSelectionBackground();

		StyleRange[] highlights = hits
			.stream()
			.map(hit -> new StyleRange(hit, findText.length(), null,
				highlightColor))
			.toArray(StyleRange[]::new);

		styledText.setStyleRanges(highlights);
		highlightIndex = -1;

		if (highlights.length > 0) {
			revealRange(highlights, 0);
			fireChangeEvent(new HighlightEvent(findText, highlights, 0));
		} else {
			Arrays.stream(currentHighlights).forEach(range-> styledText.redrawRange(range.start, range.length, true));
			fireChangeEvent(new HighlightEvent(findText, highlights, -1));
		}
	}

	private void revealRange(StyleRange[] highlights, int index) {
		Arrays.stream(highlights)
			.forEach(range -> {
				range.background = styledText.getSelectionBackground();
					range.foreground = null;
					styledText.redrawRange(range.start, range.length, true);
			});

		StyleRange currentRange = highlights[index];
		currentRange.background = styledText.getForeground();
		currentRange.foreground = styledText.getSelectionBackground();

		styledText.setStyleRanges(highlights);
		styledText.redrawRange(currentRange.start, currentRange.length, true);

		Rectangle firstRangeBounds = styledText.getTextBounds(currentRange.start,
			currentRange.start + currentRange.length);
		int topLine = styledText.getLineAtOffset(currentRange.start);
		int topLineOffset = styledText.getOffsetAtLine(topLine);
		int lineOffset = currentRange.start - topLineOffset;

		styledText.setSelection(currentRange.start, currentRange.start);
		styledText.redraw();
		highlightIndex = index;
	}

	private List<Integer> findOccurances(String content, String findText) {
		if (findText == null || findText.isEmpty())
			return Collections.emptyList();

		content = content.toLowerCase();
		findText = findText.toLowerCase();
		List<Integer> indexes = new ArrayList<Integer>();
		int index = 0;
		int wordLength = 0;

		while (index != -1) {
			index = content.indexOf(findText, index + wordLength);
			if (index != -1)
				indexes.add(index);
			wordLength = findText.length();
		}

		return indexes;
	}

	private static String print(URI uri) throws Exception {
		int options = -1;
		IFileStore fileStore = EFS.getStore(uri);
		try (InputStream in = fileStore.openInputStream(EFS.NONE, null);
			Jar jar = new Jar(fileStore.getName(), in);
			JarPrinter printer = new JarPrinter()) {
			printer.doPrint(jar, options, false, false);
			return printer.toString();
		}
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);

		IActionBars actionBars = getEditorSite().getActionBars();

		if (active) {
			update();
			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), findAction);
			actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), nextAction);
			actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), previousAction);
		} else {
			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), null);
		}
	}

	private void update() {
		if (loading || styledText == null || !isActive()) {
			return;
		}
		loading = true;
		JAREditor.background("Printing ZIP file", monitor -> {
			return print(uri);
		}, (text) -> {
			styledText.setText(text);
			styledText.setFocus();
		});

	}

	public void setInput(URI uri) {
		this.uri = uri;
		loading = false;
		update();
	}

	static class FindTextEvent extends Event {
		String findText;

		FindTextEvent(String s) {
			data = s;
		}
	}

	static class HighlightEvent extends Event {
		String			findText;
		StyleRange[] highlights;

		HighlightEvent(String text, StyleRange[] ranges, int i) {
			findText = text;
			highlights = ranges;
			index = i;
		}
	}

	private interface ChangeListener {
		void handleEvent(Event event);
	}

	private void addChangeListener(ChangeListener listener) {
		if (!changeListeners.contains(listener))
			changeListeners.add(listener);
	}

	private void fireChangeEvent(Event event) {
		changeListeners.forEach(listener -> {
			try {
				listener.handleEvent(event);
			} catch (Exception e) {}
		});
	}

	private List<ChangeListener> changeListeners = new CopyOnWriteArrayList<>();
}
