package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayClientException;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static org.junit.Assert.*;

public class TemplateUtilsTest {

    @Test
    public void testSingleReplacementMustache() {
        String template = "Answer this question: {{question}}";
        String result = TemplateUtils.format(
                template,
                Map.of(
                        "question", "Why isn't my sink working?"
                ));
        assertEquals("Answer this question: Why isn't my sink working?", result);
    }

    @Test
    public void testMultipleReplacementsMustache() {
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
    public void testMissingReplacementMustache() {
        String template = "Answer this question {{tone}}: {{question}}";
        String result = TemplateUtils.format(
                template,
                Map.of(
                        "question", "Why isn't my sink working?"
                ));
        assertEquals("Answer this question : Why isn't my sink working?", result);
    }

    @Test
    public void testSimpleTypes() {
        Object[] validValues = new Object[]{
                false,
                123,
                Integer.MAX_VALUE + 10L,
                123.456F,
                Float.MAX_VALUE + 100D,
                "stringValue"
        };

        String template = "Simple value {{value}}";
        for (Object validValue : validValues) {
            String result = TemplateUtils.format(
                    template,
                    Map.of("value", validValue));
            assertEquals(String.format("Simple value %s", validValue), result);
        }
    }

    @Test
    public void testMapValue() {
        Map<String, String> validValues = Map.of("name", "firstName", "last", "lastName");

        String template = "Map value {{value.name}} {{value.last}}";
        String result = TemplateUtils.format(
                template,
                Map.of("value", validValues));
        assertEquals("Map value firstName lastName", result);
    }

    @Test
    public void testListValue() {
        List<String> validValues = List.of("one", "two", "three");

        String template = "Value {{#value}}{{.}} {{/value}}";
        String result = TemplateUtils.format(
                template,
                Map.of("value", validValues));
        assertEquals("Value one two three ", result);
    }

    @Test
    public void testObjectValue() {
        Object invalidValue = new Object();

        String template = "value {{value}}";
        try {
            TemplateUtils.format(
                    template,
                    Map.of("value", invalidValue));
            fail("Should have gotten an exception");
        } catch (FreeplayClientException e) {
            assertEquals("Unsupported type provided as input variable: java.lang.Object", e.getMessage());
        }
    }

    @Test
    public void testLambda() {
        Function<String, Integer> function = (String string) -> 1;
        Callable<String> callable = () -> "hi";
        @SuppressWarnings("Convert2Lambda")
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
            }
        };
        Runnable testListValue = this::testListValue;   // Method reference, albeit just a runnable
        Object[] functionTypes = new Object[]{function, runnable, callable, testListValue};

        for (Object functionType : functionTypes) {
            String template = "value {{value}}";
            try {
                TemplateUtils.format(
                        template,
                        Map.of("value", functionType));
                fail("Should have gotten an exception");
            } catch (FreeplayClientException e) {
                assertTrue(e.getMessage().contains("Unsupported type provided"));
            }
        }
    }

    @Test
    public void testNestedInvalidValue() {
        Function<String, Integer> function = (String string) -> 1;
        Map<String, Object> value = Map.of(
                "nested", function
        );

        String template = "value {{value.nested}}";
        try {
            TemplateUtils.format(
                    template,
                    Map.of("value", value));
            fail("Should have gotten an exception");
        } catch (FreeplayClientException e) {
            assertTrue(e.getMessage().contains("Unsupported type provided"));
        }
    }
}
