/*
 * Copyright 2011 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.dogmaticmvc.agile;

import org.kantega.dogmaticmvc.api.ResponseWrapper;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.Charset;

/**
 *
 */
public class AgileResponseWrapper implements ResponseWrapper{
    @Override
    public HttpServletResponse wrap(HttpServletRequest request, HttpServletResponse response) {
        if(Boolean.TRUE.equals(request.getAttribute("isAgile"))) {
            return new ManifestoResponseWrapper(request, response);
        }
        return response;
    }

    private class ManifestoResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, Charset.forName("utf-8")));

        private HttpServletRequest request;

        private ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int i) throws IOException {
                out.write(i);
            }

            @Override
            public void close() throws IOException {
                writer.flush();
                String content = new String(out.toByteArray(), "utf-8");

                content = content.replaceAll("<body", "<body background=\"" +request.getContextPath() + "/agile/background.jpg\" ");

                ManifestoResponseWrapper.super.getOutputStream().write(content.getBytes("utf-8"));
                super.close();

            }
        };

        public ManifestoResponseWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(response);
            this.request = request;
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
