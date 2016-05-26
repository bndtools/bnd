package aQute.bnd.repository.p2.provider;

import java.net.URI;

public interface P2Config {
	String name(String string);

	URI url();

	String location();

	String location(String string);

}
