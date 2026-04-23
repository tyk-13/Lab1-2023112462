import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TextGraphBlackBoxTest {
    private TextGraph graphFrom(String text) throws IOException {
        Path input = Files.createTempFile("textgraph-blackbox-", ".txt");
        Files.writeString(input, text);

        TextGraph textGraph = new TextGraph();
        textGraph.buildGraph(input.toString());
        return textGraph;
    }

    @Test
    void queryBridgeWordsReturnsSingleBridgeForValidWords() throws IOException {
        TextGraph textGraph = graphFrom("a b c a b d c");

        assertEquals(
                "The bridge words from a to c are: b",
                textGraph.queryBridgeWords("a", "c"));
    }

    @Test
    void queryBridgeWordsReturnsAllPossibleBridgeWords() throws IOException {
        TextGraph textGraph = graphFrom("start alpha end start beta end");

        String result = textGraph.queryBridgeWords("start", "end");

        assertTrue(result.startsWith("The bridge words from start to end are: "));
        assertTrue(result.contains("alpha"));
        assertTrue(result.contains("beta"));
    }

    @Test
    void queryBridgeWordsReportsNoBridgeWhenWordsExistButNoMiddleWordConnectsThem()
            throws IOException {
        TextGraph textGraph = graphFrom("a b c a b d c");

        assertEquals(
                "No bridge words from c to d!",
                textGraph.queryBridgeWords("c", "d"));
    }

    @Test
    void queryBridgeWordsReportsMissingWordForOutOfGraphInput() throws IOException {
        TextGraph textGraph = graphFrom("a b c");

        assertEquals(
                "No a or missing in the graph!",
                textGraph.queryBridgeWords("a", "missing"));
    }

    @Test
    void queryBridgeWordsHandlesEmptyWordAsInvalidBoundaryInput() throws IOException {
        TextGraph textGraph = graphFrom("a b c");

        assertEquals(
                "No  or c in the graph!",
                textGraph.queryBridgeWords("", "c"));
    }
}
