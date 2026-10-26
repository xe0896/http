package com.sanim.server;

import java.io.IOException;

@FunctionalInterface
public interface FunctionalHandler {
    void handle(Request request, Response response) throws IOException;
}