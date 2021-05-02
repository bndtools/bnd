package aQute.bnd.classfile;

import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.APPEND;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.CHOP;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.FULL_FRAME;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.RESERVED;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.SAME;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.SAME_FRAME_EXTENDED;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.SAME_LOCALS_1_STACK_ITEM_EXTENDED;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class StackMapTableAttribute implements Attribute {
	public static final String		NAME	= "StackMapTable";
	public final StackMapFrame[]	entries;

	public StackMapTableAttribute(StackMapFrame[] entries) {
		this.entries = entries;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(entries);
	}

	public static StackMapTableAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_entries = in.readUnsignedShort();
		StackMapFrame[] entries = new StackMapFrame[number_of_entries];
		for (int i = 0; i < number_of_entries; i++) {
			int frame_type = in.readUnsignedByte();
			if (frame_type <= SAME) { // same_frame
				entries[i] = new SameFrame(frame_type);
			} else if (frame_type <= StackMapFrame.SAME_LOCALS_1_STACK_ITEM) { // same_locals_1_stack_item_frame
				VerificationTypeInfo stack = VerificationTypeInfo.read(in, constant_pool);
				entries[i] = new SameLocals1StackItemFrame(frame_type, stack);
			} else if (frame_type <= RESERVED) { // RESERVED
				throw new IOException("Unrecognized stack map frame type " + frame_type);
			} else if (frame_type <= SAME_LOCALS_1_STACK_ITEM_EXTENDED) { // same_locals_1_stack_item_frame_extended
				int offset_delta = in.readUnsignedShort();
				VerificationTypeInfo stack = VerificationTypeInfo.read(in, constant_pool);
				entries[i] = new SameLocals1StackItemFrameExtended(frame_type, offset_delta, stack);
			} else if (frame_type <= CHOP) { // chop_frame
				int offset_delta = in.readUnsignedShort();
				entries[i] = new ChopFrame(frame_type, offset_delta);
			} else if (frame_type <= SAME_FRAME_EXTENDED) { // same_frame_extended
				int offset_delta = in.readUnsignedShort();
				entries[i] = new SameFrameExtended(frame_type, offset_delta);
			} else if (frame_type <= APPEND) { // append_frame
				int offset_delta = in.readUnsignedShort();
				int number_of_locals = frame_type - SAME_FRAME_EXTENDED;
				VerificationTypeInfo[] locals = new VerificationTypeInfo[number_of_locals];
				for (int n = 0; n < number_of_locals; n++) {
					locals[n] = VerificationTypeInfo.read(in, constant_pool);
				}
				entries[i] = new AppendFrame(frame_type, offset_delta, locals);
			} else if (frame_type <= FULL_FRAME) { // full_frame
				int offset_delta = in.readUnsignedShort();
				int number_of_locals = in.readUnsignedShort();
				VerificationTypeInfo[] locals = new VerificationTypeInfo[number_of_locals];
				for (int n = 0; n < number_of_locals; n++) {
					locals[n] = VerificationTypeInfo.read(in, constant_pool);
				}
				int number_of_stack_items = in.readUnsignedShort();
				VerificationTypeInfo[] stack = new VerificationTypeInfo[number_of_stack_items];
				for (int n = 0; n < number_of_stack_items; n++) {
					stack[n] = VerificationTypeInfo.read(in, constant_pool);
				}
				entries[i] = new FullFrame(frame_type, offset_delta, locals, stack);
			}
		}
		return new StackMapTableAttribute(entries);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(entries.length);
		for (StackMapFrame entry : entries) {
			entry.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (StackMapFrame entry : entries) {
			attribute_length += entry.value_length();
		}
		return attribute_length;
	}

	public abstract static class StackMapFrame {
		public static final int	SAME								= 63;
		public static final int	SAME_LOCALS_1_STACK_ITEM			= 127;
		public static final int	RESERVED							= 246;
		public static final int	SAME_LOCALS_1_STACK_ITEM_EXTENDED	= 247;
		public static final int	CHOP								= 250;
		public static final int	SAME_FRAME_EXTENDED					= 251;
		public static final int	APPEND								= 254;
		public static final int	FULL_FRAME							= 255;
		public final int		tag;

		protected StackMapFrame(int tag) {
			this.tag = tag;
		}

		public abstract int type();

		@Override
		public String toString() {
			return Integer.toString(tag);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
		}

		int value_length() {
			return 1 * Byte.BYTES;
		}
	}

	public static class SameFrame extends StackMapFrame {
		public SameFrame(int tag) {
			super(tag);
		}

		@Override
		public int type() {
			return SAME;
		}
	}

	public static class SameLocals1StackItemFrame extends StackMapFrame {
		public final VerificationTypeInfo stack;

		public SameLocals1StackItemFrame(int tag, VerificationTypeInfo stack) {
			super(tag);
			this.stack = stack;
		}

		@Override
		public int type() {
			return SAME_LOCALS_1_STACK_ITEM;
		}

		@Override
		public String toString() {
			return tag + "/" + stack;
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			stack.write(out, constant_pool);
		}

		@Override
		int value_length() {
			return 1 * Byte.BYTES + stack.value_length();
		}
	}

	public static class SameLocals1StackItemFrameExtended extends StackMapFrame {
		public final int					delta;
		public final VerificationTypeInfo	stack;

		public SameLocals1StackItemFrameExtended(int tag, int delta, VerificationTypeInfo stack) {
			super(tag);
			this.delta = delta;
			this.stack = stack;
		}

		@Override
		public int type() {
			return SAME_LOCALS_1_STACK_ITEM_EXTENDED;
		}

		@Override
		public String toString() {
			return tag + "/" + delta + "/" + stack;
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			out.writeShort(delta);
			stack.write(out, constant_pool);
		}

		@Override
		int value_length() {
			return 1 * Byte.BYTES + 1 * Short.BYTES + stack.value_length();
		}
	}

	public static class ChopFrame extends StackMapFrame {
		public final int delta;

		public ChopFrame(int tag, int delta) {
			super(tag);
			this.delta = delta;
		}

		@Override
		public int type() {
			return CHOP;
		}

		@Override
		public String toString() {
			return tag + "/" + delta;
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			out.writeShort(delta);
		}

		@Override
		int value_length() {
			return 1 * Byte.BYTES + 1 * Short.BYTES;
		}
	}

	public static class SameFrameExtended extends StackMapFrame {
		public final int delta;

		public SameFrameExtended(int tag, int delta) {
			super(tag);
			this.delta = delta;
		}

		@Override
		public int type() {
			return SAME_FRAME_EXTENDED;
		}

		@Override
		public String toString() {
			return tag + "/" + delta;
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			out.writeShort(delta);
		}

		@Override
		int value_length() {
			return 1 * Byte.BYTES + 1 * Short.BYTES;
		}
	}

	public static class AppendFrame extends StackMapFrame {
		public final int					delta;
		public final VerificationTypeInfo[]	locals;

		public AppendFrame(int tag, int delta, VerificationTypeInfo[] locals) {
			super(tag);
			this.delta = delta;
			this.locals = locals;
		}

		@Override
		public int type() {
			return APPEND;
		}

		@Override
		public String toString() {
			return tag + "/" + delta + "/" + Arrays.toString(locals);
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			out.writeShort(delta);
			for (VerificationTypeInfo local : locals) {
				local.write(out, constant_pool);
			}
		}

		@Override
		int value_length() {
			int value_length = 1 * Byte.BYTES + 1 * Short.BYTES;
			for (VerificationTypeInfo local : locals) {
				value_length += local.value_length();
			}
			return value_length;
		}
	}

	public static class FullFrame extends StackMapFrame {
		public final int					delta;
		public final VerificationTypeInfo[]	locals;
		public final VerificationTypeInfo[]	stack;

		public FullFrame(int tag, int delta, VerificationTypeInfo[] locals, VerificationTypeInfo[] stack) {
			super(tag);
			this.delta = delta;
			this.locals = locals;
			this.stack = stack;
		}

		@Override
		public int type() {
			return FULL_FRAME;
		}

		@Override
		public String toString() {
			return tag + "/" + delta + "/" + Arrays.toString(locals) + "/" + Arrays.toString(stack);
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			out.writeShort(delta);
			out.writeShort(locals.length);
			for (VerificationTypeInfo local : locals) {
				local.write(out, constant_pool);
			}
			out.writeShort(stack.length);
			for (VerificationTypeInfo stack_item : stack) {
				stack_item.write(out, constant_pool);
			}
		}

		@Override
		int value_length() {
			int value_length = 1 * Byte.BYTES + 3 * Short.BYTES;
			for (VerificationTypeInfo local : locals) {
				value_length += local.value_length();
			}
			for (VerificationTypeInfo stack_item : stack) {
				value_length += stack_item.value_length();
			}
			return value_length;
		}
	}

	public static class VerificationTypeInfo {
		public static final int	ITEM_Top				= 0;
		public static final int	ITEM_Integer			= 1;
		public static final int	ITEM_Float				= 2;
		public static final int	ITEM_Double				= 3;
		public static final int	ITEM_Long				= 4;
		public static final int	ITEM_Null				= 5;
		public static final int	ITEM_UninitializedThis	= 6;
		public static final int	ITEM_Object				= 7;
		public static final int	ITEM_Uninitialized		= 8;
		public final int		tag;

		public VerificationTypeInfo(int tag) {
			this.tag = tag;
		}

		static VerificationTypeInfo read(DataInput in, ConstantPool constant_pool) throws IOException {
			int tag = in.readUnsignedByte();
			switch (tag) {
				case ITEM_Top : // Top_variable_info
				case ITEM_Integer : // Integer_variable_info
				case ITEM_Float : // Float_variable_info
				case ITEM_Double : // Double_variable_info
				case ITEM_Long : // Long_variable_info
				case ITEM_Null : // Null_variable_info
				case ITEM_UninitializedThis : // UninitializedThis_variable_info
				{
					return new VerificationTypeInfo(tag);
				}
				case ITEM_Object : // Object_variable_info
				{
					int cpool_index = in.readUnsignedShort();
					return new ObjectVariableInfo(tag, constant_pool.className(cpool_index));
				}
				case ITEM_Uninitialized : // Uninitialized_variable_info
				{
					int offset = in.readUnsignedShort();
					return new UninitializedVariableInfo(tag, offset);
				}
				default : {
					throw new IOException("Unrecognized verification type tag " + tag);
				}
			}
		}

		@Override
		public String toString() {
			return Integer.toString(tag);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
		}

		int value_length() {
			return 1 * Byte.BYTES;
		}
	}

	public static class ObjectVariableInfo extends VerificationTypeInfo {
		public final String type;

		public ObjectVariableInfo(int tag, String type) {
			super(tag);
			this.type = type;
		}

		@Override
		public String toString() {
			return tag + ":" + type;
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			int cpool_index = constant_pool.classInfo(type);
			out.writeShort(cpool_index);
		}

		@Override
		int value_length() {
			return 1 * Byte.BYTES + 1 * Short.BYTES;
		}
	}

	public static class UninitializedVariableInfo extends VerificationTypeInfo {
		public final int offset;

		public UninitializedVariableInfo(int tag, int offset) {
			super(tag);
			this.offset = offset;
		}

		@Override
		public String toString() {
			return tag + ":" + offset;
		}

		@Override
		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeByte(tag);
			out.writeShort(offset);
		}

		@Override
		int value_length() {
			return 1 * Byte.BYTES + 1 * Short.BYTES;
		}
	}
}
