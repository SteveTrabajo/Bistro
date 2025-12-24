package logic;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BarcodeService {
    
    public int generateCode() {
        // מייצר מספר רנדומלי בין 10,000 ל-99,999
        return new Random().nextInt(90000) + 10000;
    }

    public void saveImage(int code, String name) {
        try {
            String textToEncode = String.valueOf(code);
            int width = 300;
            int height = 300; // QR Code הוא בדרך כלל ריבועי

            // הגדרות נוספות (אופציונלי): תמיכה בעברית או שוליים
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            // יצירת ה-QR Code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix matrix = qrCodeWriter.encode(textToEncode, BarcodeFormat.QR_CODE, width, height, hints);

            // שמירת התמונה בתיקיית הפרויקט
            Path path = FileSystems.getDefault().getPath("./" + name + "_QR.png");
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            
            System.out.println("✅ QR Code נוצר בהצלחה: " + name + "_QR.png");
            
        } catch (Exception e) {
            System.err.println("שגיאה ביצירת ה-QR: " + e.getMessage());
        }
    }
}