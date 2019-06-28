package aQute.lib.dtoformatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.libg.glob.Glob;

/**
 * A table consists of rows and columns of {@link Cell} objects.
 */
public class Table implements Cell {

	public final int			rows;
	public final int			cols;
	public final Cell[][]		cells;
	final public int			headers;
	Canvas.Style				style	= Canvas.PLAIN;
	Object						original;

	public static final Cell	EMPTY	= new Cell() {

											@Override
											public int width() {
												return 3;
											}

											@Override
											public int height() {
												return 3;
											}

											@Override
											public String toString() {
												return " ";
											}

											@Override
											public Object original() {
												return null;
											}
										};

	public Table(int rows, int cols, int headers) {
		this.rows = rows;
		this.cols = cols;
		this.headers = headers;
		cells = new Cell[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				cells[r][c] = EMPTY;
			}
		}
	}

	public Table(List<List<String>> matrix, int headers) {
		this(matrix.size(), maxWidth(matrix), headers);
		int r = headers;

		for (List<String> row : matrix) {
			int c = 0;
			for (String col : row) {
				this.set(r, c, col);
				c++;
			}
			r++;
		}
	}

	private static int maxWidth(List<List<String>> matrix) {
		int maxWidth = 0;
		for (List<String> row : matrix) {
			maxWidth = Math.max(maxWidth, row.size());
		}
		return maxWidth;
	}

	/**
	 * Width including borders
	 */
	@Override
	public int width() {
		int w = 0;
		for (int c = 0; c < cols; c++) {
			w += width(c) - 1; // remove right border width because we overlap
		}
		return w + 1; // add right border
	}

	/**
	 * Height including borders
	 */
	@Override
	public int height() {
		int h = 0;
		for (int r = 0; r < rows; r++) {
			h += height(r) - 1;// remove bottom border width because we overlap
		}
		return h + 1; // add bottom border
	}

	public void set(int r, int c, Object label) {
		cells[r][c] = new StringCell("" + label, label);
	}

	public void set(int r, int c, Cell table) {
		cells[r][c] = table;
	}

	public Cell[] row(int row) {
		return cells[row];
	}

	@Override
	public String toString() {
		if (cols == 1 && rows > 3)
			return transpose(0).toString("⁻¹");
		else
			return toString(null);
	}

	@Override
	public Canvas render(int width, int height) {
		return render(width, height, 0, 0, 0, 0);
	}

	public Canvas render(int width, int height, int left, int top, int right, int bottom) {

		Canvas canvas = new Canvas(width + left + right, height + top + bottom);
		canvas.box(left, top, width, height, style);

		int y = top;

		for (int r = 0; r < rows; r++) {
			int ch = height(r);
			int x = left;
			for (int c = 0; c < cols; c++) {
				int cw;
				if (c == cols - 1) {
					// adjust last column width
					cw = width - x - left;
				} else {
					cw = width(c);
				}
				Cell cell = cells[r][c];
				Canvas foo = cell.render(cw, ch);
				canvas.merge(foo, x, y);
				x += cw - 1;
			}
			y += ch - 1;
		}
		return canvas;
	}

	/**
	 * Width of a column without borders
	 *
	 * @param col
	 * @return
	 */
	private int width(int col) {
		int w = 0;
		for (int r = 0; r < rows; r++) {
			Cell cell = cells[r][col];
			int width = cell.width();
			if (width > w)
				w = width;
		}
		return w;
	}

	public int height(int row) {
		int h = 0;
		for (int c = 0; c < cols; c++) {
			Cell cell = cells[row][c];
			int height = cell.height();
			if (height > h)
				h = height;
		}
		return h;
	}

	public Table transpose(int headers) {
		Table transposed = new Table(cols, rows, headers);
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				Cell c = this.get(row, col);
				transposed.set(col, row, c);
			}
		}
		return transposed;
	}

	public Cell get(int row, int col) {
		return cells[row][col];
	}

	public String toString(String message) {
		if (message == null)
			message = "";

		if (rows == 0 || cols == 0) {
			return "☒" + message;
		}
		Canvas render = render(width(), height(), 0, 0, message.length(), 0);
		render.set(width(), 0, message);
		return render.toString();
	}

	public Table addColum(int col) {
		Table t = new Table(rows, cols + 1, headers);
		if (col > 0)
			copyTo(t, 0, 0, 0, 0, rows, col);

		return copyTo(t, 0, col, 0, col + 1, rows, cols - col);
	}

	public Table setColumn(int col, Object cell) {
		for (int r = 0; r < rows; r++)
			set(r, col, cell);

		return this;
	}

	public Table copyTo(Table dest, int sourceRow, int sourceCol, int destRow, int destCol, int rows, int cols) {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				Cell cell = get(sourceRow + i, sourceCol + j);
				dest.set(destRow + i, destCol + j, cell);
			}
		}
		return dest;
	}

	public void copyColumn(Table src, int from, int to) {
		copyTo(src, 0, from, 0, to, rows, 1);
	}

	public void setBold() {
		style = Canvas.BOLD;
	}

	public Table select(List<String> columns) {
		Table dst = new Table(rows, columns.size(), headers);
		for (int toColumn = 0; toColumn < columns.size(); toColumn++) {
			int srcColumn = findHeader(columns.get(toColumn));
			if (srcColumn >= 0) {
				this.copyColumn(dst, srcColumn, toColumn);
			} else {
				throw new IllegalArgumentException("No such column " + columns.get(toColumn));
			}
		}
		return dst;
	}

	/**
	 * Select matching rows. Each row is translated to a map and then run
	 * against the given predicate. If the predicate matches the row is included
	 * in the output.
	 *
	 * @param predicate
	 * @return a new table with only the matching rows
	 */
	public Table select(Predicate<Map<String, Object>> predicate) {
		String colNames[] = new String[cols];

		if (headers == 0) {
			for (int i = 0; i < colNames.length; i++) {
				colNames[i] = "" + i;
			}
		} else {
			for (int i = 0; i < colNames.length; i++) {
				colNames[i] = get(0, i).toString();
			}
		}

		List<Integer> selectedRows = new ArrayList<>();

		Map<String, Object> map = new HashMap<>();
		for (int r = headers; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				Cell cell = get(r, c);
				Object value;
				if (cell instanceof Table) {
					value = ((Table) cell).toList();
				} else {
					String s = cell.toString();
					try {
						value = Long.parseLong(s);
					} catch (Exception e) {
						try {
							value = Double.parseDouble(s);
						} catch (Exception ee) {
							value = s;
						}
					}
				}
				map.put(colNames[c], value);
			}
			if (predicate.test(map))
				selectedRows.add(r);
		}

		Table result = new Table(selectedRows.size() + headers, cols, headers);

		copyTo(result, 0, 0, 0, 0, headers, cols);

		for (int r = 0; r < selectedRows.size(); r++) {
			copyTo(result, selectedRows.get(r), 0, r + result.headers, 0, 1, cols);
		}
		return result;
	}

	public List<String> toList() {
		List<String> list = new ArrayList<>();
		for (int r = headers; r < rows; r++) {
			list.add(toString(r));
		}
		return list;
	}

	public String toString(int row) {
		return Stream.of(cells[row])
			.map(Object::toString)
			.collect(Collectors.joining(","));
	}

	public int findHeader(String name) {
		Glob glob = new Glob(name);
		for (int h = 0; h < headers; h++) {
			for (int c = 0; c < cols; c++) {
				if (Integer.toString(c)
					.equals(name))
					return c;

				String header = get(h, c).toString();
				if (glob.finds(header) >= 0)
					return c;
			}
		}
		return -1;
	}

	public void sort(String sort, boolean reverse) {
		int col = findHeader(sort);
		if (col < 0)
			return;
		Comparator<Cell[]> cmp = (Cell[] a, Cell[] b) -> {

			Object aa = a[col].original();
			Object bb = b[col].original();
			if (aa == bb)
				return 0;

			if (aa == null)
				return -1;
			if (bb == null)
				return 1;

			String aaa = aa.toString();
			String bbb = bb.toString();

			try {
				long la = Long.parseLong(aaa);
				long lb = Long.parseLong(bbb);
				return Long.compare(la, lb);
			} catch (Exception e) {
				try {
					double la = Long.parseLong(aaa);
					double lb = Long.parseLong(bbb);
					return Double.compare(la, lb);
				} catch (Exception ee) {
					return aaa.compareTo(bbb);
				}
			}
		};

		Arrays.sort(cells, headers, rows, reverse ? cmp.reversed() : cmp);
	}

	@Override
	public Object original() {
		return original;
	}

}
