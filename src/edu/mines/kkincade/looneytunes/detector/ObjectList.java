package edu.mines.kkincade.looneytunes.detector;

public class ObjectList {
	private String title;
	private String message;
	private int icon;
	private String address;
	
	public ObjectList(int icon, String title, String message, String address){
		this.title = title;
		this.message = message;
		this.icon = icon;
		this.address = address;
	}
	
	/** -------------------------- Getters and Setters ------------------------- **/
	
	public int getIcon() { return icon; }
	public String getTitle() { return title; }
	public String getMsg() { return message; }
	public String getAdress() { return address; }
	public void setTitle(String title) { this.title = title; }
	public void setMsg(String message) { this.message = message; }
	
}
