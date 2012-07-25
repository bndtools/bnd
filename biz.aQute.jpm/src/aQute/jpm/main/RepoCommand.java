package aQute.jpm.main;

import java.io.*;
import java.text.*;

import aQute.lib.collections.*;
import aQute.lib.getopt.*;

public class RepoCommand {
//	final static SimpleDateFormat	date	= new SimpleDateFormat();
//	final Main						main;
//
//	public RepoCommand(Main main) {
//		this.main = main;
//	}
//
//	interface ListOptions extends Options {
//		boolean revisions();
//	}
//
//	public void _list(ListOptions options) throws Exception {
//		ExtList<String> l = new ExtList<String>(options._());
//			
//		for (String program : main.cache.list(null)) {
//			main.out.printf("%-20s %s\n",program, options.revisions() ? main.cache.versions(program):"");
//		}
//	}
//
//	interface GetOptions extends Options {}
//
//	public void _get(GetOptions options) throws Exception {
//		for (String id : options._()) {
//			String[] split = id.split("-");
//			if (split.length != 2)
//				main.error("Not a revision identifier %s", id);
//			else {
//				File f = main.cache.get(split[0], split[1]);
//				if (f == null)
//					main.error("No such revision %s", id);
//				else
//					main.out.println(f.getAbsolutePath());
//			}
//		}
//	}
//
//
/////	String description = program.description;
////	for (int i = 0; i < program.revisions.size() && description == null; i++)
////		description = program.revisions.get(i).description;
////
////	main.out.printf("%-30s %20s %s %s\n", program._id, date.format(program.modified), notNull(description),
////			notNull(program.vendor));
////	if (options.revisions()) {
////		for (RevisionRef ref : program.revisions) {
////			main.out.printf("  %-10s %c %s %s %s\n", ref.version.base, ref.master ? 'M' : 'S',
////					Hex.toHexString(ref.sha), ref.url, notNull(ref.summary));
////		}
////
////	}
}
