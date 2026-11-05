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

@Table(name = "gyms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Gym {

    @Id
    @ToString.Include
    private Long id;

    @NotBlank
    @Size(max = 200)
    @ToString.Include
    private String name;

    @NotBlank
    @Size(max = 100)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String city;

    @NotBlank
    @Size(max = 300)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String address;

    public Gym(Long id, String name, String city, String address) {
        this.id = id;
        this.name = checkText(name, "Название зала обязательно");
        this.city = checkText(city, "Город обязателен");
        this.address = checkText(address, "Адрес обязателен");
    }

    public static Gym of(String name, String city, String address) {
        return new Gym(null, name, city, address);
    }

    public void relocate(String newCity, String newAddress) {
        this.city = checkText(newCity, "Город обязателен");
        this.address = checkText(newAddress, "Адрес обязателен");
    }

    public void rename(String newName) {
        this.name = checkText(newName, "Название зала обязательно");
    }

    private static String checkText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
