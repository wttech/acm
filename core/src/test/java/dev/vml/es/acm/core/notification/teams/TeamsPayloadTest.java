package dev.vml.es.acm.core.notification.teams;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TeamsPayloadTest {

    private static final String OUTPUT = "470 KB, 4027 lines (last 512 chars)\n```\nFantaHub=Fanta Hub\n```";

    private Map<String, Object> executionFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("Status", "succeeded");
        fields.put("Duration", "3735 ms (3 seconds)");
        fields.put("Output", OUTPUT);
        fields.put("Error", "(empty)");
        return fields;
    }

    private List<TeamsPayload.CardElement> body(Map<String, Object> fields) {
        return TeamsPayload.builder()
                .message("ACM Code Execution", "Completed: /conf/acm/settings/script/manual/example.groovy", fields)
                .build()
                .getAttachments()
                .get(0)
                .getContent()
                .getBody();
    }

    @Test
    void shouldRenderMultilineFieldAsSectionAfterFactSet() {
        List<TeamsPayload.CardElement> body = body(executionFields());

        assertEquals(5, body.size());
        assertInstanceOf(TeamsPayload.TextBlock.class, body.get(0)); // title
        assertInstanceOf(TeamsPayload.TextBlock.class, body.get(1)); // text

        TeamsPayload.FactSet factSet = assertInstanceOf(TeamsPayload.FactSet.class, body.get(2));
        assertEquals(3, factSet.getFacts().size());
        assertEquals("Status", factSet.getFacts().get(0).getTitle());
        assertEquals("Error", factSet.getFacts().get(2).getTitle());

        TeamsPayload.TextBlock header = assertInstanceOf(TeamsPayload.TextBlock.class, body.get(3));
        assertEquals("Output", header.getText());
        assertEquals("Large", header.getSize());
        assertEquals("Bolder", header.getWeight());

        TeamsPayload.TextBlock content = assertInstanceOf(TeamsPayload.TextBlock.class, body.get(4));
        assertEquals(OUTPUT, content.getText());
        assertTrue(content.getWrap());
        assertNull(content.getSize());
    }

    @Test
    void shouldRenderErrorAsSectionLikeOutput() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("Status", "failed");
        fields.put("Output", OUTPUT);
        fields.put("Error", "1 KB, 12 lines\n```\njava.lang.RuntimeException: Boom!\n```");

        List<TeamsPayload.CardElement> body = body(fields);

        assertEquals(7, body.size());
        assertEquals(1, assertInstanceOf(TeamsPayload.FactSet.class, body.get(2))
                .getFacts()
                .size());
        assertEquals(
                "Output", assertInstanceOf(TeamsPayload.TextBlock.class, body.get(3)).getText());
        assertEquals(
                "Error", assertInstanceOf(TeamsPayload.TextBlock.class, body.get(5)).getText());
        assertEquals(
                "Bolder",
                assertInstanceOf(TeamsPayload.TextBlock.class, body.get(5)).getWeight());
    }

    @Test
    void shouldRenderSingleLineFieldsAsFactsOnly() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("Status", "succeeded");
        fields.put("Output", "(empty)");

        List<TeamsPayload.CardElement> body = body(fields);

        assertEquals(3, body.size());
        assertEquals(2, assertInstanceOf(TeamsPayload.FactSet.class, body.get(2))
                .getFacts()
                .size());
    }
}
