package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CodeAttribute implements Attribute {
	public static final String		NAME	= "Code";
	public final int				max_stack;
	public final int				max_locals;
	public final ByteBuffer			code;
	public final ExceptionHandler[]	exception_table;
	public final Attribute[]		attributes;

	CodeAttribute(int max_stack, int max_locals, ByteBuffer code, ExceptionHandler[] exception_table,
		Attribute[] attributes) {
		this.max_stack = max_stack;
		this.max_locals = max_locals;
		this.code = code;
		this.exception_table = exception_table;
		this.attributes = attributes;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(attributes);
	}

	static CodeAttribute parseCodeAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int max_stack = in.readUnsignedShort();
		int max_locals = in.readUnsignedShort();
		int code_length = in.readInt();
		ByteBuffer code = ClassFile.slice(in, code_length);
		int exception_table_length = in.readUnsignedShort();
		ExceptionHandler[] exception_table = new ExceptionHandler[exception_table_length];
		for (int i = 0; i < exception_table_length; i++) {
			exception_table[i] = ExceptionHandler.parseExceptionHandler(in, constant_pool);
		}
		Attribute[] attributes = ClassFile.parseAttributes(in, constant_pool);
		return new CodeAttribute(max_stack, max_locals, code, exception_table, attributes);
	}

	public static class ExceptionHandler {
		public final int	start_pc;
		public final int	end_pc;
		public final int	handler_pc;
		public final String	catch_type;

		ExceptionHandler(int start_pc, int end_pc, int handler_pc, String catch_type) {
			this.start_pc = start_pc;
			this.end_pc = end_pc;
			this.handler_pc = handler_pc;
			this.catch_type = catch_type;
		}

		@Override
		public String toString() {
			return String.format("from=%d to=%d target=%d type=%s", start_pc, end_pc, handler_pc, catch_type);
		}

		static ExceptionHandler parseExceptionHandler(DataInput in, ConstantPool constant_pool) throws IOException {
			int start_pc = in.readUnsignedShort();
			int end_pc = in.readUnsignedShort();
			int handler_pc = in.readUnsignedShort();
			int catch_type = in.readUnsignedShort();
			String catch_type_name = (catch_type != 0) ? constant_pool.className(catch_type) : null;
			return new ExceptionHandler(start_pc, end_pc, handler_pc, catch_type_name);
		}
	}
}
