package aQute.libg.remote.sink;

import java.io.*;

import aQute.libg.command.*;
import aQute.libg.remote.*;

public class AreaImpl extends Area {
	File				root;
	File				cwd;
	Command				command;
	Thread				thread;
	InputStream			stdin;
	Appendable			stdout;
	Appendable			stderr;
	PipedOutputStream	toStdin;
}
