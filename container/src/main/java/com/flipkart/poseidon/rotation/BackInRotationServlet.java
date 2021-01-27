package com.flipkart.poseidon.rotation;

import org.intellij.lang.annotations.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by shrey.garg on 2019-06-16.
 */
@Component
public class BackInRotationServlet extends HttpServlet {
    @Language("JSON")
    private static final String message = "{\"message\": \"Back in Rotation\"}";

    private final RotationManager manager;

    @Autowired
    public BackInRotationServlet(RotationManager manager) {
        this.manager = manager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

        manager.bir();
        resp.setStatus(200);
        try (ServletOutputStream output = resp.getOutputStream()) {
            output.print(message);
        }
    }
}
