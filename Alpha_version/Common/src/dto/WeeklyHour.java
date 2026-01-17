package dto;

import java.io.Serializable;
import java.time.LocalTime;

public class WeeklyHour implements Serializable {
    private int dayOfWeek; // 1=Sun,2=Mon,...,7=Sat
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean isActive;

    /*
	 * Creates a WeeklyHour instance.
	 * @param dayOfWeek  the day of the week (1=Sun,2=Mon,...,7=Sat)
	 * @param openTime   the opening time
	 * @param closeTime  the closing time
	 */
    public WeeklyHour(int dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isActive = true;
    }
    
    /*
	 * Checks if the weekly hour is active.
	 * @return true if active, false otherwise
	 */
    public int getDayOfWeek() { 
    	return dayOfWeek; 
	}
    
    /*
     * Gets the opening time.
     * @return the opening time
     */
    public LocalTime getOpenTime() { 
    	return openTime; 
	}
    
    /*
	 * Gets the closing time.
	 * @return the closing time
	 */
    public LocalTime getCloseTime() { 
    	return closeTime; 
	}
    
}
// end of WeeklyHour.java