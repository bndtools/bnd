package doubler.impl;

import doubler.Doubler;

public class DoublerImpl implements Doubler {
    public int doubleIt(int toDouble) {
        return toDouble * 2;
    }
    public int tripleIt(int toTriple) {
    	return toTriple * 3;
    }
}
