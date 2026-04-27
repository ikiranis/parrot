package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity(name = "MediaFile")
@Table(name = "media_file")
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "path", nullable = false)
    private String path;

    @NotBlank
    @Size(max = 255)
    @Column(name = "filename", nullable = false)
    private String filename;

    @Size(max = 100)
    @Column(name = "hash")
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private MediaKind kind;

    public MediaFile() {
    }

    public MediaFile(String path, String filename, String hash, MediaKind kind) {
        this.path = path;
        this.filename = filename;
        this.hash = hash;
        this.kind = kind;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public MediaKind getKind() {
        return kind;
    }

    public void setKind(MediaKind kind) {
        this.kind = kind;
    }
}
