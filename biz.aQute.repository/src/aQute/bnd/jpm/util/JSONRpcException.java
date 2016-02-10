package aQute.bnd.jpm.util;

import java.io.PrintStream;
import java.io.PrintWriter;

import aQute.jsonrpc.domain.JSON.JSONRPCError;
import aQute.lib.strings.Strings;

public class JSONRpcException extends RuntimeException {
	private static final long	serialVersionUID	= 1L;
	private JSONRPCError		error;

	public JSONRpcException(JSONRPCError error) {
		this.error = error;
	}

	public String toString() {
		return "[" + error.code + "] " + error.message;
	}

	@Override
	public void printStackTrace(PrintWriter writer) {
		writer.append("FROM REMOTE SYSTEM\n");
		if (error != null && error.trace != null)
			writer.append(Strings.join("\n", error.trace));
	}

	@Override
	public void printStackTrace(PrintStream writer) {
		PrintWriter pw = new PrintWriter(writer);
		printStackTrace(pw);
	}

	@Override
	public void printStackTrace() {
		printStackTrace(System.out);
	}

}
