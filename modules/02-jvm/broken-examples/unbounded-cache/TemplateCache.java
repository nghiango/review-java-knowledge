package lab.jvm.broken.unboundedcache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class TemplateCache {
    private static final Logger LOGGER = Logger.getLogger(TemplateCache.class.getName());
    private static final Map<String, String> RENDERED_TEMPLATES = new ConcurrentHashMap<>();

    public String render(String tenantId, String templateName, Map<String, String> model) {
        String key = tenantId + ":" + templateName + ":" + model;
        return RENDERED_TEMPLATES.computeIfAbsent(key, this::renderTemplate);
    }

    public int size() {
        return RENDERED_TEMPLATES.size();
    }

    private String renderTemplate(String key) {
        LOGGER.info(() -> "rendering template " + key);
        return "<html><body>" + key.repeat(128) + "</body></html>";
    }
}
