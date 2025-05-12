package com.vml.es.aem.acm.core.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Mock {

    String getId();

    boolean request(HttpServletRequest request) throws MockRequestException;

    void respond(HttpServletRequest request, HttpServletResponse response) throws MockResponseException;

    void fail(HttpServletRequest request, HttpServletResponse response, Exception e) throws MockResponseException;
}
