package entities;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int itemId;
    private String name;
    private double price;
    private int quantity;

    public Item(int itemId, String name, double price, int quantity) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    public String getName() { 
    	return name;
    }
    
    public double getPrice() { 
    	return price; 
    }
    
    public int getQuantity() { 
    	return quantity; 
    }
    
    public double getTotal() { 
    	return price * quantity; 
    }
    public int getItemId() {
		return itemId;
	}
    
}
