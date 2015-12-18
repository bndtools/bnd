package aQute.libg.remote.sink;

import java.io.File;
import java.io.InputStream;
import java.io.PipedOutputStream;

import aQute.libg.command.Command;
import aQute.libg.remote.Area;

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
