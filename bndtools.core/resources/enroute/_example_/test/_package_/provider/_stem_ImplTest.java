package _package_.provider;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;


/*
 * Example JUNit test case
 * 
 */

public class _stem_ImplTest {

	
	/*
	 * Example test method
	 * 
	 */
	
	@Test
	public void simple() {
		_stem_Impl _stem_ = new _stem_Impl();
		Map<String,Object> map = new HashMap<>();
		map.put("name", "Test");
		_stem_.activate(map);
		
		_stem_.say("sanctum sanctorum");
	}

	
}
