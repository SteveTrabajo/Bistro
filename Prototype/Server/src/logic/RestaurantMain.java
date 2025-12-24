package logic;

import java.util.Scanner;

public class RestaurantMain {
    public static void main(String[] args) {
        BarcodeService service = new BarcodeService();
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter Customer Name:");
        String name = sc.nextLine();
        
        int code = service.generateCode();
        ReservationEntity res = new ReservationEntity(name, code);
        service.saveImage(code, name);

        System.out.println("Customer " + name + " got barcode: " + code);
        
        System.out.println("\n--- SIMULATING SCAN ---");
        System.out.println("Enter barcode to verify:");
        int input = sc.nextInt();

        if (input == res.getBarcode()) {
            System.out.println("SUCCESS: Welcome " + res.getName());
        } else {
            System.out.println("ERROR: Invalid Barcode!");
        }
        sc.close();
    }
}