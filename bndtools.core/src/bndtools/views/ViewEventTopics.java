package bndtools.views;

/**
 * Topics for the EventBroker which is used for communication between different
 * views. For example if View1 sends an event to View2 wants to open a dialog in
 * the other view. .
 */
public enum ViewEventTopics {

	/**
	 * Event to open the advances search of the repositories view.
	 */
	REPOSITORIESVIEW_OPEN_ADVANCED_SEARCH("EVENT/RepositoriesView/openAdvancedSearch");

	private String eventtype;

	ViewEventTopics(String eventtype) {
		this.eventtype = eventtype;
	}

	public String topic() {
		return eventtype;
	}

	@Override
	public String toString() {
		return eventtype;
	}

}
