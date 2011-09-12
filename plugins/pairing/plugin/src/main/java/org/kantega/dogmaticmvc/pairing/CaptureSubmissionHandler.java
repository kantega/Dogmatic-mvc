package org.kantega.dogmaticmvc.pairing;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.kantega.dogmaticmvc.api.Handler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 */
public class CaptureSubmissionHandler implements Handler {
    private final ServletContext servletContext;

    public CaptureSubmissionHandler(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public boolean canHandle(HttpServletRequest request) {
        return "/pairing/capture".equals(request.getServletPath());
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) {

        String servletPath = request.getParameter("servletPath");

        File imagesDirectory = new File(servletContext.getRealPath("/pairing/images"));
        imagesDirectory.mkdirs();

        File sourceImageDirectory = new File(imagesDirectory, servletPath.substring("/".length()));
        sourceImageDirectory.mkdirs();

        String a = request.getParameter("a");
        String b = request.getParameter("b");

        write(a, new File(sourceImageDirectory, "a.png"));
        write(b, new File(sourceImageDirectory, "b.png"));

        try {
            response.sendRedirect(servletPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(String a, File file) {
        try {
            FileOutputStream output = new FileOutputStream(file);
            IOUtils.copy(new ByteArrayInputStream(Base64.decodeBase64(a)), output);
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
