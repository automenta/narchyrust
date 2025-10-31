package nars;

import jcog.data.list.Lst;
import nars.lang.LM;
import nars.lang.NARLang;
import nars.lang.NARLang0;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NARLangTest {
    private TestNARLang l;
    private List<String> outputs;

    /**
     * Test implementation that captures outputs and allows controlled testing
     */
    static class TestNARLang extends NARLang {
        private final List<String> outputs = new Lst<>();
        private final TestLM lm = new TestLM("","");
        private static final TestLM chat = new TestLM("","");
        private static final TestLM translate = new TestLM();

        public TestNARLang() {
            this(NARS.tmp());
        }

        public TestNARLang(NAR nar) {
            super(nar.main(), new RecentAttention(), chat, translate,
                10, 0.1f, 8, 100);
        }

        @Override
        protected void accept(Node x) {
            super.accept(x);
            outputs.add(x.content());
        }

        public List<String> getOutputs() {
            return new Lst<>(outputs);
        }

        public void clearOutputs() {
            outputs.clear();
        }
    }

    /**
     * Simple test language model that returns predictable responses
     */
    static class TestLM extends LM {
        private final List<String> responses = new Lst<>();
        private int responseIndex = 0;

        public TestLM(String url, String model) {
            super(url, model);
        }

        public TestLM(LM l) {
            var url = l.url;
            var model = l.model;
            this(url, model);
        }

        public TestLM() {
            this(NARLang0.lm());
        }

        public void addResponse(String response) {
            responses.add(response);
        }

        @Override
        public String query(String input, String systemPrompt) {
            return responses.isEmpty() ? "Default response for: " + input : responses.get(responseIndex++ % responses.size());
        }

        @Override
        public CompletableFuture<String> queryAsync(String input, String systemPrompt) {
            return CompletableFuture.completedFuture(query(input, systemPrompt));
        }
    }

    @BeforeEach
    void setup() {
        l = new TestNARLang();
        outputs = l.getOutputs();
    }

    @Test
    void testBasicInputOutput() {
        // Test that system accepts input and produces some output
        l.input("The sky is blue", "en");
        assertFalse(outputs.isEmpty(), "Should produce some output");
    }

    @Test
    void testLanguageTranslation() {
        // Test translation between languages
        l.input("The cat chases the mouse", "en");

        // Wait briefly for async operations
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify translations were attempted
        var hasNarsese = outputs.stream().anyMatch(o -> o.contains("chase") && o.contains("cat") && o.contains("mouse"));
        assertTrue(hasNarsese, "Should produce Narsese translation");
    }

    @Test
    void testMemoryManagement() {
        // Test that system maintains memory limits
        for (var i = 0; i < 20; i++) l.input("Test input " + i, "en");

        assertTrue(l.graphSize() <= 8, "Graph should not exceed capacity");
    }

    @Test
    void testReasoningChain() {
        // Test basic reasoning capabilities
        l.input("All birds can fly", "en");
        l.input("A penguin is a bird", "en");
        l.input("What can a penguin do?", "en");

        // Wait for reasoning
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify some form of reasoning output was produced
        assertTrue(outputs.stream().anyMatch(o ->
                        o.toLowerCase().contains("penguin") && o.toLowerCase().contains("fly")),
                "Should produce reasoning about penguin's capabilities");
    }

    @Test
    void testAttentionMechanism() {
        // Add several inputs
        for (var i = 0; i < 5; i++) l.input("Important fact " + i, "en");

        // Add a highly relevant input
        l.input("Critical information!", "en");

        // Test that attention mechanism prioritizes relevant information
        var selected = l.attention.select(l.graph, 2);
        assertFalse(selected.isEmpty(),
                "Should select nodes for attention");
        assertTrue(selected.stream().anyMatch(n -> n.content().contains("Critical")),
                "Should prioritize important information");
    }

    @Test
    void testConsistency() {
        // Test that system maintains consistent knowledge
        l.input("Cats are mammals", "en");
        l.input("All mammals are animals", "en");

        // Wait for processing
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check for consistent knowledge
        assertTrue(outputs.stream().anyMatch(o ->
                o.toLowerCase().contains("cat") && o.toLowerCase().contains("animal")),
        "Should derive that cats are animals");
    }

    @Test
    void testErrorHandling() {
        // Test system's response to invalid input
        assertDoesNotThrow(() -> l.input("", "en"),
                "Should handle empty input gracefully");
        assertDoesNotThrow(() -> l.input("Test", "invalid_language"),
                "Should handle invalid language gracefully");
    }
}