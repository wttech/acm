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

    @Test
    void shouldNotTreatTextInsideRejectedCommentAsSeparateComment() {
        // The first "/*" opens a single (non-nested) comment ending at the first "*/".
        // It is rejected because it is not at the start of the file and not preceded by
        // import/package. The "/*" inside its body must not be re-considered as its own comment.
        String code = "xxx /* comment mentions import foo;\n\n/* real */\n\nvoid f(){}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldParseFrontmatterWithoutLeakingClosingDelimiter() {
        String code = "/*\n" + "---\n" + "version: 1.0.0\n" + "---\n" + "Description text\n" + "*/\n" + "\n"
                + "void doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertEquals("1.0.0", metadata.getValues().get("version"));
        assertEquals("Description text", metadata.getValues().get("description"));
        assertEquals(2, metadata.getValues().size());
    }

    @Test
    void shouldParseWhitespaceOnlyCode() {
        ExecutableMetadata metadata = ExecutableMetadata.parse("   \n\t\n  ");

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenOnlyLineCommentsPresent() {
        String code = "// a line comment\nvoid doRun() {\n    // another one\n}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldReturnEmptyForUnterminatedBlockComment() {
        String code = "/* never closed\n\nvoid doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlockCommentAttachedDirectlyToCode() {
        String code = "/* description */\nvoid doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenCommentIsOnlyFollowedByOneNewline() {
        String code = "/* description */\n";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyBlockComment() {
        String code = "/**/\n\nvoid doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldSkipJavadocStyleCommentEvenWhenFollowedByBlankLine() {
        // A leading "/**" comment is skipped (javadoc-style), but the comment after it is not at
        // the start of the file nor preceded by import/package, so it's correctly rejected too.
        String code = "/** javadoc style, ignored */\n\n/* real description */\n\nvoid doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    void shouldSkipJavadocStyleCommentAndFindLaterValidOneAfterImport() {
        String code =
                "import foo.Bar;\n\n/** javadoc style, ignored */\n\n/* real description */\n\nvoid doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertEquals("real description", metadata.getValues().get("description"));
    }

    @Test
    void shouldParseLargeBlockComment() {
        StringBuilder description = new StringBuilder();
        for (int index = 0; index < 10_000; index++) {
            description.append("11111111112222222222333333333344444444445555555555666666666677777777778888888888\n");
        }

        String code = "/*\n" + description + "*/\n\n" + "void doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertEquals(description.toString().trim(), metadata.getValues().get("description"));
    }

    @Test
    void shouldParseLargeDescriptionWithoutClosingFrontmatterMarker() {
        StringBuilder description = new StringBuilder("---\n");
        for (int index = 0; index < 10_000; index++) {
            description.append("11111111112222222222333333333344444444445555555555666666666677777777778888888888\n");
        }

        String code = "/*\n" + description + "*/\n\n" + "void doRun() {}";

        ExecutableMetadata metadata = ExecutableMetadata.parse(code);

        assertEquals(description.toString().trim(), metadata.getValues().get("description"));
    }
}