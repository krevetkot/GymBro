package ru.itmo.gymbro.catalog.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("gyms")
public class Gym {

    @Id
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 300)
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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    private static String checkText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Gym gym)) {
            return false;
        }
        return city.equals(gym.city) && address.equals(gym.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, address);
    }

    @Override
    public String toString() {
        return "Gym{id=" + id + ", name=" + name + ", city=" + city + ", address=" + address + "}";
    }
}
