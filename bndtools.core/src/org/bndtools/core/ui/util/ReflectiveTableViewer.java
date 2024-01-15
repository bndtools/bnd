package org.bndtools.core.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings({
	"rawtypes", "unchecked"
})
public class ReflectiveTableViewer<T> extends TableViewer {
	final static Image	checked		= Icons.image("checked", false);
	final static Image	unchecked	= Icons.image("unchecked", false);

	public enum ColumnType {
		TEXT,
		CHECKBOX
	}

	public ReflectiveTableViewer(Composite parent, int style) {
		super(parent, style);
		setContentProvider(ArrayContentProvider.getInstance());

	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public static class TableColumnDTO<X> {
		final String						title;
		final int							width;
		final ColumnType					type;
		final Function<Object, Object>		getter;
		final BiConsumer<Object, Object>	setter;

		Function<Object, String>			validator;
		Function<Object, Boolean>			enabled;

		TableColumnDTO(String title, int width, ColumnType type, Function getter, BiConsumer setter) {
			this.title = title;
			this.width = width;
			this.type = type;
			this.getter = getter;
			this.setter = setter;
		}

		public TableColumnDTO(String title, int width, ColumnType type, Function<Object, Object> getter) {
			this(title, width, type, getter, null);
		}

		public TableColumnDTO<X> validate(Function<Object, String> validator) {
			this.validator = validator;
			return this;
		}

		public TableColumnDTO<X> enabled(Function<X, Boolean> enabled) {
			this.enabled = (Function<Object, Boolean>) enabled;
			return this;
		}

		@Override
		public String toString() {
			return title + ":" + width + ":" + type;
		}
	}

	abstract class Handler extends EditingSupport implements ICellEditorValidator {
		final TableColumnDTO<?> c;

		Handler(TableColumnDTO<?> c) {
			super(ReflectiveTableViewer.this);
			this.c = c;
		}

		protected abstract CellLabelProvider getLabelProvider();

		@Override
		protected boolean canEdit(Object element) {
			return c.setter != null && (c.enabled == null || c.enabled.apply(element));
		}

		@Override
		protected Object getValue(Object element) {
			return c.getter.apply(element);
		}

		@Override
		protected void setValue(Object element, Object value) {
			c.setter.accept(element, value);
			update(element, null);
		}

		@Override
		public String toString() {
			return "handler " + c;
		}

		@Override
		public String isValid(Object value) {
			if (c.validator == null)
				return null;
			return c.validator.apply(value);
		}

	}

	class BooleanHandler extends Handler {

		public BooleanHandler(TableColumnDTO c) {
			super(c);
		}

		@Override
		protected CellLabelProvider getLabelProvider() {
			return new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return null;
				}

				@Override
				public Image getImage(Object element) {
					boolean b = (boolean) c.getter.apply(element);
					return b ? checked : unchecked;
				}
			};
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			CheckboxCellEditor editor = new CheckboxCellEditor(getTable(), SWT.CHECK);
			editor.setValidator(this);
			return editor;
		}

	}

	class TextHandler extends Handler {

		public TextHandler(TableColumnDTO c) {
			super(c);
		}

		@Override
		protected CellLabelProvider getLabelProvider() {
			return new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					Object value = c.getter.apply(element);
					return value != null ? value.toString() : "";
				}
			};
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			TextCellEditor editor = new TextCellEditor(getTable());
			editor.setValidator(this);
			return editor;
		}

	}

	final List<TableColumnDTO> columns = new ArrayList<>();
	final List<Handler>			handlers	= new ArrayList<>();

	public TableColumnDTO<T> checkbox(String title, int width, Function<T, Boolean> getter) {
		return new TableColumnDTO(title, width, ColumnType.CHECKBOX, getter, null);
	}

	public TableColumnDTO<T> checkbox(String title, int width, Function<T, Boolean> getter,
		BiConsumer<T, Boolean> setter) {
		return add(new TableColumnDTO(title, width, ColumnType.CHECKBOX, getter, setter));
	}

	public TableColumnDTO<T> text(String title, int width, Function<T, String> getter) {
		return add(new TableColumnDTO(title, width, ColumnType.TEXT, getter, null));
	}

	public TableColumnDTO<T> text(String title, int width, Function<T, String> getter, BiConsumer<T, String> setter) {
		return add(new TableColumnDTO(title, width, ColumnType.TEXT, getter, setter));
	}

	TableColumnDTO<T> add(TableColumnDTO c) {
		columns.add(c);
		return c;
	}

	public void build() {

		for (TableColumnDTO c : columns) {
			Handler handler = switch (c.type) {
				case CHECKBOX -> new BooleanHandler(c);
				case TEXT -> new TextHandler(c);
			};

			TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
			column.getColumn()
				.setText(c.title);
			column.getColumn()
				.setWidth(c.width);
			column.setLabelProvider(handler.getLabelProvider());
			column.setEditingSupport(handler);
		}
		getTable().setHeaderVisible(true);
		getTable().setLinesVisible(true);
	}

	public String validate() {
		StringBuilder sb = new StringBuilder();
		for (Handler handler : handlers) {

		}
		return sb.isEmpty() ? null : sb.toString();
	}

}
