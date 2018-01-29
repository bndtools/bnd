package aQute.lib.justif;

import java.util.ArrayList;
import java.util.List;

public class Table {
	int					row;
	int					column;
	int					maxColumn	= 0;
	List<List<Justif>>	matrix		= new ArrayList<>();

	public Justif nextCell(String format, Object args) {
		return cell(row, column++);
	}

	public Justif firstCell() {
		return cell(row++, column = 0);
	}

	private Justif cell(int row, int column) {
		while (matrix.size() <= row)
			matrix.add(new ArrayList<>());

		List<Justif> line = matrix.get(row);
		while (line.size() <= column) {
			line.add(new Justif());
			maxColumn = Math.max(line.size(), maxColumn);
		}

		return line.get(column);
	}

	public void append(Appendable app) {
		for (int r = 0; r < matrix.size(); r++) {
			List<Justif> line = matrix.get(r);
			for (int c = 0; c < line.size(); c++) {

			}
		}

	}

}
