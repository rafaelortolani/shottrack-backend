package com.shottrack.backend.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** ADR-0026: imagens de teste geradas em memória (JPEG com EXIF/GPS, PNG, GIF, "WebP", cabeçalho de PNG gigante). */
public final class TestImages {

    public static final String SECRET_GPS = "GPS-LATITUDE-23.5505S-LONGITUDE-46.6333W";

    private TestImages() {
    }

    /** Canto superior esquerdo vermelho, resto azul — dá para saber se a orientação foi corrigida. */
    public static BufferedImage marked(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.RED);
        g.fillRect(0, 0, width / 3, height / 3);
        g.dispose();
        return image;
    }

    public static byte[] jpeg(int width, int height) {
        return write(marked(width, height), "jpg");
    }

    public static byte[] png(int width, int height) {
        return write(marked(width, height), "png");
    }

    public static byte[] gif(int width, int height) {
        return write(marked(width, height), "gif");
    }

    /** JPEG com orientação EXIF e um segmento extra com "coordenadas" (para provar que são removidos). */
    public static byte[] jpegWithExif(int width, int height, int orientation) {
        byte[] plain = jpeg(width, height);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(plain[0]);
        out.write(plain[1]); // SOI
        byte[] exif = exifSegment(orientation);
        out.writeBytes(exif);
        byte[] gps = (SECRET_GPS).getBytes(StandardCharsets.US_ASCII);
        out.write(0xFF);
        out.write(0xFE); // comentário
        out.write(((gps.length + 2) >> 8) & 0xFF);
        out.write((gps.length + 2) & 0xFF);
        out.writeBytes(gps);
        out.write(plain, 2, plain.length - 2);
        return out.toByteArray();
    }

    private static byte[] exifSegment(int orientation) {
        // TIFF big-endian: cabeçalho + IFD0 com a tag 0x0112 (orientação) e uma tag de GPS fictícia
        byte[] tiff = {
                'M', 'M', 0, 0x2A, 0, 0, 0, 8,
                0, 2,
                0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                (byte) 0x88, 0x25, 0, 4, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0
        };
        byte[] header = {'E', 'x', 'i', 'f', 0, 0};
        int length = 2 + header.length + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xE1);
        out.write((length >> 8) & 0xFF);
        out.write(length & 0xFF);
        out.writeBytes(header);
        out.writeBytes(tiff);
        return out.toByteArray();
    }

    /** Cabeçalho de WebP (RIFF....WEBP) com lixo depois — basta para a assinatura. */
    public static byte[] fakeWebp() {
        byte[] body = "RIFF\u0010\u0000\u0000\u0000WEBPVP8 0123456789abcdef".getBytes(StandardCharsets.ISO_8859_1);
        return body;
    }

    /** PNG válido só até o cabeçalho IHDR com as dimensões pedidas (sem dados): lê-se o tamanho sem decodificar. */
    public static byte[] pngHeaderOnly(int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        byte[] ihdr = new byte[13];
        putInt(ihdr, 0, width);
        putInt(ihdr, 4, height);
        ihdr[8] = 8;  // profundidade
        ihdr[9] = 2;  // RGB
        chunk(out, "IHDR", ihdr);
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        byte[] length = new byte[4];
        putInt(length, 0, data.length);
        out.writeBytes(length);
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        byte[] crcBytes = new byte[4];
        putInt(crcBytes, 0, (int) crc.getValue());
        out.writeBytes(crcBytes);
    }

    private static void putInt(byte[] target, int at, int value) {
        target[at] = (byte) (value >>> 24);
        target[at + 1] = (byte) (value >>> 16);
        target[at + 2] = (byte) (value >>> 8);
        target[at + 3] = (byte) value;
    }

    private static byte[] write(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static BufferedImage read(byte[] bytes) {
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Lista os marcadores APP/COM presentes antes dos dados da imagem (E1 = EXIF/XMP, FE = comentário, ED = IPTC). */
    public static java.util.List<Integer> metadataMarkers(byte[] jpeg) {
        java.util.List<Integer> markers = new java.util.ArrayList<>();
        int pos = 2;
        while (pos + 4 <= jpeg.length && (jpeg[pos] & 0xFF) == 0xFF) {
            int marker = jpeg[pos + 1] & 0xFF;
            if (marker == 0xDA) {
                break;
            }
            if (marker != 0xE0 && (marker == 0xFE || (marker >= 0xE1 && marker <= 0xEF))) {
                markers.add(marker);
            }
            pos += 2 + (((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF));
        }
        return markers;
    }

    public static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }
}
