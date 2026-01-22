package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Node;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TemplateUtils {

    public static String format(String template, Map<String, Object> variables) {
        validateVariables(variables);

        DefaultMustacheFactory mf = new NoEscapeMustacheFactory();
        mf.setObjectHandler(new JSONObjectSerializingObjectHandler());

        Mustache mustache;
        try {
            mustache = mf.compile(new StringReader(template), "<template-name>");
        } catch (Exception e) {
            throw new FreeplayClientException("Unable to format template.", e);
        }

        StringWriter writer = new StringWriter();

        mustache.execute(writer, variables);
        writer.flush();
        return writer.toString();
    }

    private static void validateVariables(Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            validateValue(entry.getValue());
        }
    }

    private static void validateVariables(Collection<?> variables) {
        for (Object variable : variables) {
            validateValue(variable);
        }
    }

    private static void validateValue(Object variable) {
        if (isSimpleType(variable)) {
            return;
        }
        if (variable instanceof Collection) {
            validateVariables((Collection<?>) variable);
        } else if (variable instanceof Map) {
            //noinspection unchecked
            validateVariables((Map<String, Object>) variable);
        } else {
            throw new FreeplayClientException(String.format(
                    "Unsupported type provided as input variable: %s", variable.getClass().getName()));
        }
    }

    private static boolean isSimpleType(Object value) {
        return value instanceof String ||
                value instanceof Number ||
                value instanceof Boolean;
    }

    private static class JSONObjectSerializingObjectHandler extends ReflectionObjectHandler {
        ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String stringify(Object object) {
            if (object == null) {
                return "";
            }

            // Check if the object is a primitive type, its wrapper, or a String
            if (object instanceof Number || object instanceof Boolean || object instanceof Character || object instanceof CharSequence) {
                return object.toString();
            }

            // Otherwise, serialize to JSON for object types
            try {
                return objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new FreeplayClientException("Error formatting template.", e);
            }
        }
    }

    private static class NoEscapeMustacheFactory extends DefaultMustacheFactory {
        @Override
        public void encode(String value, Writer writer) {
            try {
                writer.write(value);
            } catch (IOException e) {
                throw new FreeplayClientException("Error formatting template.", e);
            }
        }

        @Override
        public Mustache compilePartial(String s) {
            return new NoOpMustache();
        }
    }

    public static class NoOpMustache implements Mustache {

        @Override
        public void append(String text) {

        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public Object clone() {
            return new NoOpMustache();
        }

        @Override
        public Writer execute(Writer writer, List<Object> scopes) {
            return writer;
        }

        @Override
        public Code[] getCodes() {
            return new Code[0];
        }

        @Override
        public void identity(Writer writer) {
        }

        @Override
        public void init() {
        }

        @Override
        public Object clone(Set<Code> seen) {
            return new NoOpMustache();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Node invert(Node node, String text, AtomicInteger position) {
            return node;
        }

        @Override
        public void setCodes(Code[] codes) {

        }

        @Override
        public Writer run(Writer writer, List<Object> scopes) {
            return writer;
        }

        @Override
        public Node invert(String text) {
            return null;
        }
    }
}
