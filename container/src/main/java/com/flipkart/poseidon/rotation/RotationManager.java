package com.flipkart.poseidon.rotation;

import org.springframework.stereotype.Component;

/**
 * Created by shrey.garg on 2019-06-16.
 */
@Component
public class RotationManager {
    private volatile boolean status = false;

    public boolean status() {
        return status;
    }

    public void bir() {
        status = true;
    }

    public void oor() {
        status = false;
    }
}
