package aQute.bnd.service.buildevents;

import aQute.bnd.osgi.Builder;

public interface PostBuildPlugin {

	void postBuild(Builder builder) throws Exception;

}
