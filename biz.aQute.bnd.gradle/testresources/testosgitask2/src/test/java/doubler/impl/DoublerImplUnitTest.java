package doubler.impl;

import doubler.Doubler;
import org.junit.Test;
import static org.junit.Assert.*;

public class DoublerImplUnitTest {
    @Test
    public void testIt() {
        Doubler doubler = new DoublerImpl();
        assertEquals(4, doubler.doubleIt(2));
    }
}
