package logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QRCodeGenerator is responsible for generating QR code images for member codes.
 */
public class QRCodeGenerator {
	
	/********************************* Instance Variables *********************************/
	
    private static final String QR_FOLDER = "member_qr_codes";
    private static final int QR_SIZE = 300;
    
    /************************************* Methods ***************************************/
    /**
     * Generates and saves a QR code image for the given member code.
     *
     * @param memberCode the unique member code
     * @return the file path if successful, null otherwise
     */
    public static Path generateAndSaveQRCode(String memberCode, String fullName) {
        try {
            Path folderPath = Paths.get(QR_FOLDER);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            // Generate QR code
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(memberCode, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            // Save QR code image
            Path filePath = folderPath.resolve("member_" + memberCode + "_"+fullName+".png");
            MatrixToImageWriter.writeToPath(matrix, "PNG", filePath);
            // Return the file path
            return filePath;
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
