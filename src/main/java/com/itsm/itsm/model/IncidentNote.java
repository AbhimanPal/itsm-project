package com.itsm.itsm.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// A note always belongs to one incident (composition): it is created only
// through Incident.addNote() and is deleted together with its incident.
@Entity
public class IncidentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Incident incident;

    @ManyToOne(optional = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteType type;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected IncidentNote() {
    }

    // Package-private: only classes in the model package (i.e. Incident) can create notes
    IncidentNote(Incident incident, User author, NoteType type, String text) {
        this.incident = incident;
        this.author = author;
        this.type = type;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public NoteType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
