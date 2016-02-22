package aQute.bnd.jpm.util;

import java.util.List;

import aQute.struct.struct;

/**
 * The messages used in the JSON RPC protocol
 */
public interface JSON {
	public static class Request extends struct {
		public String		jsonrpc	= "2.0";
		public String		method;
		public List<Object>	params	= list();
		public long			id;
	}

	public static class Response extends struct {
		public String		jsonrpc	= "2.0";
		public Object		result;
		public JSONRPCError	error;
		public long			id;
	}

	public static class JSONRPCError extends struct {
		public long			code;
		public String		message;
		public List<String>	trace;
	}

}
