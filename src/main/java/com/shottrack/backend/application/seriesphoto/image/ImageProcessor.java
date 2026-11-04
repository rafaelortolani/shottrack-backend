package com.shottrack.backend.application.seriesphoto.image;

import com.shottrack.backend.application.seriesphoto.PhotoProperties;
import com.shottrack.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ADR-0026 / UC60: o arquivo enviado nunca é guardado. O tipo vem da assinatura do conteúdo (não do nome nem do
 * Content-Type); as dimensões são lidas do cabeçalho ANTES de decodificar (bomba de descompressão); a imagem é
 * decodificada já reduzida quando for muito maior que o necessário, tem a orientação EXIF aplicada e é recodificada
 * em JPEG — o que descarta todos os metadados (inclusive GPS) e qualquer carga "poliglota". No máximo
 * `app.photos.max-concurrent-processing` imagens são processadas ao mesmo tempo, para proteger a memória.
 * Só JPEG e PNG: o JDK não decodifica WebP (o navegador converte para JPEG antes de enviar, ADR-0026).
 */
@Component
public class ImageProcessor {

    static final int MAX_SIDE = 2000;
    static final int THUMBNAIL_SIDE = 400;
    private static final float JPEG_QUALITY = 0.85f;
    private static final long WAIT_FOR_SLOT_SECONDS = 30;

    private final long maxPixels;
    private final Semaphore slots;

    public ImageProcessor(PhotoProperties properties) {
        this.maxPixels = properties.maxMegapixels() * 1_000_000L;
        this.slots = new Semaphore(properties.maxConcurrentProcessing(), true);
    }

    public ProcessedImage process(byte[] data) {
        String format = detectFormat(data);
        boolean acquired = acquireSlot();
        try {
            return decodeAndEncode(data, format);
        } finally {
            if (acquired) {
                slots.release();
            }
        }
    }

    private boolean acquireSlot() {
        try {
            if (!slots.tryAcquire(WAIT_FOR_SLOT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException("PHOTO_PROCESSING_BUSY", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("PHOTO_PROCESSING_BUSY", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** Assinatura do conteúdo: JPEG (FF D8 FF) e PNG (89 PNG). WebP e o resto → 415. */
    private static String detectFormat(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "png";
        }
        throw new BusinessException("PHOTO_UNSUPPORTED_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    private ProcessedImage decodeAndEncode(byte[] data, String format) {
        if ("jpeg".equals(format) && !jpegIsComplete(data)) {
            // o decodificador do JDK devolve uma imagem parcial para JPEG truncado, sem erro
            throw new BusinessException("PHOTO_INVALID_IMAGE", HttpStatus.BAD_REQUEST);
        }
        BufferedImage decoded;
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
            if (!readers.hasNext()) {
                throw new BusinessException("PHOTO_UNSUPPORTED_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new BusinessException("PHOTO_INVALID_IMAGE", HttpStatus.BAD_REQUEST);
                }
                if ((long) width * height > maxPixels) {
                    throw new BusinessException("PHOTO_DIMENSIONS_TOO_LARGE", HttpStatus.BAD_REQUEST);
                }
                ImageReadParam param = reader.getDefaultReadParam();
                // imagem bem maior que o necessário: decodifica já reduzida (menos memória e tempo)
                int step = Math.max(1, Math.max(width, height) / MAX_SIDE);
                param.setSourceSubsampling(step, step, 0, 0);
                decoded = reader.read(0, param);
            } finally {
                reader.dispose();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // arquivo truncado/corrompido ou modelo de cor sem suporte (ex.: CMYK); sem a mensagem: pode citar o conteúdo
            throw new BusinessException("PHOTO_INVALID_IMAGE", HttpStatus.BAD_REQUEST);
        }

        int orientation = "jpeg".equals(format) ? ExifOrientation.read(data) : 1;
        BufferedImage oriented = toUprightRgb(decoded, orientation);
        decoded.flush();
        BufferedImage main = scaleDown(oriented, MAX_SIDE);
        BufferedImage thumb = scaleDown(main, THUMBNAIL_SIDE);
        try {
            return new ProcessedImage(encodeJpeg(main), main.getWidth(), main.getHeight(), encodeJpeg(thumb));
        } catch (IOException e) {
            throw new BusinessException("PHOTO_INVALID_IMAGE", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * JPEG completo = tem o marcador de fim (FF D9) depois do início dos dados (FF DA). Antes do FF DA o FF D9 pode
     * aparecer na miniatura embutida no EXIF; nos dados da imagem o byte FF é sempre escapado, então não há falso positivo.
     */
    private static boolean jpegIsComplete(byte[] d) {
        int scanStart = -1;
        for (int i = 2; i + 1 < d.length; i++) {
            if ((d[i] & 0xFF) == 0xFF && (d[i + 1] & 0xFF) == 0xDA) {
                scanStart = i;
                break;
            }
            if ((d[i] & 0xFF) == 0xFF && d[i + 1] != 0 && (d[i + 1] & 0xFF) != 0xFF) {
                int marker = d[i + 1] & 0xFF;
                if (marker >= 0xD0 && marker <= 0xD8 || marker == 0x01) {
                    i++;
                    continue;
                }
                if (i + 3 >= d.length) {
                    return false;
                }
                i += 1 + (((d[i + 2] & 0xFF) << 8) | (d[i + 3] & 0xFF));
            }
        }
        if (scanStart < 0) {
            return false;
        }
        for (int i = d.length - 2; i > scanStart; i--) {
            if ((d[i] & 0xFF) == 0xFF && (d[i + 1] & 0xFF) == 0xD9) {
                return true;
            }
        }
        return false;
    }

    /** Desenha em RGB (fundo branco no lugar da transparência) já aplicando a orientação EXIF. */
    private static BufferedImage toUprightRgb(BufferedImage source, int orientation) {
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swap = orientation >= 5;
        BufferedImage target = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, target.getWidth(), target.getHeight());
            g.drawImage(source, transformFor(orientation, w, h), null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static AffineTransform transformFor(int orientation, int w, int h) {
        return switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();
        };
    }

    /** Reduz para que o lado maior fique em `maxSide` (nunca amplia), em passos de metade para não serrilhar. */
    private static BufferedImage scaleDown(BufferedImage source, int maxSide) {
        int longest = Math.max(source.getWidth(), source.getHeight());
        if (longest <= maxSide) {
            return source;
        }
        double ratio = (double) maxSide / longest;
        int targetW = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int targetH = Math.max(1, (int) Math.round(source.getHeight() * ratio));
        BufferedImage current = source;
        while (current.getWidth() >= targetW * 2 && current.getHeight() >= targetH * 2) {
            current = resize(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        return resize(current, targetW, targetH);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /** Sem metadados: o writer recebe a imagem sem IIOMetadata e só grava o cabeçalho JFIF mínimo. */
    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
