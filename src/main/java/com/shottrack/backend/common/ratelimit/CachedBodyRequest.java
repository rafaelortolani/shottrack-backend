package com.shottrack.backend.common.ratelimit;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * O filtro de rate limit precisa ler o e-mail do corpo, mas o controller
 * ainda precisa receber o corpo inteiro: este wrapper devolve primeiro os
 * bytes já lidos e depois o resto do stream original.
 */
class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] prefix;

    CachedBodyRequest(HttpServletRequest request, byte[] prefix) {
        super(request);
        this.prefix = prefix;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream rest = super.getInputStream();
        InputStream replay = new SequenceInputStream(new ByteArrayInputStream(prefix), rest);
        return new ServletInputStream() {
            private boolean finished;

            @Override
            public int read() throws IOException {
                int value = replay.read();
                if (value == -1) {
                    finished = true;
                }
                return value;
            }

            @Override
            public int read(byte[] buffer, int off, int len) throws IOException {
                int count = replay.read(buffer, off, len);
                if (count == -1) {
                    finished = true;
                }
                return count;
            }

            @Override
            public boolean isFinished() {
                return finished;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("leitura assíncrona não suportada");
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String encoding = getCharacterEncoding();
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
