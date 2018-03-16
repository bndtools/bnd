package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * This element describes all of the mailing lists associated with a project.
 * The auto-generated site references this information.
 */
public class MailingListDTO extends DTO {
	/**
	 * The name of the mailing list.
	 */

	public String	name;

	/**
	 * The email address or link that can be used to subscribe to the mailing
	 * list. If this is an email address, a <code>mailto:</code> link will
	 * automatically be created when the documentation is created.
	 */
	public String	subscribe;

	/**
	 * The email address or link that can be used to unsubscribe to the mailing
	 * list. If this is an email address, a <code>mailto:</code> link will
	 * automatically be created when the documentation is created.
	 */

	public String	unsubscribe;

	/**
	 * The email address or link that can be used to post to the mailing list.
	 * If this is an email address, a <code>mailto:</code> link will
	 * automatically be created when the documentation is created.
	 */

	public String	post;

	/**
	 * The link to a URL where you can browse the mailing list archive.
	 */
	public String	archive;

	/**
	 * The link to alternate URLs where you can browse the list archive.
	 */

	public String[]	otherArchives;
}
