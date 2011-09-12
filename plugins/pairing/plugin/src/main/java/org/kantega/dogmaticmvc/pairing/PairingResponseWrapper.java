package org.kantega.dogmaticmvc.pairing;

import org.kantega.dogmaticmvc.api.ResponseWrapper;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 *
 */
public class PairingResponseWrapper implements ResponseWrapper {

    private final ServletContext servletContext;

    public PairingResponseWrapper(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public HttpServletResponse wrap(HttpServletRequest request, HttpServletResponse response) {
        String pairProgrammed = (String) request.getAttribute("pairProgrammed");
        if (pairProgrammed != null) {
            return new PairingHttpServletResponseWrapper(request.getContextPath(), response, servletContext, pairProgrammed);
        }
        return response;
    }

    private class PairingHttpServletResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, Charset.forName("utf-8")) {
            @Override
            public void close() throws IOException {
                super.close();
            }
        });
        private String contentType;
        private String contextPath;
        private String imageHtml;
        private ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int i) throws IOException {
                out.write(i);
            }

            @Override
            public void close() throws IOException {
                writer.flush();
                String replacement = new String(out.toByteArray(), "utf-8").replaceAll("<body.*>", "<body>" + imageHtml);
                PairingHttpServletResponseWrapper.super.getOutputStream().write(replacement.getBytes("utf-8"));
                PairingHttpServletResponseWrapper.super.getOutputStream().close();

            }
        };

        public PairingHttpServletResponseWrapper(String contextPath, HttpServletResponse response, ServletContext servletContext, String pairProgrammed) {
            super(response);
            this.contextPath = contextPath;
            this.imageHtml = "<div style=\"text-align: center;width: 320;float:right;padding:10px;border:1px solid gray; background-color:lightgray\">" +
                    "<h2>DogmaticMVC proudly certifies that this piece of software was pair programmed by:</h2>" +
                    "<img src=\"" + contextPath + pairProgrammed + "a.png\"><p>and:</p>" +
                    "<img src=\"" + contextPath + pairProgrammed + "b.png\"></div>";
        }

        @Override
        public void setContentType(String type) {
            super.setContentType(type);
            this.contentType = type;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return servletOutputStream;
        }
    }
}
