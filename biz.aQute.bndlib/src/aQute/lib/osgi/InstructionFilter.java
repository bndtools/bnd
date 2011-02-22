package aQute.lib.osgi;

import java.io.*;
import java.util.regex.*;

public class InstructionFilter implements FileFilter {

	private Instruction instruction;
	private boolean recursive;
	private Pattern doNotCopy;
	
	public InstructionFilter (Instruction instruction, boolean recursive, Pattern doNotCopy) {
		this.instruction = instruction;
		this.recursive = recursive;
		this.doNotCopy = doNotCopy;
	}
	public InstructionFilter (Instruction instruction, boolean recursive) {
		this(instruction, recursive, Pattern.compile(Constants.DEFAULT_DO_NOT_COPY));
	}
	public boolean isRecursive() {
		return recursive;
	}
	public boolean accept(File pathname) {
		if (doNotCopy != null && doNotCopy.matcher(pathname.getName()).matches()) {
			return false;
		}

		if (pathname.isDirectory() && isRecursive()) {
			return true;
		}
		
		if (instruction == null) {
			return true;
		}
		return !instruction.isNegated() == instruction.matches(pathname.getName());
	}
}
