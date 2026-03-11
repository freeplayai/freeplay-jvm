package ai.freeplay.client.adapters;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.resources.prompts.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class RoleSupport {
    public static final RoleSupport DEFAULT = new RoleSupport(
            Set.of("system", "user", "assistant"),
            Map.of()
    );

    public static final RoleSupport OPENAI = new RoleSupport(
            Set.of("system", "user", "assistant", "tool"),
            Map.of("developer", "system")
    );

    public static final RoleSupport OPENAI_RESPONSES = new RoleSupport(
            Set.of("system", "user", "assistant", "developer", "tool"),
            Map.of()
    );

    public static final RoleSupport GEMINI = new RoleSupport(
            Set.of("system", "user", "assistant", "model"),
            Map.of()
    );

    private static final System.Logger LOGGER = System.getLogger(RoleSupport.class.getName());

    private final Set<String> supported;
    private final Map<String, String> coerceMap;

    public RoleSupport(Set<String> supported, Map<String, String> coerceMap) {
        this.supported = Collections.unmodifiableSet(supported);
        this.coerceMap = Collections.unmodifiableMap(coerceMap);
    }

    public Set<String> getSupported() {
        return supported;
    }

    public Map<String, String> getCoerceMap() {
        return coerceMap;
    }

    public static List<ChatMessage> prepareMessages(
            List<ChatMessage> messages,
            RoleSupport roleSupport,
            String flavorName
    ) {
        List<ChatMessage> result = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            String role = message.getRole();
            if (roleSupport.supported.contains(role)) {
                result.add(message);
            } else {
                String coerced = roleSupport.coerceMap.get(role);
                if (coerced != null) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Role ''{0}'' is not natively supported by this flavor. Coercing to ''{1}''.",
                            role, coerced);
                    result.add(message.withRole(coerced));
                } else {
                    throw new FreeplayConfigurationException(format(
                            "Role '%s' is not supported by %s flavor. " +
                            "Please update your prompt template in Freeplay to use a flavor that supports the '%s' role.",
                            role, flavorName, role
                    ));
                }
            }
        }
        return result;
    }
}
