package aQute.bnd.classfile.writer;

import java.io.DataOutput;
import java.io.IOException;

public interface DataOutputWriter {
	public void write(DataOutput out) throws IOException;
}
