package aQute.bnd.runtime.gogo;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;

import aQute.lib.dtoformatter.Cell;
import aQute.lib.dtoformatter.DTOFormatter;
import aQute.lib.dtoformatter.Table;
import aQute.lib.filter.Filter;
import aQute.lib.strings.Strings;

public class View {

	final BundleContext	context;
	final DTOFormatter	formatter;

	public View(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		this.formatter = formatter;

	}

	@Descriptor("view a list as a table to control sorting and selecting")
	public Cell vw(CommandSession session, @Parameter(names = {
		"-c", "--column"
	}, absentValue = "") String columns, @Parameter(names = {
		"-w", "--where"
	}, absentValue = "") String where, @Parameter(names = {
		"-s", "--sort"
	}, absentValue = "") String sort, @Parameter(names = {
		"-a", "--ascent"
	}, absentValue = "false", presentValue = "true") boolean ascent,

		Collection<?> something) {

		return convert(session, columns, where, sort, ascent, something);
	}

	private Cell convert(CommandSession session, String columns, String where, String sort, boolean ascent,
		Object something) {
		Cell cell = formatter.cell(something, (src, level, nxt) -> session.format(src, level));
		if (!(cell instanceof Table))
			return cell;

		Table table = (Table) cell;

		if (!where.isEmpty()) {
			if (!where.startsWith("(")) {
				where = "(" + where;
			}
			if (!where.endsWith("(")) {
				where = where + ")";
			}
			Filter filter = new Filter(where);
			table = table.select(map -> {
				try {
					return filter.matchMap(map);
				} catch (Exception e) {
					return false;
				}
			});
		}

		if (!columns.isEmpty())
			table = table.select(Strings.split(columns));

		if (!sort.isEmpty())
			table.sort(sort, ascent);

		return table;
	}

	@Descriptor("view a list as a table to control sorting and selecting")
	public Cell vw(CommandSession session, @Parameter(names = {
		"-c", "--column"
	}, absentValue = "") String columns, @Parameter(names = {
		"-w", "--where"
	}, absentValue = "") String where, @Parameter(names = {
		"-s", "--sort"
	}, absentValue = "") String sort, @Parameter(names = {
		"-a", "--ascent"
	}, absentValue = "false", presentValue = "true") boolean ascent, String... args) throws Exception {
		Object result = session.execute(Stream.of(args)
			.collect(Collectors.joining(" ")));

		return convert(session, columns, where, sort, ascent, result);
	}

}
