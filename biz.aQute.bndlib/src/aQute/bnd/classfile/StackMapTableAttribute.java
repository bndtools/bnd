package aQute.bnd.classfile;

import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.APPEND;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.CHOP;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.FULL_FRAME;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.RESERVED;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.SAME;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.SAME_FRAME_EXTENDED;
import static aQute.bnd.classfile.StackMapTableAttribute.StackMapFrame.SAME_LOCALS_1_STACK_ITEM_EXTENDED;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Double;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Float;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Integer;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Long;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Null;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Object;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Top;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_Uninitialized;
import static aQute.bnd.classfile.StackMapTableAttribute.VerificationTypeInfo.ITEM_UninitializedThis;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class StackMapTableAttribute implements Attribute {
	public static final String		NAME	= "StackMapTable";
	public final StackMapFrame[]	entries;

	StackMapTableAttribute(StackMapFrame[] entries) {
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

	static StackMapTableAttribute parseStackMapTableAttribute(DataInput in, ConstantPool constant_pool)
		throws IOException {
		int number_of_entries = in.readUnsignedShort();
		StackMapFrame[] entries = new StackMapFrame[number_of_entries];
		for (int i = 0; i < number_of_entries; i++) {
			int frame_type = in.readUnsignedByte();
			if (frame_type <= SAME) { // same_frame
				entries[i] = new SameFrame(frame_type);
			} else if (frame_type <= StackMapFrame.SAME_LOCALS_1_STACK_ITEM) { // same_locals_1_stack_item_frame
				VerificationTypeInfo stack = parseVerificationTypeInfo(in, constant_pool);
				entries[i] = new SameLocals1StackItemFrame(frame_type, stack);
			} else if (frame_type <= RESERVED) { // RESERVED
				throw new IOException("Unrecognized stack map frame type " + frame_type);
			} else if (frame_type <= SAME_LOCALS_1_STACK_ITEM_EXTENDED) { // same_locals_1_stack_item_frame_extended
				int offset_delta = in.readUnsignedShort();
				VerificationTypeInfo stack = parseVerificationTypeInfo(in, constant_pool);
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
					locals[n] = parseVerificationTypeInfo(in, constant_pool);
				}
				entries[i] = new AppendFrame(frame_type, offset_delta, locals);
			} else if (frame_type <= FULL_FRAME) { // full_frame
				int offset_delta = in.readUnsignedShort();
				int number_of_locals = in.readUnsignedShort();
				VerificationTypeInfo[] locals = new VerificationTypeInfo[number_of_locals];
				for (int n = 0; n < number_of_locals; n++) {
					locals[n] = parseVerificationTypeInfo(in, constant_pool);
				}
				int number_of_stack_items = in.readUnsignedShort();
				VerificationTypeInfo[] stack = new VerificationTypeInfo[number_of_stack_items];
				for (int n = 0; n < number_of_stack_items; n++) {
					stack[n] = parseVerificationTypeInfo(in, constant_pool);
				}
				entries[i] = new FullFrame(frame_type, offset_delta, locals, stack);
			}
		}
		return new StackMapTableAttribute(entries);
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

		StackMapFrame(int tag) {
			this.tag = tag;
		}

		public abstract int type();

		@Override
		public String toString() {
			return Integer.toString(tag);
		}
	}

	public static class SameFrame extends StackMapFrame {
		SameFrame(int tag) {
			super(tag);
		}

		@Override
		public int type() {
			return SAME;
		}
	}

	public static class SameLocals1StackItemFrame extends StackMapFrame {
		public final VerificationTypeInfo stack;

		SameLocals1StackItemFrame(int tag, VerificationTypeInfo stack) {
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
	}

	public static class SameLocals1StackItemFrameExtended extends StackMapFrame {
		public final int					delta;
		public final VerificationTypeInfo	stack;

		SameLocals1StackItemFrameExtended(int tag, int delta, VerificationTypeInfo stack) {
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
	}

	public static class ChopFrame extends StackMapFrame {
		public final int delta;

		ChopFrame(int tag, int delta) {
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
	}

	public static class SameFrameExtended extends StackMapFrame {
		public final int delta;

		SameFrameExtended(int tag, int delta) {
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
	}

	public static class AppendFrame extends StackMapFrame {
		public final int					delta;
		public final VerificationTypeInfo[]	locals;

		AppendFrame(int tag, int delta, VerificationTypeInfo[] locals) {
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
	}

	public static class FullFrame extends StackMapFrame {
		public final int					delta;
		public final VerificationTypeInfo[]	locals;
		public final VerificationTypeInfo[]	stack;

		FullFrame(int tag, int delta, VerificationTypeInfo[] locals, VerificationTypeInfo[] stack) {
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

		VerificationTypeInfo(int tag) {
			this.tag = tag;
		}

		@Override
		public String toString() {
			return Integer.toString(tag);
		}
	}

	public static class ObjectVariableInfo extends VerificationTypeInfo {
		public final String type;

		ObjectVariableInfo(int tag, String type) {
			super(tag);
			this.type = type;
		}

		@Override
		public String toString() {
			return tag + ":" + type;
		}
	}

	public static class UninitializedVariableInfo extends VerificationTypeInfo {
		public final int offset;

		UninitializedVariableInfo(int tag, int offset) {
			super(tag);
			this.offset = offset;
		}

		@Override
		public String toString() {
			return tag + ":" + offset;
		}
	}

	static VerificationTypeInfo parseVerificationTypeInfo(DataInput in, ConstantPool constant_pool) throws IOException {
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
}
