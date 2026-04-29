package net.diveon.backend.domain.problem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "obo_templates")
public class OboTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "whiteboard_data", columnDefinition = "TEXT", nullable = false)
    private String whiteboardData;

    @Column(name = "mappings", columnDefinition = "TEXT", nullable = false)
    private String mappings;

    public OboTemplate() {
    }

    public OboTemplate(String name, String whiteboardData, String mappings) {
        this.name = name;
        this.whiteboardData = whiteboardData;
        this.mappings = mappings;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWhiteboardData() {
        return whiteboardData;
    }

    public String getMappings() {
        return mappings;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWhiteboardData(String whiteboardData) {
        this.whiteboardData = whiteboardData;
    }

    public void setMappings(String mappings) {
        this.mappings = mappings;
    }
}
