package com.justeam.rdp.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.RequestBodyTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Enforces the anonymous device-ingest body limit while bytes are read, before a String/JSON tree is allocated. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeviceRequestSizeFilter extends OncePerRequestFilter {
    static final int MAX_BODY_BYTES = 262_144;
    private final ObjectMapper mapper;

    public DeviceRequestSizeFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !uri.startsWith("/api/device-ingest/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            reject(response);
            return;
        }
        chain.doFilter(new LimitedRequest(request, MAX_BODY_BYTES), response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(413, "设备测点报文超过256KB限制"));
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private final int limit;

        private LimitedRequest(HttpServletRequest request, int limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedInputStream(super.getInputStream(), limit);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(getCharacterEncoding())));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long limit;
        private long consumed;

        private LimitedInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0 && ++consumed > limit) throw tooLarge();
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int allowed = (int) Math.min(length, Math.max(1, limit - consumed + 1));
            int read = delegate.read(bytes, offset, allowed);
            if (read > 0 && (consumed += read) > limit) throw tooLarge();
            return read;
        }

        private RequestBodyTooLargeException tooLarge() {
            return new RequestBodyTooLargeException("设备测点报文超过256KB限制");
        }

        @Override public boolean isFinished() { return delegate.isFinished(); }
        @Override public boolean isReady() { return delegate.isReady(); }
        @Override public void setReadListener(ReadListener listener) { delegate.setReadListener(listener); }
    }
}
