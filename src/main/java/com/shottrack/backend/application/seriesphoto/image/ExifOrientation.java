package com.shottrack.backend.application.seriesphoto.image;

/**
 * Lê só a orientação EXIF (tag 0x0112) de um JPEG, sem dependência externa. Qualquer arquivo malformado ou sem a
 * tag devolve 1 (sem rotação) — a imagem é recodificada de qualquer jeito, então um EXIF estranho não quebra nada.
 */
final class ExifOrientation {

    private ExifOrientation() {
    }

    static int read(byte[] jpeg) {
        try {
            return find(jpeg);
        } catch (RuntimeException e) {
            return 1;
        }
    }

    private static int find(byte[] b) {
        int pos = 2; // depois do SOI (FFD8)
        while (pos + 4 <= b.length) {
            if ((b[pos] & 0xFF) != 0xFF) {
                return 1;
            }
            int marker = b[pos + 1] & 0xFF;
            if (marker == 0xFF) {
                pos++;
                continue;
            }
            if (marker == 0xDA || marker == 0xD9) { // início dos dados / fim: acabaram os cabeçalhos
                return 1;
            }
            int length = u16(b, pos + 2, false);
            if (length < 2) {
                return 1;
            }
            if (marker == 0xE1 && length >= 16 && isExifHeader(b, pos + 4)) {
                return orientationFromTiff(b, pos + 10, Math.min(b.length, pos + 2 + length));
            }
            pos += 2 + length;
        }
        return 1;
    }

    private static boolean isExifHeader(byte[] b, int at) {
        return b[at] == 'E' && b[at + 1] == 'x' && b[at + 2] == 'i' && b[at + 3] == 'f' && b[at + 4] == 0 && b[at + 5] == 0;
    }

    private static int orientationFromTiff(byte[] b, int tiff, int end) {
        boolean little = b[tiff] == 'I' && b[tiff + 1] == 'I';
        if (!little && !(b[tiff] == 'M' && b[tiff + 1] == 'M')) {
            return 1;
        }
        long ifd = tiff + u32(b, tiff + 4, little);
        if (ifd < tiff || ifd + 2 > end) {
            return 1;
        }
        int entries = u16(b, (int) ifd, little);
        for (int i = 0; i < entries; i++) {
            int entry = (int) ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (u16(b, entry, little) == 0x0112) {
                int value = u16(b, entry + 8, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static int u16(byte[] b, int at, boolean little) {
        int first = b[at] & 0xFF;
        int second = b[at + 1] & 0xFF;
        return little ? (second << 8) | first : (first << 8) | second;
    }

    private static long u32(byte[] b, int at, boolean little) {
        long first = u16(b, at, little);
        long second = u16(b, at + 2, little);
        return little ? (second << 16) | first : (first << 16) | second;
    }
}
