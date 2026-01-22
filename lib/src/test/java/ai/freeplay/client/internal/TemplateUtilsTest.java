package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayClientException;
import org.junit.Test;

import java.util.Arrays;
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

    @Test
    public void testJson() {
        String template = "{{foo}}";
        String result = TemplateUtils.format(template, Map.of("foo", Map.of("bar", "baz")));
        assertEquals("{\"bar\":\"baz\"}", result);
    }

    @Test
    public void testEmptyMap() {
        String template = "{{foo}}";
        String result = TemplateUtils.format(template, Map.of("foo", Map.of()));
        assertEquals("{}", result);
    }

    @Test
    public void testTopLevelArray() {
        String template = "{{foo}}";
        String result = TemplateUtils.format(template, Map.of("foo", Arrays.asList("Larry", "Moe", "Curly")));
        assertEquals("[\"Larry\",\"Moe\",\"Curly\"]", result);
    }


    @Test
    public void testNumber() {
        String template = "{{foo}}";
        String result = TemplateUtils.format(template, Map.of("foo", 1));
        assertEquals("1", result);
    }

    @Test
    public void testConditional() {
        String template = "{{#bar}}{{foo}}{{/bar}}";
        assertEquals("", TemplateUtils.format(template, Map.of("foo", 1, "bar", List.of())));
        assertEquals("1", TemplateUtils.format(template, Map.of("foo", 1, "bar", true)));
    }

    @Test
    public void testLiteral() {
        String template = "{{{literal}}}";
        String result = TemplateUtils.format(template, Map.of("literal", Map.of("foo", "bar")));
        assertEquals("{\"foo\":\"bar\"}", result);
    }

    @Test
    public void testUndefinedVariable() {
        String template = "{{foo}}";
        String result = TemplateUtils.format(template, Map.of());
        assertEquals("", result);
    }

    @Test
    public void testArrayVariable() {
        String template = "{{#foo}}{{.}}{{/foo}}";
        String result = TemplateUtils.format(template, Map.of("foo", Arrays.asList(1, 2, 3)));
        assertEquals("123", result);
    }

    @Test
    public void testNestedObject() {
        String template = "{{foo.bar}}";
        String result = TemplateUtils.format(template, Map.of("foo", Map.of("bar", "baz")));
        assertEquals("baz", result);
    }

    @Test
    public void testUnescapedCharacters() {
        String template = "{{{foo}}}";
        String result = TemplateUtils.format(template, Map.of("foo", "<script>alert(\"xss\")</script>"));
        assertEquals("<script>alert(\"xss\")</script>", result);
    }

    @Test
    public void testMissingClosingTag() {
        String template = "{{#foo}}{{bar}}";
        assertThrows(Exception.class, () -> TemplateUtils.format(template, Map.of("foo", true, "bar", "baz")));
    }

    @Test
    public void testEmptyTemplate() {
        String template = "";
        String result = TemplateUtils.format(template, Map.of("foo", "bar"));
        assertEquals("", result);
    }

    @Test
    public void testWhitespaceHandling() {
        String template = "{{ foo }}";
        String result = TemplateUtils.format(template, Map.of("foo", "bar"));
        assertEquals("bar", result);
    }

    @Test
    public void testArrayOfNumbersAndStrings() {
        String template = "{{#foo}}{{.}}{{/foo}}";
        String result = TemplateUtils.format(template, Map.of("foo", Arrays.asList(1, "two", 3, "four", 5.1)));
        assertEquals("1two3four5.1", result);
    }

    @Test
    public void testMissingVariable() {
        String template = "{{foo}}";
        TemplateUtils.format(template, Map.of());
        // This test doesn't assert anything, it just checks that no exception is thrown
    }

    private static class SimpleBeanForTest {
        private String name;

        public SimpleBeanForTest() {
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testNonMapObjectThrows() {
        String template = "{{foo}}";
        SimpleBeanForTest bean = new SimpleBeanForTest();
        bean.setName("Mr. Bean");

        assertThrows(FreeplayClientException.class, () -> TemplateUtils.format(template, Map.of("foo", bean)));
    }

    @Test
    public void testFunctionValueThrows() {
        String template = "{{foo}}";
        Function<String, Integer> function = (String) -> 1;

        assertThrows(FreeplayClientException.class, () -> TemplateUtils.format(template, Map.of("foo", function)));
    }
}
