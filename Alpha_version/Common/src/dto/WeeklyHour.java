package dto;

import java.io.Serializable;
import java.time.LocalTime;

public class WeeklyHour implements Serializable {
    private int dayOfWeek; // 1=Sun,2=Mon,...,7=Sat
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean isActive;

    public WeeklyHour(int dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isActive = true;
    }
    
    // Getters
    public int getDayOfWeek() { 
    	return dayOfWeek; 
	}
    public LocalTime getOpenTime() { 
    	return openTime; 
	}
    
    public LocalTime getCloseTime() { 
    	return closeTime; 
	}
    
}