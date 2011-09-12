package org.kantega.dogmaticmvc.pairing;

import org.kantega.dogmaticmvc.api.*;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;



/**
 *
 */
public class PairingPlugin extends DogmaticPluginBase {

    private final List<VerificationPhase> phases;
    private final List<Handler> handlers;
    private final List<ResponseWrapper> wrappers;

    public PairingPlugin(ServletContext servletContext, TemplateEngine templateEngine) {
        this.phases = Collections.<VerificationPhase>singletonList(new PairingVerificationPhase(servletContext, templateEngine));
        this.handlers = Collections.<Handler>singletonList(new CaptureSubmissionHandler(servletContext));
        this.wrappers = Collections.<ResponseWrapper>singletonList(new PairingResponseWrapper(servletContext));
    }


    @Override
    public List<VerificationPhase> getVerificationPhases() {
        return phases;
    }

    @Override
    public List<Handler> getHandlers() {
        return handlers;
    }

    @Override
    public List<ResponseWrapper> getResponseWrappers() {
        return wrappers;
    }
}
