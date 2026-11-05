package ru.itmo.gymbro.catalog.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Table(name = "sports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Sport {

    @Id
    @ToString.Include
    private Long id;

    @NotBlank
    @Size(max = 100)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String name;

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
}
