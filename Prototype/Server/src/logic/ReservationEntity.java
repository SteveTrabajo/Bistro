package logic;

public class ReservationEntity {
    private String name;
    private int barcode;
    private boolean used = false;

    public ReservationEntity(String name, int barcode) {
        this.name = name;
        this.barcode = barcode;
    }
    public int getBarcode() { return barcode; }
    public String getName() { return name; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}