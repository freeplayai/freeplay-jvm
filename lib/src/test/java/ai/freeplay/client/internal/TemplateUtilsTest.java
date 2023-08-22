package ai.freeplay.client.internal;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TemplateUtilsTest {
    @Test
    public void testSingleReplacement() {
        String template = "Answer this question: {{question}}";
        String result = TemplateUtils.format(
                template,
                Map.of(
                        "question", "Why isn't my sink working?"
                ));
        assertEquals("Answer this question: Why isn't my sink working?", result);
    }

    @Test
    public void testMultipleReplacements() {
        String template = "Answer this question {{tone}}: {{question}}";
        String result = TemplateUtils.format(
                template,
                Map.of(
                        "tone", "nicely",
                        "question", "Why isn't my sink working?"
                ));
        assertEquals("Answer this question nicely: Why isn't my sink working?", result);
    }

    @Test
    public void testMissingReplacement() {
        String template = "Answer this question {{tone}}: {{question}}";
        String result = TemplateUtils.format(
                template,
                Map.of(
                        "question", "Why isn't my sink working?"
                ));
        assertEquals("Answer this question : Why isn't my sink working?", result);
    }
}
