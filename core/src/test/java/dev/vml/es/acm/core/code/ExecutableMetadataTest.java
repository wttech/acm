package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutableMetadataTest {

    private static final Path SCRIPTS_BASE_PATH =
            Paths.get("../ui.content.example/src/main/content/jcr_root/conf/acm/settings/script");

    private String readScript(String relativePath) throws IOException {
        Path scriptPath = SCRIPTS_BASE_PATH.resolve(relativePath);
        return new String(Files.readAllBytes(scriptPath), StandardCharsets.UTF_8);
    }

    @Test
    void shouldParseEmptyCode() {
        ExecutableMetadata metadata = ExecutableMetadata.parse("");

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldParseNullCode() {
        ExecutableMetadata metadata = ExecutableMetadata.parse(null);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldParseHelloWorldScript() throws IOException {
        String code = readScript("manual/example/ACME-200_hello-world.groovy");
        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertEquals(
                "Prints \"Hello World!\" to the console.", metadata.getValues().get("description"));
    }

    @Test
    void shouldParseInputsScript() throws IOException {
        String code = readScript("manual/example/ACME-201_inputs.groovy");
        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        String description = (String) metadata.getValues().get("description");
        assertNotNull(description);
        assertTrue(description.contains("Prints animal information to the console based on user input"));
        assertEquals("<john.doe@acme.com>", metadata.getValues().get("author"));
    }

    @Test
    void shouldParsePageThumbnailScript() throws IOException {
        String code = readScript("manual/example/ACME-202_page-thumbnail.groovy");
        ExecutableMetadata metadata = ExecutableMetadata.parse(code);
        String description = (String) metadata.getValues().get("description");

        assertNotNull(description);
        assertTrue(description.contains("Updates the thumbnail"));
        assertTrue(description.contains("File must be a JPEG image"));
        assertEquals("<john.doe@acme.com>", metadata.getValues().get("author"));
    }

    @Test
    void shouldParseScriptWithoutFrontmatter() throws IOException {
        String code = readScript("automatic/example/ACME-20_once.groovy");
        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertFalse(metadata.getValues().isEmpty());
        assertNotNull(metadata.getValues().get("description"));
        String description = (String) metadata.getValues().get("description");
        assertTrue(description.contains("conditions.once()"));
    }

    @Test
    void shouldParseMultipleAuthors() {
        String code = "/*\n" + "---\n"
                + "author:\n"
                + "  - John Doe\n"
                + "  - Jane Smith\n"
                + "---\n"
                + "Multi-author script\n"
                + "*/\n"
                + "\n"
                + "void doRun() {\n"
                + "    println \"Hello\"\n"
                + "}";
        ExecutableMetadata metadata = ExecutableMetadata.parse(code);
        Object authors = metadata.getValues().get("author");
        assertTrue(authors instanceof List);
        @SuppressWarnings("unchecked")
        List<String> authorsList = (List<String>) authors;

        assertEquals(2, authorsList.size());
        assertEquals("John Doe", authorsList.get(0));
        assertEquals("Jane Smith", authorsList.get(1));
    }

    @Test
    void shouldParseCustomTags() {
        String code = "/*\n" + "---\n"
                + "version: 1.0.0\n"
                + "since: 2025-01-01\n"
                + "category: migration\n"
                + "---\n"
                + "Custom script with metadata\n"
                + "*/\n"
                + "\n"
                + "void doRun() {\n"
                + "    println \"Hello\"\n"
                + "}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertEquals("Custom script with metadata", metadata.getValues().get("description"));
        assertEquals("1.0.0", metadata.getValues().get("version"));
        assertEquals("2025-01-01", metadata.getValues().get("since"));
        assertEquals("migration", metadata.getValues().get("category"));
    }
}
