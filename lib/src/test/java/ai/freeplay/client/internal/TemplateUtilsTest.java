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

    // ========== Comprehensive Empty Array Tests ==========

    @Test
    public void testEmptyArraySectionNoRender() {
        // Empty array with section should not render
        String template = "{{#items}}Item: {{.}}{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", List.of()));
        assertEquals("", result);
    }

    @Test
    public void testEmptyArrayInverseRenders() {
        // Empty array with inverse section should render
        String template = "{{^items}}No items available{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", List.of()));
        assertEquals("No items available", result);
    }

    @Test
    public void testNonEmptyArraySectionRenders() {
        // Non-empty array with section should render
        String template = "{{#items}}Item: {{.}}{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", Arrays.asList("foo", "bar")));
        assertEquals("Item: fooItem: bar", result);
    }

    @Test
    public void testNonEmptyArrayInverseNoRender() {
        // Non-empty array with inverse section should not render
        String template = "{{^items}}No items available{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", Arrays.asList("foo", "bar")));
        assertEquals("", result);
    }

    @Test
    public void testEmptyArrayBothSectionsDetailed() {
        // Empty array with both section and inverse
        String template = "{{#items}}\nItem: {{.}}\n{{/items}}\n{{^items}}\nNo items available\n{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", List.of()));
        assertTrue(result.contains("No items available"));
        assertFalse(result.contains("Item:"));
    }

    @Test
    public void testNonEmptyArrayBothSectionsDetailed() {
        // Non-empty array with both section and inverse
        String template = "{{#items}}\nItem: {{.}}\n{{/items}}\n{{^items}}\nNo items available\n{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", List.of("foo")));
        assertTrue(result.contains("Item: foo"));
        assertFalse(result.contains("No items available"));
    }

    @Test
    public void testEmptyUniqueOffersBugReport() {
        // Empty array with unique_offers template from bug report
        String template = "{{#unique_offers}}\nDATA: {{.}}\n{{/unique_offers}}\n{{^unique_offers}}\nYou have no knowledge of personalized unique offers for this subscriber.\n{{/unique_offers}}";
        String result = TemplateUtils.format(template, Map.of("unique_offers", List.of()));
        assertTrue(result.contains("You have no knowledge of personalized unique offers for this subscriber."));
        assertFalse(result.contains("DATA:"));
    }

    @Test
    public void testNonEmptyUniqueOffers() {
        // Non-empty array with unique_offers template
        String template = "{{#unique_offers}}\nDATA: {{.}}\n{{/unique_offers}}\n{{^unique_offers}}\nYou have no knowledge of personalized unique offers for this subscriber.\n{{/unique_offers}}";
        String result = TemplateUtils.format(template, Map.of("unique_offers", Arrays.asList("offer1", "offer2")));
        assertTrue(result.contains("DATA: offer1"));
        assertTrue(result.contains("DATA: offer2"));
        assertFalse(result.contains("You have no knowledge of personalized unique offers for this subscriber."));
    }

    @Test
    public void testUndefinedVariableInverseRenders() {
        // Undefined variable with inverse section should render
        String template = "{{^items}}No items available{{/items}}";
        String result = TemplateUtils.format(template, Map.of());
        assertEquals("No items available", result);
    }

    @Test
    public void testFalseBooleanInverseRenders() {
        // False boolean with inverse section should render
        String template = "{{^flag}}Flag is false{{/flag}}";
        String result = TemplateUtils.format(template, Map.of("flag", false));
        assertEquals("Flag is false", result);
    }

    @Test
    public void testEmptyStringInverseRenders() {
        // Empty string with inverse section should render
        String template = "{{^text}}No text available{{/text}}";
        String result = TemplateUtils.format(template, Map.of("text", ""));
        assertEquals("No text available", result);
    }

    @Test
    public void testNestedObjectEmptyArray() {
        // Nested object with empty array property
        String template = "{{#data.items}}\nItem: {{.}}\n{{/data.items}}\n{{^data.items}}\nNo items in data\n{{/data.items}}";
        String result = TemplateUtils.format(template, Map.of("data", Map.of("items", List.of())));
        assertTrue(result.contains("No items in data"));
        assertFalse(result.contains("Item:"));
    }

    @Test
    public void testArraySingleEmptyStringRenders() {
        // Array with single empty string should render section
        String template = "{{#items}}\nItem: [{{.}}]\n{{/items}}\n{{^items}}\nNo items\n{{/items}}";
        String result = TemplateUtils.format(template, Map.of("items", List.of("")));
        assertTrue(result.contains("Item: []"));
        assertFalse(result.contains("No items"));
    }

    // ========== Zero Value Handling Tests ==========
    // WARNING: mustache.java treats 0 as TRUTHY (different from Python & Node SDKs which treats it as falsy)
    // This is a known issue and will be fixed in a future release.

    @Test
    public void testZeroSimpleVariableRenders() {
        // Zero as simple variable should render
        String template = "Count: {{count}}";
        String result = TemplateUtils.format(template, Map.of("count", 0));
        assertEquals("Count: 0", result);
    }

    @Test
    public void testZeroSectionRenders() {
        // Zero in section DOES render in mustache.java (differs from other SDKs)
        // In mustache.java, 0 is truthy
        String template = "{{#count}}Count is: {{.}}{{/count}}";
        String result = TemplateUtils.format(template, Map.of("count", 0));
        assertEquals("Count is: 0", result);
    }

    @Test
    public void testZeroInverseNoRender() {
        // Zero with inverse section does NOT render inverse in mustache.java (differs from other SDKs)
        // In mustache.java, 0 is truthy
        String template = "{{^count}}No count{{/count}}";
        String result = TemplateUtils.format(template, Map.of("count", 0));
        assertEquals("", result);
    }

    @Test
    public void testZeroBothSections() {
        // Zero with both section and inverse - section renders, not inverse (differs from other SDKs)
        // In mustache.java, 0 is truthy
        String template = "{{#count}}\nCount is: {{.}}\n{{/count}}\n{{^count}}\nCount is zero or missing\n{{/count}}";
        String result = TemplateUtils.format(template, Map.of("count", 0));
        assertTrue(result.contains("Count is: 0"));
        assertFalse(result.contains("Count is zero or missing"));
    }

    @Test
    public void testPositiveNumberSectionRenders() {
        // Positive number in section should render
        String template = "{{#count}}Count is: {{.}}{{/count}}";
        String result = TemplateUtils.format(template, Map.of("count", 5));
        assertEquals("Count is: 5", result);
    }

    @Test
    public void testPositiveNumberInverseNoRender() {
        // Positive number with inverse section should not render inverse
        String template = "{{^count}}No count{{/count}}";
        String result = TemplateUtils.format(template, Map.of("count", 5));
        assertEquals("", result);
    }

    @Test
    public void testNegativeNumberSectionRenders() {
        // Negative number in section should render
        String template = "{{#count}}Count is: {{.}}{{/count}}";
        String result = TemplateUtils.format(template, Map.of("count", -5));
        assertEquals("Count is: -5", result);
    }

    @Test
    public void testArrayWithZeroRenders() {
        // Array with zero should render section
        String template = "{{#numbers}}Number: {{.}} {{/numbers}}";
        String result = TemplateUtils.format(template, Map.of("numbers", Arrays.asList(0, 1, 2)));
        assertEquals("Number: 0 Number: 1 Number: 2 ", result);
    }

    @Test
    public void testZeroVsUndefinedBehavior() {
        // Zero vs undefined behavior
        String template = "Value: {{value}}";

        // Zero should render as "0"
        assertEquals("Value: 0", TemplateUtils.format(template, Map.of("value", 0)));

        // Undefined should render as empty string
        assertEquals("Value: ", TemplateUtils.format(template, Map.of()));
    }

    @Test
    public void testEmptyArrayFromJSONDeserialization() {
        // Test with variables that come from JSON parsing (simulating real-world usage)
        String jsonVariables = "{\"unique_offers\": [], \"other_field\": \"value\"}";
        Map<String, Object> variables = JSONUtil.parseMap(jsonVariables);
        
        String template = "{{#unique_offers}}DATA: {{.}}{{/unique_offers}}{{^unique_offers}}You have no knowledge of personalized unique offers for this subscriber.{{/unique_offers}}";
        
        String result = TemplateUtils.format(template, variables);
        assertEquals("You have no knowledge of personalized unique offers for this subscriber.", result);
        
        // Also test with populated array from JSON
        String jsonVariablesPopulated = "{\"unique_offers\": [\"offer1\", \"offer2\"]}";
        Map<String, Object> variablesPopulated = JSONUtil.parseMap(jsonVariablesPopulated);
        String resultPopulated = TemplateUtils.format(template, variablesPopulated);
        assertEquals("DATA: offer1DATA: offer2", resultPopulated);
    }
}
