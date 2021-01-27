package com.flipkart.poseidon;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloHandler extends AbstractHandler {


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/plain");

        response.getOutputStream().println("Hello");

        request.setHandled(true);
    }
}