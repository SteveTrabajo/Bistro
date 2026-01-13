package dto;

import java.io.Serializable;
import java.time.LocalDate;

public class Holiday implements Serializable {
    private LocalDate date;
    private String name;
    private boolean isClosed;

    public Holiday(LocalDate date, String name, boolean isClosed) {
        this.date = date;
        this.name = name;
        this.isClosed = isClosed;
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
    
    @Override
    public String toString() {
    	// if the holiday is closed, add (Closed) to the string, else just return date and name
        return date + ": " + name + (isClosed ? " (Closed)" : "");
    }
}