package com.justeam.rdp.auth;

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

/** Rejects oversized anonymous authentication JSON before MVC materializes it. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRequestSizeFilter extends OncePerRequestFilter {
    static final int MAX_BODY_BYTES = 16_384;
    private final ObjectMapper mapper;

    public AuthRequestSizeFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equalsIgnoreCase(request.getMethod()) || !uri.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiResponse.error(413, "认证请求体超过16KB限制"));
            return;
        }
        chain.doFilter(new LimitedRequest(request), response);
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private LimitedRequest(HttpServletRequest request) { super(request); }
        @Override public ServletInputStream getInputStream() throws IOException {
            return new LimitedInputStream(super.getInputStream());
        }
        @Override public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(getCharacterEncoding())));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private long consumed;
        private LimitedInputStream(ServletInputStream delegate) { this.delegate = delegate; }
        @Override public int read() throws IOException {
            int value=delegate.read();
            if(value>=0&&++consumed>MAX_BODY_BYTES)throw tooLarge();
            return value;
        }
        @Override public int read(byte[] bytes,int offset,int length)throws IOException {
            int allowed=(int)Math.min(length,Math.max(1,MAX_BODY_BYTES-consumed+1));
            int read=delegate.read(bytes,offset,allowed);
            if(read>0&&(consumed+=read)>MAX_BODY_BYTES)throw tooLarge();
            return read;
        }
        private RequestBodyTooLargeException tooLarge(){return new RequestBodyTooLargeException("认证请求体超过16KB限制");}
        @Override public boolean isFinished(){return delegate.isFinished();}
        @Override public boolean isReady(){return delegate.isReady();}
        @Override public void setReadListener(ReadListener listener){delegate.setReadListener(listener);}
    }
}
