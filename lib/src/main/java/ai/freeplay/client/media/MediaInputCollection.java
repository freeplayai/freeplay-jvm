package ai.freeplay.client.media;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MediaInputCollection {
    private final Map<String, MediaInput> inputs;

    public MediaInputCollection() {
        this.inputs = new HashMap<>();
    }

    public MediaInput put(String name, MediaInput input) {
        return inputs.put(name, input);
    }

    public Optional<MediaInput> get(String name) {
        return Optional.ofNullable(inputs.get(name));
    }

    public Set<Map.Entry<String, MediaInput>> entries() {
        return inputs.entrySet();
    }
}
