package gui.controllers;

import com.google.zxing.*;
import com.google.zxing.client.j2se. BufferedImageLuminanceSource;
import com.google.zxing.client.j2se. MatrixToImageWriter;
import com.google.zxing. common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class BarcodeController {
    
    /**
     * Reads a QR code file and decodes it to a Strings
     */
    public static String decodeQRCode(File qrCodeFile) throws IOException, NotFoundException {
        BufferedImage bufferedImage = ImageIO. read(qrCodeFile);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }
    
    /**
     * Creates a QR code file from a String
     */
    public static void encodeQRCode(String text, String filePath, int width, int height) 
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
}