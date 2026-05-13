package net.diveon.backend.domain.group.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "group_tag",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_GROUP_TAG_UNIQUE", columnNames = {"group_id", "tag"})
    }
)
public class GroupTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @Column(name = "tag", nullable = false, length = 50)
    private String tag;

    public GroupTag() {
    }

    public GroupTag(Group group, String tag) {
        this.group = group;
        this.tag = tag;
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public String getTag() { return tag; }
}
