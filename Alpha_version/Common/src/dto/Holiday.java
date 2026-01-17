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

    public LocalDate getDate() { 
    	return date; 
	}
    
    public String getName() { 
    	return name; 
	}
    
    public boolean isClosed() { 
    	return isClosed; 
	}
    public LocalTime getOpenTime() {
        return openTime;
    }

    // NEW
    public LocalTime getCloseTime() {
        return closeTime;
    }
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