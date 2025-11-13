package dev.vml.es.acm.core.format;

import groovy.text.GStringTemplateEngine;
import java.io.*;
import java.util.Map;

public class TemplateFormatter {

    public String renderString(String template, Map<?, ?> vars) {
        try {
            Writer writer = new StringWriter();
            getEngine().createTemplate(template).make(vars).writeTo(writer);
            return writer.toString();
        } catch (Exception e) {
            throw new FormatException("Cannot render template from & to string!", e);
        }
    }

    public void renderFromString(String template, Map<?, ?> vars, Writer writer) {
        try {
            getEngine().createTemplate(template).make(vars).writeTo(writer);
        } catch (Exception e) {
            throw new FormatException("Cannot render template from string!", e);
        }
    }

    public String renderToString(Reader reader, Map<?, ?> vars) {
        try {
            Writer writer = new StringWriter();
            getEngine().createTemplate(reader).make(vars).writeTo(writer);
            return writer.toString();
        } catch (Exception e) {
            throw new FormatException("Cannot render template to string!", e);
        }
    }

    public void render(InputStream inputStream, Map<?, ?> vars, OutputStream outputStream) {
        try (Reader reader = new InputStreamReader(inputStream);
                Writer writer = new OutputStreamWriter(outputStream)) {
            getEngine().createTemplate(reader).make(vars).writeTo(writer);
        } catch (Exception e) {
            throw new FormatException("Cannot render template!", e);
        }
    }

    public void render(Reader reader, Map<?, ?> vars, Writer writer) {
        try {
            getEngine().createTemplate(reader).make(vars).writeTo(writer);
        } catch (Exception e) {
            throw new FormatException("Cannot render template!", e);
        }
    }

    private GStringTemplateEngine getEngine() {
        return new GStringTemplateEngine();
    }
}
