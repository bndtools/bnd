package aQute.p2.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import aQute.bnd.util.dto.DTO;

public class P2Index extends DTO {
	public long			modified;
	public List<URI>	content		= new ArrayList<>();
	public List<URI>	artifacts	= new ArrayList<>();
}
