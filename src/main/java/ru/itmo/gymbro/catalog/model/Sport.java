package ru.itmo.gymbro.catalog.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "sports")
@Getter
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    protected Sport() {
    }

    public Sport(Long id, String name) {
        this.id = id;
        this.name = checkName(name);
    }

    public static Sport of(String name) {
        return new Sport(null, name);
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
