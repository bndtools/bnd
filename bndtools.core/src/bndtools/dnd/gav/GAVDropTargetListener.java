package bndtools.dnd.gav;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryResourceElement;

public abstract class GAVDropTargetListener implements DropTargetListener {

	private final static Logger					logger					= LoggerFactory
		.getLogger(GAVDropTargetListener.class);

	private volatile boolean				alternateSyntaxEnabled	= false;

	private final TextTransfer				textTransfer			= TextTransfer.getInstance();
	private final LocalSelectionTransfer	localSelectionTransfer	= LocalSelectionTransfer.getTransfer();
	private final StyledText				styledText;

	public GAVDropTargetListener(StyledText styledText) {
		this.styledText = requireNonNull(styledText);
		addAlternateSyntaxKeyListener(this.styledText);
	}

	abstract boolean hasAlternateSyntax();

	abstract String format(Resource resource, RepositoryPlugin repositoryPlugin, boolean noVersion,
		boolean useAlternateSyntax, String line, String indentPrefix);

	public boolean isAlternateSyntaxEnabled() {
		return alternateSyntaxEnabled;
	}

	@Override
	public void drop(DropTargetEvent event) {
		if (textTransfer.isSupportedType(event.currentDataType)) {
			String lineAtInsertionPoint = styledText.getLine(styledText.getLineAtOffset(styledText.getCaretOffset()));
			String indentPrefix = lineAtInsertionPoint.split("\\S", 2)[0];
			// always move the caret to the end of the line
			styledText.invokeAction(ST.LINE_END);

			String text = (String) event.data;
			ISelection selection = localSelectionTransfer.getSelection();
			Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
			while (iterator.hasNext()) {
				Object item = iterator.next();
				if (item instanceof RepositoryBundle) {
					RepositoryBundle rb = (RepositoryBundle) item;
					text = format(rb.getResource(), rb.getRepo(), true, isAlternateSyntaxEnabled(),
						lineAtInsertionPoint, indentPrefix);
					break;
				} else if (item instanceof RepositoryBundleVersion) {
					RepositoryBundleVersion rbv = (RepositoryBundleVersion) item;
					text = format(rbv.getResource(), rbv.getRepo(), false, isAlternateSyntaxEnabled(),
						lineAtInsertionPoint, indentPrefix);
					break;
				} else if (item instanceof RepositoryResourceElement) {
					RepositoryResourceElement rbe = (RepositoryResourceElement) item;
					text = format(rbe.getResource(), rbe.getRepositoryBundleVersion()
						.getRepo(), true, isAlternateSyntaxEnabled(), lineAtInsertionPoint, indentPrefix);
					break;
				}
				logger.debug("drop event.data {}", event.data);
			}
			if (text != null) {
				styledText.insert(text);
			}
		}
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT) {
			if ((event.operations & DND.DROP_COPY) != 0) {
				event.detail = DND.DROP_COPY;
			} else {
				event.detail = DND.DROP_NONE;
			}
		}
	}

	@Override
	public void dragLeave(DropTargetEvent event) {}

	@Override
	public void dragOperationChanged(DropTargetEvent event) {}

	@Override
	public void dragOver(DropTargetEvent event) {}

	@Override
	public void dropAccept(DropTargetEvent event) {}

	public static String indent(boolean tabs, int size) {
		return new String(new char[size]).replace("\0", tabs ? "\t" : " ");
	}

	private void addAlternateSyntaxKeyListener(Control control) {
		if (!hasAlternateSyntax()) {
			return;
		}
		Display display = control.getDisplay();
		display.addFilter(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.ALT) {
					alternateSyntaxEnabled = true;
				}
			}
		});
		display.addFilter(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.ALT) {
					alternateSyntaxEnabled = false;
				}
			}
		});
	}
}
