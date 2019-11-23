package bndtools.release;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import bndtools.release.api.ReleaseContext.Error;
import bndtools.release.nl.Messages;

public class ErrorList {

	private List<Error>	errors;
	private Composite	container;

	public ErrorList(List<Error> errors) {
		this.errors = errors;
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		container.setLayout(gridLayout);
		container.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		addErrorItems();
	}

	private void addErrorItems() {

		Composite comp = new Composite(container, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		comp.setLayout(gridLayout);
		comp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		for (Error error : errors) {
			ErrorItem errorItem = new ErrorItem(error);
			errorItem.createControl(comp);
		}
	}

	public Control getControl() {
		return container;
	}

	public void dispose() {
		container.dispose();
		container = null;
	}

	public static class ErrorItem {

		private Error error;

		public ErrorItem(Error error) {
			this.error = error;
		}

		public void createControl(Composite parent) {
			// Composite c1 = new Composite(parent, SWT.NONE);
			// GridLayout gridLayout = new GridLayout();
			// gridLayout.numColumns = 1;
			// gridLayout.horizontalSpacing = 0;
			// gridLayout.verticalSpacing = 5;
			// gridLayout.marginWidth = 0;
			// gridLayout.marginHeight = 10;
			//
			// c1.setLayout(gridLayout);
			// c1.setLayoutData(new GridData(SWT.HORIZONTAL, SWT.VERTICAL, true,
			// true));

			Group g = new Group(parent, SWT.SHADOW_ETCHED_IN);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 1;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 5;
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 10;
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.widthHint = 500;
			g.setLayout(gridLayout);
			g.setLayoutData(gridData);

			g.setText(error.getScope()
				+ (error.getSymbName() == null ? "" : " :  " + error.getSymbName() + "-" + error.getVersion())); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

			Composite c2 = new Composite(g, SWT.NONE);
			gridLayout = new GridLayout();
			gridLayout.numColumns = 2;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 5;
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 10;

			c2.setLayout(gridLayout);
			gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
			// gridData.horizontalAlignment = GridData.FILL;
			// gridData.grabExcessHorizontalSpace = true;
			c2.setLayoutData(gridData);

			Label label = new Label(c2, SWT.NONE);
			label.setText(Messages.message);

			Text text = new Text(c2, SWT.BORDER | SWT.MULTI);
			text.setEditable(false);
			text.setText(error.getMessage());

			if (error.getSymbName() != null) {
				label = new Label(c2, SWT.NONE);
				label.setText(Messages.symbolicName);

				text = new Text(c2, SWT.BORDER);
				text.setEditable(false);
				text.setText(error.getSymbName());

				label = new Label(c2, SWT.NONE);
				label.setText(Messages.version1);

				text = new Text(c2, SWT.BORDER);
				text.setEditable(false);
				text.setText(error.getVersion());
			}

			createTableViewer(g);
		}

		private void createTableViewer(Composite parent) {
			if (error.getList() == null) {
				return;
			}

			Composite c2 = new Composite(parent, SWT.NONE);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 1;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 5;
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 10;

			c2.setLayout(gridLayout);
			c2.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

			TableViewer viewer = new TableViewer(c2, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

			String[] headers = error.getHeaders();
			if (headers.length == 0) {
				headers = new String[error.getList().length];
				for (int i = 0; i < headers.length; i++) {
					headers[i] = ""; //$NON-NLS-1$
				}
			}

			for (int i = 0; i < headers.length; i++) {
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn()
					.setText(headers[i]);
				column.getColumn()
					.setWidth(getWidth(c2, headers[i], i));
				column.getColumn()
					.setResizable(true);
				column.getColumn()
					.setMoveable(true);
			}
			Table table = viewer.getTable();
			table.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

			if (error.getHeaders().length > 0) {
				table.setHeaderVisible(true);
			}
			table.setLinesVisible(true);

			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new ArrayLabelProvider());

			viewer.setInput(error.getList());
		}

		private int getWidth(Drawable cmp, String title, int colNo) {
			String[][] table = error.getList();
			int maxLength = title.length();
			for (int i = 0; i < table.length; i++) {
				int len = table[i][colNo].length();
				if (len > maxLength) {
					maxLength = len;
				}
			}
			maxLength += 2;
			GC gc = new GC(cmp);
			int charWidth = (int) Math.round(gc.getFontMetrics()
				.getAverageCharacterWidth());
			gc.dispose();
			return charWidth * maxLength;
		}

	}

	private static class ArrayLabelProvider extends LabelProvider implements ITableLabelProvider {
		public ArrayLabelProvider() {
			super();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String[] cols = (String[]) element;
			return cols[columnIndex];
		}
	}

	private static class ArrayContentProvider implements IStructuredContentProvider {
		public ArrayContentProvider() {
			super();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			String[][] table = (String[][]) inputElement;
			return table;
		}

		@Override
		public void dispose() {
			/* ignore */
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			/* ignore */
		}

	}

}
