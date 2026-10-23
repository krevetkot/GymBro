package ru.itmo.gymbro.catalog.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogModelTest {

    @Test
    @DisplayName("Виды спорта равны по названию без учёта пробелов по краям")
    void sportsAreEqualByName() {
        Sport sport = new Sport(1L, " Бег ");
        Sport same = Sport.of("Бег");

        assertThat(sport).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(Sport.of("Йога"));
        assertThat(sport).isEqualTo(sport).isNotEqualTo(null).isNotEqualTo("Бег");
        assertThat(sport.toString()).contains("id=1", "name=Бег");
        assertThatThrownBy(() -> Sport.of(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Залы равны по городу и адресу, название не важно")
    void gymsAreEqualByCityAndAddress() {
        Gym gym = new Gym(1L, "Атлант", "Москва", "Ленина, 1");
        Gym sameAddress = Gym.of("Другой", " Москва ", " Ленина, 1 ");

        assertThat(gym).isEqualTo(sameAddress).hasSameHashCodeAs(sameAddress)
                .isNotEqualTo(Gym.of("Атлант", "Москва", "Ленина, 2"));
        assertThat(gym).isEqualTo(gym).isNotEqualTo(null).isNotEqualTo("Атлант");
        assertThat(gym.toString()).contains("name=Атлант", "city=Москва", "address=Ленина, 1");
    }

    @Test
    @DisplayName("Зал можно переименовать и перенести, пустые значения запрещены")
    void gymChangesKeepInvariants() {
        Gym gym = Gym.of("Атлант", "Москва", "Ленина, 1");

        gym.rename(" Атлант 2 ");
        gym.relocate(" Казань ", " Баумана, 5 ");

        assertThat(gym.getName()).isEqualTo("Атлант 2");
        assertThat(gym.getCity()).isEqualTo("Казань");
        assertThat(gym.getAddress()).isEqualTo("Баумана, 5");
        assertThatThrownBy(() -> gym.rename(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gym.relocate("Казань", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Заявки равны только по id; новые заявки без id равны лишь самим себе")
    void requestsAreEqualById() {
        GymRequest stored = new GymRequest(5L, 7, "Атлант", "Москва", "Ленина, 1",
                RequestStatus.PENDING, null, null, null);
        GymRequest sameId = new GymRequest(5L, 8, "Другой", "Казань", "Баумана, 5",
                RequestStatus.PENDING, null, null, null);
        GymRequest fresh = GymRequest.submit(7, "Атлант", "Москва", "Ленина, 1");

        assertThat(stored).isEqualTo(sameId).hasSameHashCodeAs(sameId);
        assertThat(fresh).isEqualTo(fresh).isNotEqualTo(GymRequest.submit(7, "Атлант", "Москва", "Ленина, 1"));
        assertThat(stored).isNotEqualTo(null).isNotEqualTo("заявка");
        assertThat(stored.toString()).contains("id=5", "author=7", "status=PENDING");
        assertThat(stored.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Заявка рассматривается один раз; одобренная ссылается на зал, отклонённая — нет")
    void requestReviewRules() {
        GymRequest approved = GymRequest.submit(7, "Атлант", "Москва", "Ленина, 1");
        approved.approve(10, 20);
        GymRequest rejected = GymRequest.submit(7, "Атлант", "Москва", "Ленина, 2");
        rejected.reject(10);

        assertThat(approved.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(approved.getGymId()).isEqualTo(20);
        assertThat(rejected.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(rejected.getGymId()).isNull();
        assertThatThrownBy(() -> approved.reject(10)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new GymRequest(null, 7, "Атлант", "Москва", "Ленина, 1",
                RequestStatus.APPROVED, 10L, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GymRequest(null, 0, "Атлант", "Москва", "Ленина, 1",
                RequestStatus.PENDING, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
