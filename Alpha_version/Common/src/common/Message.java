package common;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 6724999812338990274L;
	private String id;
	public Object data;
	
	/**
	 * Default constructor
	 * 
	 * @param id
	 * @param data
	 */
	 
	public Message(String id, Object data)
	{
		this.id = id;
		this.data = data;
	}
	
	public String getId()
	{
		return id;
	}
	
	public Object getData()
	{
		return data;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setData(Object data)
	{
		this.data = data;
	}
}
