package net.diveon.backend.domain.problem.dto.response;

import net.diveon.backend.domain.problem.entity.OboTemplate;
import java.util.List;

public class OboTemplateResponse {

    private final int status;
    private final String message;
    private final boolean enabled;
    private final List<OboTemplate> templates;

    public OboTemplateResponse(int status, String message, boolean enabled, List<OboTemplate> templates) {
        this.status = status;
        this.message = message;
        this.enabled = enabled;
        this.templates = templates;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<OboTemplate> getTemplates() {
        return templates;
    }
}
