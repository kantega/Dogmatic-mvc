package org.kantega.dogmaticmvc.pairing;

import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.api.VerificationPhase;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;

/**
 */
public class PairingVerificationPhase implements VerificationPhase {

    private final ServletContext servletContext;
    private TemplateEngine templateEngine;

    public PairingVerificationPhase(ServletContext servletContext, TemplateEngine templateEngine) {
        this.servletContext = servletContext;
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {

        try {
            if(method.getAnnotation(PairProgrammed.class) != null) {
                String path = "/pairing/images/" + method.getDeclaringClass().getName() + "/";
                if(servletContext.getResource(path) == null) {
                    TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/pairing/provepairing.vm");
                    template.setAttribute("servletPath", req.getServletPath());
                    template.render(req, resp);
                    return false;
                } else {
                    req.setAttribute("pairProgrammed", path);
                }

            }
            return true;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
