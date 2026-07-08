package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public class OboPassThroughResponse {
    private JsonNode nodes;
    private JsonNode edges;
    private JsonNode frames;

    public OboPassThroughResponse() {
    }

    public OboPassThroughResponse(JsonNode nodes, JsonNode edges, JsonNode frames) {
        this.nodes = nodes;
        this.edges = edges;
        this.frames = frames;
    }

    public JsonNode getNodes() {
        return nodes;
    }

    public JsonNode getEdges() {
        return edges;
    }

    public JsonNode getFrames() {
        return frames;
    }

    public void setNodes(JsonNode nodes) {
        this.nodes = nodes;
    }

    public void setEdges(JsonNode edges) {
        this.edges = edges;
    }

    public void setFrames(JsonNode frames) {
        this.frames = frames;
    }
}
