package bndtools.editor.common;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.utils.collections.CollectionUtils;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class UpDownButtonBarPart {

	private final Image	imgUp	= AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_up.png")
		.createImage();
	private final Image	imgDown	= AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_down.png")
		.createImage();

	public interface UpDownListener {
		void changed(List<Object> list);
	}

	private final TableViewer			viewer;
	private final List<UpDownListener>	listeners	= new ArrayList<>();

	private ToolBar						toolbar;

	private ToolItem					btnUp;
	private ToolItem					btnDown;

	public UpDownButtonBarPart(TableViewer viewer) {
		this.viewer = viewer;
	}

	public Control createControl(Composite parent, int style) {
		toolbar = new ToolBar(parent, style);

		btnUp = new ToolItem(toolbar, SWT.PUSH);
		btnUp.setImage(imgUp);
		btnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doMoveUp();
			}
		});

		btnDown = new ToolItem(toolbar, SWT.PUSH);
		btnDown.setImage(imgDown);
		btnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doMoveDown();
			}
		});

		toolbar.addDisposeListener(de -> {
			if (!imgUp.isDisposed())
				imgUp.dispose();
			if (!imgDown.isDisposed())
				imgDown.dispose();
		});
		return toolbar;
	}

	public void setEnabledUp(boolean enabled) {
		btnUp.setEnabled(enabled);
	}

	public void setEnabledDown(boolean enabled) {
		btnDown.setEnabled(enabled);
	}

	private void doMoveUp() {
		int[] indexes = viewer.getTable()
			.getSelectionIndices();

		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) viewer.getInput();
		if (CollectionUtils.moveUp(list, indexes)) {
			viewer.setInput(list);
			for (UpDownListener l : listeners) {
				l.changed(list);
			}
		}
	}

	private void doMoveDown() {
		int[] indexes = viewer.getTable()
			.getSelectionIndices();

		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) viewer.getInput();
		if (CollectionUtils.moveDown(list, indexes)) {
			viewer.setInput(list);
			for (UpDownListener l : listeners) {
				l.changed(list);
			}
		}
	}

	public void addListener(UpDownListener l) {
		listeners.add(l);
	}

	public void removeListener(UpDownListener l) {
		listeners.remove(l);
	}
}
