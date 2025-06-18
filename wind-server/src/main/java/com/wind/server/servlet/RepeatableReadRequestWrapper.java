package com.wind.server.servlet;

import com.wind.common.exception.BaseException;
import com.wind.web.util.HttpQueryUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 支持重复读取的 {@link HttpServletRequestWrapper}
 * 注意：请谨慎使用该类，由于会缓存请求内容 {@link #cachedContent}，不要缓存大请求（例如文件上传），导致内存压力过大而导致程序奔溃
 *
 * @author wuxp
 * @see org.springframework.web.util.ContentCachingRequestWrapper
 */
public class RepeatableReadRequestWrapper extends HttpServletRequestWrapper {

    private final ByteArrayOutputStream cachedContent;

    private final int contentCacheLimit;

    /**
     * 查询参数、表单参数缓存
     * {@link HttpServletRequest#getParameter(String)}
     */
    private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

    private ServletInputStream inputStream;

    private BodyInputStream bodyInputStream;

    private BufferedReader reader;

    /**
     * Create a new RepeatableReadRequestWrapper for the given servlet request.
     *
     * @param request the original servlet request
     */
    public RepeatableReadRequestWrapper(HttpServletRequest request) {
        this(request, request.getContentLength() > 0 ? request.getContentLength() : 1024);
    }

    /**
     * Create a new RepeatableReadRequestWrapper for the given servlet request.
     *
     * @param request           the original servlet request
     * @param contentCacheLimit the maximum number of bytes to cache per request
     * @see #handleContentOverflow(int)
     * @since 4.3.6
     */
    public RepeatableReadRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
        super(request);
        this.cachedContent = new ByteArrayOutputStream(contentCacheLimit);
        this.contentCacheLimit = contentCacheLimit;
        tryCacheRequestParameters();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.inputStream == null) {
            this.inputStream = new ContentCachingInputStream(getRequest().getInputStream());
            return this.inputStream;
        }
        if (this.bodyInputStream == null) {
            this.bodyInputStream = new BodyInputStream(this.cachedContent.toByteArray());
        }
        // 重置流，用于多次读取
        this.bodyInputStream.reset();
        return this.bodyInputStream;
    }

    @Override
    public String getCharacterEncoding() {
        String enc = super.getCharacterEncoding();
        return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.reader == null) {
            this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }
        return this.reader;
    }

    @Override
    public String getParameter(String name) {
        return parameters.getFirst(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = new HashMap<>();
        parameters.forEach((name, values) -> {
            if (values != null) {
                result.put(name, values.toArray(new String[0]));
            }
        });
        return result;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Iterator<String> iterator = parameters.keySet().iterator();
        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public String[] getParameterValues(String name) {
        tryCacheRequestParameters();
        List<String> result = parameters.get(name);
        return result == null ? null : result.toArray(new String[0]);
    }

    /**
     * Template method for handling a content overflow: specifically, a request
     * body being read that exceeds the specified content cache limit.
     * <p>The default implementation is empty. Subclasses may override this to
     * throw a payload-too-large exception or the like.
     *
     * @param contentCacheLimit the maximum number of bytes to cache per request
     *                          which has just been exceeded
     * @see #RepeatableReadRequestWrapper(HttpServletRequest, int)
     * @since 4.3.6
     */
    protected void handleContentOverflow(int contentCacheLimit) {
        throw BaseException.common("cache request body overflow，limit = " + contentCacheLimit);
    }

    private void tryCacheRequestParameters() {
        if (parameters.isEmpty()) {
            fillParametersByQuery();
            fillParametersByForm();
        }
    }

    private void fillParametersByQuery() {
        parameters.putAll(HttpQueryUtils.parseQueryParams(getQueryString()));
    }

    private boolean isFormRequest() {
        String contentType = getContentType();
        return (contentType != null && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
    }

    private void fillParametersByForm() {
        try {
            if (isFormRequest()) {
                String body = StreamUtils.copyToString(getInputStream(), StandardCharsets.UTF_8);
                parameters.putAll(HttpQueryUtils.parseQueryParams(body));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write request parameters to cached", exception);
        }
    }

    private class ContentCachingInputStream extends ServletInputStream {

        private final ServletInputStream is;

        private boolean overflow = false;

        public ContentCachingInputStream(ServletInputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            int ch = this.is.read();
            if (ch != -1 && !this.overflow) {
                if (cachedContent.size() == contentCacheLimit) {
                    this.overflow = true;
                    handleContentOverflow(contentCacheLimit);
                } else {
                    cachedContent.write(ch);
                }
            }
            return ch;
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            int count = this.is.read(b);
            writeToCache(b, 0, count);
            return count;
        }

        private void writeToCache(final byte[] b, final int off, int count) {
            if (!this.overflow && count > 0) {
                if (count + cachedContent.size() > contentCacheLimit) {
                    this.overflow = true;
                    cachedContent.write(b, off, contentCacheLimit - cachedContent.size());
                    handleContentOverflow(contentCacheLimit);
                    return;
                }
                cachedContent.write(b, off, count);
            }
        }

        @Override
        public int read(@NonNull final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.read(b, off, len);
            writeToCache(b, off, count);
            return count;
        }

        @Override
        public int readLine(final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.readLine(b, off, len);
            writeToCache(b, off, count);
            return count;
        }

        @Override
        public boolean isFinished() {
            return this.is.isFinished();
        }

        @Override
        public boolean isReady() {
            return this.is.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            this.is.setReadListener(readListener);
        }
    }

    private static class BodyInputStream extends ServletInputStream {

        private final InputStream delegate;

        public BodyInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return this.delegate.read();
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            return this.delegate.read(b, off, len);
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return this.delegate.read(b);
        }

        @Override
        public long skip(long n) throws IOException {
            return this.delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            return this.delegate.available();
        }

        @Override
        public void close() throws IOException {
            this.delegate.close();
        }

        @Override
        public synchronized void mark(int readLimit) {
            this.delegate.mark(readLimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            this.delegate.reset();
        }

        @Override
        public boolean markSupported() {
            return this.delegate.markSupported();
        }
    }

}
