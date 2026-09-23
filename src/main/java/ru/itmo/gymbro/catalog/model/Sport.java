package ru.itmo.gymbro.catalog.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("sports")
public class Sport {

    @Id
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    public Sport(Long id, String name) {
        this.id = id;
        this.name = checkName(name);
    }

    public static Sport of(String name) {
        return new Sport(null, name);
    }

    public void rename(String newName) {
        this.name = checkName(newName);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private static String checkName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Название вида спорта обязательно");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Sport sport)) {
            return false;
        }
        return name.equals(sport.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Sport{id=" + id + ", name=" + name + "}";
    }
}
