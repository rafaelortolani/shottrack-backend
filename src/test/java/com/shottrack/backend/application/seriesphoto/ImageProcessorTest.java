package com.shottrack.backend.application.seriesphoto;

import com.shottrack.backend.application.seriesphoto.image.ImageProcessor;
import com.shottrack.backend.application.seriesphoto.image.ProcessedImage;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.support.TestImages;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC60 (ADR-0026): o processamento da imagem — tipo pelo conteúdo, megapixels antes de decodificar, EXIF, metadados. */
class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor(new PhotoProperties(10_485_760, 40, 2, Duration.ofMinutes(60)));

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }

    @Test
    void shouldReencodeJpegAndPngAsJpegWithAThumbnail() {
        for (byte[] original : new byte[][]{TestImages.jpeg(300, 200), TestImages.png(300, 200)}) {
            ProcessedImage result = processor.process(original);

            assertThat(result.image()[0] & 0xFF).isEqualTo(0xFF);
            assertThat(result.image()[1] & 0xFF).isEqualTo(0xD8);
            assertThat(result.widthPx()).isEqualTo(300);
            assertThat(result.heightPx()).isEqualTo(200);
            assertThat(TestImages.read(result.thumbnail()).getWidth()).isEqualTo(300); // menor que 400: não amplia
        }
    }

    @Test
    void shouldShrinkTheLongestSideTo2000AndTheThumbnailTo400() {
        ProcessedImage result = processor.process(TestImages.jpeg(3000, 1500));

        assertThat(result.widthPx()).isEqualTo(2000);
        assertThat(result.heightPx()).isEqualTo(1000);
        BufferedImage thumb = TestImages.read(result.thumbnail());
        assertThat(thumb.getWidth()).isEqualTo(400);
        assertThat(thumb.getHeight()).isEqualTo(200);
    }

    @Test
    void shouldRemoveAllMetadataIncludingExifAndGps() {
        byte[] original = TestImages.jpegWithExif(200, 100, 1);
        assertThat(TestImages.contains(original, TestImages.SECRET_GPS)).isTrue();
        assertThat(TestImages.metadataMarkers(original)).isNotEmpty();

        ProcessedImage result = processor.process(original);

        assertThat(TestImages.contains(result.image(), TestImages.SECRET_GPS)).isFalse();
        assertThat(TestImages.contains(result.image(), "Exif")).isFalse();
        assertThat(TestImages.metadataMarkers(result.image())).isEmpty();
        assertThat(TestImages.metadataMarkers(result.thumbnail())).isEmpty();
        assertThat(TestImages.contains(result.thumbnail(), TestImages.SECRET_GPS)).isFalse();
    }

    @Test
    void shouldApplyTheExifOrientationSoTheStoredImageIsUpright() {
        // original 60x40 com o canto superior esquerdo vermelho; orientação 6 = girar 90° horário ao exibir
        ProcessedImage rotated = processor.process(TestImages.jpegWithExif(60, 40, 6));
        BufferedImage image = TestImages.read(rotated.image());
        assertThat(image.getWidth()).isEqualTo(40);
        assertThat(image.getHeight()).isEqualTo(60);
        assertThat(isRed(image.getRGB(38, 2))).as("vermelho no canto superior direito").isTrue();
        assertThat(isRed(image.getRGB(2, 2))).as("canto superior esquerdo já não é vermelho").isFalse();

        // 3 = 180°: o vermelho vai para o canto inferior direito, dimensões iguais
        BufferedImage upsideDown = TestImages.read(processor.process(TestImages.jpegWithExif(60, 40, 3)).image());
        assertThat(upsideDown.getWidth()).isEqualTo(60);
        assertThat(isRed(upsideDown.getRGB(57, 37))).isTrue();

        // 8 = girar 90° anti-horário: vermelho no canto inferior esquerdo
        BufferedImage counter = TestImages.read(processor.process(TestImages.jpegWithExif(60, 40, 8)).image());
        assertThat(counter.getHeight()).isEqualTo(60);
        assertThat(isRed(counter.getRGB(2, 57))).isTrue();

        // 2 = espelho horizontal: vermelho no canto superior direito, dimensões iguais
        BufferedImage mirrored = TestImages.read(processor.process(TestImages.jpegWithExif(60, 40, 2)).image());
        assertThat(mirrored.getWidth()).isEqualTo(60);
        assertThat(isRed(mirrored.getRGB(57, 2))).isTrue();
    }

    private static boolean isRed(int rgb) {
        Color color = new Color(rgb);
        return color.getRed() > 150 && color.getBlue() < 110;
    }

    @Test
    void shouldIgnoreAnUnreadableExifBlockInsteadOfFailing() {
        byte[] jpeg = TestImages.jpeg(100, 80);
        byte[] broken = new byte[jpeg.length + 12];
        System.arraycopy(jpeg, 0, broken, 0, 2);
        byte[] bad = {(byte) 0xFF, (byte) 0xE1, 0, 10, 'E', 'x', 'i', 'f', 0, 0, 'M', 'M'};
        System.arraycopy(bad, 0, broken, 2, bad.length);
        System.arraycopy(jpeg, 2, broken, 2 + bad.length, jpeg.length - 2);

        assertThat(processor.process(broken).widthPx()).isEqualTo(100);
    }

    @Test
    void shouldFlattenTransparencyOnWhiteWhenConvertingPng() {
        BufferedImage transparent = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(transparent, "png", out);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }

        Color pixel = new Color(TestImages.read(processor.process(out.toByteArray()).image()).getRGB(20, 20));

        assertThat(pixel.getRed()).isGreaterThan(240);
        assertThat(pixel.getGreen()).isGreaterThan(240);
        assertThat(pixel.getBlue()).isGreaterThan(240);
    }

    @Test
    void shouldRejectTypesOtherThanJpegAndPngByContent() {
        assertCode(() -> processor.process(TestImages.gif(20, 20)), "PHOTO_UNSUPPORTED_TYPE");
        assertCode(() -> processor.process(TestImages.fakeWebp()), "PHOTO_UNSUPPORTED_TYPE");
        assertCode(() -> processor.process("%PDF-1.4 conteúdo".getBytes(StandardCharsets.UTF_8)), "PHOTO_UNSUPPORTED_TYPE");
        assertCode(() -> processor.process("<?php echo 1; ?>".getBytes(StandardCharsets.UTF_8)), "PHOTO_UNSUPPORTED_TYPE");
        assertCode(() -> processor.process(new byte[]{1}), "PHOTO_UNSUPPORTED_TYPE");
    }

    @Test
    void shouldRejectCorruptedImages() {
        byte[] jpeg = TestImages.jpeg(400, 300);
        assertCode(() -> processor.process(Arrays.copyOf(jpeg, jpeg.length / 3)), "PHOTO_INVALID_IMAGE");
        assertCode(() -> processor.process(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0}), "PHOTO_INVALID_IMAGE");
        byte[] png = TestImages.png(400, 300);
        assertCode(() -> processor.process(Arrays.copyOf(png, png.length / 2)), "PHOTO_INVALID_IMAGE");
    }

    @Test
    void shouldRejectMoreThan40MegapixelsFromTheHeaderWithoutDecoding() {
        // 60000x60000 = 3,6 bilhões de pixels: se fosse decodificado, estouraria a memória
        assertCode(() -> processor.process(TestImages.pngHeaderOnly(60_000, 60_000)), "PHOTO_DIMENSIONS_TOO_LARGE");
        // 40 MP exatos passam do cabeçalho (falham depois por não haver dados) — o limite é "mais de 40"
        assertCode(() -> processor.process(TestImages.pngHeaderOnly(8_001, 5_000)), "PHOTO_DIMENSIONS_TOO_LARGE");
        assertCode(() -> processor.process(TestImages.pngHeaderOnly(8_000, 5_000)), "PHOTO_INVALID_IMAGE");
    }

    @Test
    void shouldQueueExcessConcurrentRequestsInsteadOfFailingThem() throws Exception {
        // 2 vagas e 6 chamadas ao mesmo tempo: o semáforo serializa o excedente e todas terminam bem
        byte[] image = TestImages.jpeg(2400, 1800);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        AtomicInteger succeeded = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(6);
        for (int i = 0; i < 6; i++) {
            pool.submit(() -> {
                try {
                    processor.process(image);
                    succeeded.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        done.await();
        pool.shutdownNow();
        assertThat(succeeded.get()).isEqualTo(6);
    }
}
