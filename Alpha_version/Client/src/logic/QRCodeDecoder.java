package logic;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public final class QRCodeDecoder {

    private QRCodeDecoder() {}

    /**
     * Decodes the QR code content from an image file.
     *
     * @param file PNG/JPG image containing a QR code
     * @return decoded string (trimmed)
     * @throws IllegalArgumentException if file is null or unreadable
     * @throws com.google.zxing.NotFoundException if no QR code is found in the image
     */
    public static String decodeFromFile(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }

        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalArgumentException("Unsupported or corrupted image");
        }

        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new MultiFormatReader().decode(bitmap);
        return result == null ? null : result.getText().trim();
    }
}
