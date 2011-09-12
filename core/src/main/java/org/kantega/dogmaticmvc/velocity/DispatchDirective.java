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

package org.kantega.dogmaticmvc.velocity;

import org.kantega.dogmaticmvc.web.DogmaticMVCFilter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.*;


/**
 */
public class DispatchDirective extends Directive {
    public String getName() {
        return "dispatch";
    }

    public int getType() {
        return LINE;
    }

    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        final Node firstNode = node.jjtGetChild(0);
        String name;
        if (firstNode instanceof ASTStringLiteral) {
            ASTStringLiteral nameNode = (ASTStringLiteral) firstNode;
            name = nameNode.literal();

            name = name.substring(1, name.length() - 1);
        } else if (firstNode instanceof ASTReference) {
            ASTReference nameNode = (ASTReference) node.jjtGetChild(0);
            StringWriter sw = new StringWriter();
            firstNode.render(internalContextAdapter, sw);
            name = sw.toString();
        } else {
            throw new IllegalArgumentException("Unknown Velocity node type " + firstNode.getClass().getName());
        }
        final HttpServletRequest request = DogmaticMVCFilter.getRequest();
        final CharResponseWrapper response = new CharResponseWrapper(DogmaticMVCFilter.getResponse());

        try {
            request.getRequestDispatcher(name).forward(request, response);
        } catch (ServletException e) {
            throw new RuntimeException("Exception dispatching request to " + name, e);
        }

        writer.write(response.toString());
        return false;
    }


}

class CharResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter output;
    private String contentType;
    private boolean shouldWrap = true;

    public String toString() {
        return output.toString();
    }

    public CharResponseWrapper(HttpServletResponse response) {
        super(response);
        output = new CharArrayWriter();
    }


    public PrintWriter getWriter() throws IOException {
        if (isWrapped()) {
            super.getWriter();
            return new PrintWriter(output);
        } else {
            return super.getWriter();
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        shouldWrap = false;
        return super.getOutputStream();
    }


    public void setContentType(String contentType) {
        super.setContentType(contentType);
        this.contentType = contentType;
    }

    public boolean isWrapped() {
        if (shouldWrap && contentType != null && (contentType.startsWith("text/html") || contentType.startsWith("text/xml"))) {
            return true;
        } else {
            return false;
        }
    }

    public String getContentType() {
        return contentType;
    }
}
