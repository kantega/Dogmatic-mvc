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

package org.kantega.dogmaticmvc.web;

import org.kantega.dogmaticmvc.RequestParam;
import org.kantega.dogmaticmvc.SessionAttr;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DefaultMethodParameterFactoryTest {
    private MethodParameterFactory fac;
    private HandlerClass hc;
    private Map<String, Object> model;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private ApplicationContext applicationContext;
    private ServletContext servletContext;
    private DefaultMethodParameterFactory defFac;
    private HttpSession session;


    @Before
    public void setup() {
        servletContext = mock(ServletContext.class);
        applicationContext = mock(ApplicationContext.class);

        defFac = new DefaultMethodParameterFactory(applicationContext, servletContext);
        fac = defFac;

        hc = new HandlerClass();

        model = new HashMap<String, Object>();

        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);

        session= mock(HttpSession.class);
        when(req.getSession(true)).thenReturn(session);

    }


    @Test
    public void mapShouldGiveModel() {
        Object[] objects = fac.getMethodParameters(getMethod("model", Map.class), req, resp, model);
        assertSame(model, objects[0]);
    }

    @Test
    public void servletContextShouldGiveServletContext() {
        Object[] objects = fac.getMethodParameters(getMethod("servletContext", ServletContext.class), req, resp, model);
        assertSame(servletContext, objects[0]);
    }


    @Test
    public void requestShouldGiveRequest() {
        Object[] objects = fac.getMethodParameters(getMethod("request", HttpServletRequest.class), req, resp, model);
        assertSame(req, objects[0]);
    }

    @Test
    public void responseShouldGiveResponse() {
        Object[] objects = fac.getMethodParameters(getMethod("response", HttpServletResponse.class), req, resp, model);
        assertSame(resp, objects[0]);
    }

    @Test
    public void outputStreamShouldGiveServletOutputStream() throws IOException {

        ServletOutputStream sos = mock(ServletOutputStream.class);
        when(resp.getOutputStream()).thenReturn(sos);
        Object[] objects = fac.getMethodParameters(getMethod("outputStream", OutputStream.class), req, resp, model);
        assertSame(sos, objects[0]);
    }

    @Test
    public void servletOutputStreamShouldGiveServletOutputStream() throws IOException {

        ServletOutputStream sos = mock(ServletOutputStream.class);
        when(resp.getOutputStream()).thenReturn(sos);
        Object[] objects = fac.getMethodParameters(getMethod("servletOutputStream", ServletOutputStream.class), req, resp, model);
        assertSame(sos, objects[0]);
    }

    @Test
    public void missingRequestParamShouldFail() throws IOException {


        when(req.getParameter("missing")).thenReturn(null);
        try {
            Object[] objects = fac.getMethodParameters(getMethod("missingRequestParam", String.class), req, resp, model);
            fail("Expected exception");
        } catch (Exception e) {

        }

    }


    @Test
    public void missingNotRequiredRequestParamShouldNotFail() throws IOException {


        when(req.getParameter("missing")).thenReturn(null);

        Object[] objects = fac.getMethodParameters(getMethod("missingRequestParamNotRequired", String.class), req, resp, model);

        assertNull(objects[0]);

    }


    @Test
    public void stringRequestParamShouldGiveString() throws IOException {
        when(req.getParameter("param")).thenReturn("string");
        Object[] objects = fac.getMethodParameters(getMethod("stringRequestParam", String.class), req, resp, model);
        assertEquals("string", objects[0]);
    }

    @Test
    public void intRequestParamShouldGiveInt() throws IOException {
        when(req.getParameter("param")).thenReturn("1");
        Object[] objects = fac.getMethodParameters(getMethod("intRequestParam", int.class), req, resp, model);
        assertEquals(1, objects[0]);
    }


    @Test
    public void booleanRequestParamShouldGiveBoolean() throws IOException {
        when(req.getParameter("param")).thenReturn("true");
        Object[] objects = fac.getMethodParameters(getMethod("booleanRequestParam", boolean.class), req, resp, model);
        assertEquals(true, objects[0]);
    }

    @Test
    public void longRequestParamShouldGiveLong() throws IOException {
        when(req.getParameter("param")).thenReturn("123");
        Object[] objects = fac.getMethodParameters(getMethod("longRequestParam", long.class), req, resp, model);
        assertEquals(123L, objects[0]);
    }

    @Test
    public void unknownRequestParamTypeShouldFail() throws IOException {
        when(req.getParameter("param")).thenReturn("123");
        try {
            Object[] objects = fac.getMethodParameters(getMethod("unknownRequestParamType", Object.class), req, resp, model);
            fail("Expected exception");
        } catch (Exception e) {

        }
    }
    

    @Test
    public void sessionAttrShouldGiveAttrValue() throws IOException {

        Object value = new Object();
        when(session.getAttribute("attr")).thenReturn(value);
        Object[] objects = fac.getMethodParameters(getMethod("sessionAttr", Object.class), req, resp, model);
        assertSame(value, objects[0]);
    }

    @Test
    public void missingSessionAttrShouldFail() throws IOException {

        try {
            Object[] objects = fac.getMethodParameters(getMethod("missingSessionAttr", Object.class), req, resp, model);
            fail("Expected exception");
        } catch (Exception e) {

        }
    }

    @Test
    public void missingNonRequiredSessionAttrShouldFail() throws IOException {
        Object[] objects = fac.getMethodParameters(getMethod("missingNonRequiredSessionAttr", Object.class), req, resp, model);
        assertNull(objects[0]);
    }

    @Test
    public void unassignableSessionAttrShouldFail() throws IOException {

        when(session.getAttribute("attr")).thenReturn("string");
        try {
            Object[] objects = fac.getMethodParameters(getMethod("unassignableSessionAttr", int.class), req, resp, model);
            fail("Expected exception");
        } catch (Exception e) {

        }
    }





    @Test
    public void sessionShouldGiveHttpSession() throws IOException {

        Object[] objects = fac.getMethodParameters(getMethod("session", HttpSession.class), req, resp, model);
        assertSame(session, objects[0]);
    }

    @Test
    public void printWriterShouldGiveResponseWriter() throws IOException {

        PrintWriter writer= mock(PrintWriter.class);
        when(resp.getWriter()).thenReturn(writer);
        Object[] objects = fac.getMethodParameters(getMethod("printWriter", PrintWriter.class), req, resp, model);
        assertSame(writer, objects[0]);
    }

    @Test
    public void applicationContextShouldGiveApplicationContext() throws IOException {

        Object[] objects = fac.getMethodParameters(getMethod("applicationContext", ApplicationContext.class), req, resp, model);
        assertSame(applicationContext, objects[0]);
    }

    @Test
    public void missingApplicationContextShouldFail() throws IOException {

        defFac.setApplicationContext(null);

        try {
            Object[] objects = fac.getMethodParameters(getMethod("applicationContext", ApplicationContext.class), req, resp, model);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {

        }

    }

    @Test
    public void missingSpringBeanShouldFail() throws IOException {

        when(applicationContext.getBeansOfType(SpringBean.class)).thenReturn(new HashMap<String, SpringBean>());

        try {
            Object[] objects = fac.getMethodParameters(getMethod("springBean", SpringBean.class), req, resp, model);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {

        }

    }

    @Test
    public void multipleSpringBeansShouldFail() throws IOException {

        Map<String, SpringBean> beans = new HashMap<String, SpringBean>();
        beans.put("1", new SpringBean());
        beans.put("2", new SpringBean());

        when(applicationContext.getBeansOfType(SpringBean.class)).thenReturn(beans);

        try {
            Object[] objects = fac.getMethodParameters(getMethod("springBean", SpringBean.class), req, resp, model);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {

        }

    }

    @Test
    public void singleSpringBeanShouldReturnBean() {

        Map<String, SpringBean> beans = new HashMap<String, SpringBean>();
        SpringBean bean = new SpringBean();
        beans.put("1", bean);

        when(applicationContext.getBeansOfType(SpringBean.class)).thenReturn(beans);

        Object[] objects = fac.getMethodParameters(getMethod("springBean", SpringBean.class), req, resp, model);

        assertSame(bean, objects[0]);
    }


    private Method getMethod(String name, Class... parameterTypes) {
        try {
            return hc.getClass().getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    class HandlerClass {

        public void model(Map map) {

        }

        public void request(HttpServletRequest req) {

        }

        public void response(HttpServletResponse req) {

        }

        public void outputStream(OutputStream put) {

        }

        public void servletOutputStream(ServletOutputStream put) {

        }

        public void session(HttpSession session) {

        }

        public void printWriter(PrintWriter printWriter) {

        }

        public void applicationContext(ApplicationContext applicationContext) {

        }

        public void servletContext(ServletContext servletContext) {

        }

        public void missingRequestParam(@RequestParam("missing") String param) {

        }

        public void missingRequestParamNotRequired(@RequestParam(value = "missing", required = false) String param) {

        }

        public void stringRequestParam(@RequestParam("param") String param) {

        }

        public void intRequestParam(@RequestParam("param") int param) {

        }

        public void booleanRequestParam(@RequestParam("param") boolean param) {

        }

        public void longRequestParam(@RequestParam("param") long param) {

        }

        public void unknownRequestParamType(@RequestParam("param") Object param) {

        }

        public void sessionAttr(@SessionAttr("attr") Object sessionAttr) {

        }

        public void missingSessionAttr(@SessionAttr("missing") Object sessionAttr) {

        }

        public void missingNonRequiredSessionAttr(@SessionAttr(value = "missing", required = false) Object sessionAttr) {

        }

        public void unassignableSessionAttr(@SessionAttr("attr") int sessionAttr) {

        }

        public void springBean(SpringBean springBean) {

        }
    }


}

class SpringBean {
    }
