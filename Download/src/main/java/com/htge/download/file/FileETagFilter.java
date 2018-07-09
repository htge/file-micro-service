package com.htge.download.file;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Component
public class FileETagFilter extends ShallowEtagHeaderFilter {
    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";

    //全局限速，速率=limiter.getRate()*blockSize
    private static final int blockSize = 16384;
    private static final RateLimiter limiter = RateLimiter.create(64.0);

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletResponse responseToUse = response;
        if (!isAsyncDispatch(request) && !(response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new FileAwareContentCachingResponseWrapper(response, request);
        }

        filterChain.doFilter(request, responseToUse);

        if (!isAsyncStarted(request) && !isContentCachingDisabled(request)) {
            updateResponse(request, responseToUse);
        }
    }

    private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        Assert.notNull(responseWrapper, "ContentCachingResponseWrapper not found");
        HttpServletResponse rawResponse = (HttpServletResponse) responseWrapper.getResponse();

        String requestETag = request.getHeader(HEADER_IF_NONE_MATCH);
        String responseETag = response.getHeader(HEADER_ETAG);
        if (requestETag != null && responseETag != null
                && (responseETag.equals(requestETag)
                || responseETag.replaceFirst("^W/", "")
                .equals(requestETag.replaceFirst("^W/", ""))
                || "*".equals(requestETag))) {
            if (logger.isTraceEnabled()) {
                logger.trace("ETag [" + responseETag + "] equal to If-None-Match, sending 304");
            }
            rawResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("ETag [" + responseETag + "] not equal to If-None-Match [" + requestETag +
                        "], sending normal response");
            }
            File file = (File)request.getAttribute("file");
            FileRange fileRange = (FileRange)request.getAttribute("fileRange");
            if (file == null || fileRange == null) {
                responseWrapper.copyBodyToResponse();
            } else {
                updateResponseBody(file, fileRange, rawResponse.getOutputStream());
            }
        }
    }

    private void updateResponseBody(File file, FileRange fileRange, OutputStream outputStream) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            long start = fileRange.getStart();
            long size = fileRange.getEnd() - start + 1;
            if (start > 0) { //跳过N个字节
                if (inputStream.skip(start) != start) {
                    logger.error("Skip file failed?");
                }
            }

            byte[] b = new byte[blockSize];
            int n;
            while (size > 0 && (n = inputStream.read(b)) != -1) {
                limiter.acquire();
                if (size > n) {
                    outputStream.write(b, 0, n);
                } else {
                    outputStream.write(b, 0, (int)size);
                }
                size -= n;
            }
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
        } catch (ClientAbortException e) {
            logger.info("Download "+file.toString()+" abouted");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { //这里要关闭资源
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isContentCachingDisabled(HttpServletRequest request) {
        return (request.getAttribute(STREAMING_ATTRIBUTE) != null);
    }

    public class FileAwareContentCachingResponseWrapper extends ContentCachingResponseWrapper {

        private final HttpServletRequest request;

        private FileAwareContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return (useRawResponse() ? getResponse().getOutputStream() : super.getOutputStream());
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return (useRawResponse() ? getResponse().getWriter() : super.getWriter());
        }

        private boolean useRawResponse() {
            return isContentCachingDisabled(this.request);
        }
    }
}
