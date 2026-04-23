import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TextGraphWhiteBoxTest {
    private TextGraph graphFrom(String text) throws IOException {
        Path input = Files.createTempFile("textgraph-whitebox-", ".txt");
        Files.writeString(input, text);

        TextGraph textGraph = new TextGraph();
        textGraph.buildGraph(input.toString());
        return textGraph;
    }

    @Test
    void pathOneReturnsImmediatelyWhenFirstWordIsMissing() throws IOException {
        TextGraph textGraph = graphFrom("a b c");

        assertEquals(
                "No missing or c in the graph!",
                textGraph.queryBridgeWords("missing", "c"));
    }

    @Test
    void pathTwoScansNeighborsAndFindsNoBridgeWord() throws IOException {
        TextGraph textGraph = graphFrom("a b c a b d c");

        assertEquals(
                "No bridge words from c to d!",
                textGraph.queryBridgeWords("c", "d"));
    }

    @Test
    void pathThreeScansNeighborsAndFindsBridgeWord() throws IOException {
        TextGraph textGraph = graphFrom("a b c a b d c");

        assertEquals(
                "The bridge words from a to c are: b",
                textGraph.queryBridgeWords("a", "c"));
    }
}
