package com.duoshouji.server.service.note;

public class NoteFilter {

	private Tag tag;
	
	public NoteFilter() {
		super();
	}
	
	public void setTag(Tag tag) {
		this.tag = tag;
	}
	
	public Tag getTag() {
		return tag;
	}
	
	public boolean isTagSet() {
		return tag != null;
	}
}