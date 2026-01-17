package dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class Holiday implements Serializable {
	private static final long serialVersionUID = 1L;
	private LocalDate date;
    private String name;
    private boolean isClosed;
    private LocalTime openTime;
    private LocalTime closeTime;

    /*
     * Creates a Holiday instance.
     * @param date      the date of the holiday
     * @param name      the name of the holiday
     * @param isClosed  whether the business is closed on this holiday
     * @param openTime  the opening time (null if closed)
     * @param closeTime the closing time (null if closed)
     */
    public Holiday(LocalDate date, String name, boolean isClosed, LocalTime openTime, LocalTime closeTime) {
        this.date = date;
        this.name = name;
        this.isClosed = isClosed;
        // If closed => times must be null
        if (isClosed) {
            this.openTime = null;
            this.closeTime = null;
        } else {
            this.openTime = openTime;
            this.closeTime = closeTime;
        }
    }

    /*
     * Gets the date of the holiday.
     * @return the date
     */
    public LocalDate getDate() { 
    	return date; 
	}
    
    /*
	 * Gets the name of the holiday.
	 * @return the name
	 */
    public String getName() { 
    	return name; 
	}
    
    /*
     * Checks if the business is closed on this holiday.
     * @return true if closed, false otherwise
     */
    public boolean isClosed() { 
    	return isClosed; 
	}
    
    /*
	 * Gets the opening time.
	 * @return the opening time (null if closed)
	 */
    public LocalTime getOpenTime() {
        return openTime;
    }

    /*
     * Gets the closing time.
     * @return the closing time (null if closed)
     */
    public LocalTime getCloseTime() {
        return closeTime;
    }
    
    /*
	 * @return a string representation of the Holiday
	 */
    @Override
    public String toString() {
        if (isClosed) {
            return date + ": " + name + " (Closed)";
        }
        if (openTime != null && closeTime != null) {
            String o = openTime.toString().substring(0, 5);
            String c = closeTime.toString().substring(0, 5);
            return date + ": " + name + " (" + o + "-" + c + ")";
        }
        return date + ": " + name;
    }
}
// End of Holiday.java