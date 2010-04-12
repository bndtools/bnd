package aQute.service.scripting;

import java.io.*;
import java.util.*;

public interface Scripter {
    String MIME_TYPE = "mime.type";
    
    Object eval(Map<String,Object> context, Reader reader ) throws Exception;
}
