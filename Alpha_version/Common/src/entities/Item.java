package entities;

import java.io.Serializable;

/*
 * Represents an item with its details.
 */
public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int itemId;
    private String name;
    private double price;
    private int quantity;

    /*
	 * Creates an Item instance.
	 * @param itemId    the item ID
	 * @param name      the name of the item
	 * @param price     the price of the item
	 * @param quantity  the quantity of the item
	 */
    public Item(int itemId, String name, double price, int quantity) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    /*
     * Gets the name of the item.
     * @return the name
     */
    public String getName() { 
    	return name;
    }
    
    /*
	 * Gets the price of the item.
	 * @return the price
	 */
    public double getPrice() { 
    	return price; 
    }
    
    /*
     * Gets the quantity of the item.
     * @return the quantity
     */
    public int getQuantity() { 
    	return quantity; 
    }
    
    /*
	 * Calculates the total price for the item based on its price and quantity.
	 * @return the total price
	 */
    public double getTotal() { 
    	return price * quantity; 
    }
    
    /*
     * Gets the item ID.
     * @return the item ID
     */
    public int getItemId() {
		return itemId;
	}
    
}
// end of Item.java