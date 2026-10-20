package ru.itmo.gymbro.profile.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private UserRepository users;

    @Autowired
    private SportRepository sports;

    @Autowired
    private GymRepository gyms;

    @Test
    @DisplayName("Анкета сохраняется вместе с видами спорта и залами")
    void savesWholeAggregate() {
        long userId = newUser("profile1@mail.ru");
        long sportId = newSport("Бадминтон");
        long gymId = newGym("Тверская, 1");

        UserProfile profile = UserProfile.create(userId, "Ксения", LocalDate.of(2003, 5, 17), "люблю бегать");
        profile.replaceSports(Set.of(UserSport.of(sportId, SportLevel.BEGINNER)));
        profile.replaceGyms(Set.of(UserGym.of(gymId)));

        UserProfile saved = profiles.save(profile);
        assertThat(saved.getId()).isNotNull();

        UserProfile reloaded = profiles.findByUserId(userId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Ксения");
        assertThat(reloaded.getBirthDate()).isEqualTo(LocalDate.of(2003, 5, 17));
        assertThat(reloaded.getAbout()).isEqualTo("люблю бегать");
        assertThat(reloaded.getSports()).extracting(UserSport::getSportId).containsExactly(sportId);
        assertThat(reloaded.getSports().iterator().next().getLevel()).isEqualTo(SportLevel.BEGINNER);
        assertThat(reloaded.getGyms()).extracting(UserGym::getGymId).containsExactly(gymId);
    }

    @Test
    @DisplayName("Замена видов спорта переписывает коллекцию целиком")
    void replacesSports() {
        long userId = newUser("profile2@mail.ru");
        long running = newSport("Трейлраннинг");
        long swimming = newSport("Синхронное плавание");

        UserProfile profile = UserProfile.create(userId, "Анна", LocalDate.of(2000, 1, 1), null);
        profile.replaceSports(Set.of(UserSport.of(running, SportLevel.ADVANCED)));
        profiles.save(profile);

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        stored.replaceSports(Set.of(UserSport.of(swimming, SportLevel.BEGINNER)));
        profiles.save(stored);

        UserProfile reloaded = profiles.findByUserId(userId).orElseThrow();
        assertThat(reloaded.getSports()).extracting(UserSport::getSportId).containsExactly(swimming);
    }

    @Test
    @DisplayName("Анкета удаляется вместе с детьми")
    void deletesProfile() {
        long userId = newUser("profile4@mail.ru");
        long sportId = newSport("Керлинг");
        UserProfile profile = UserProfile.create(userId, "Вера", LocalDate.of(1998, 7, 7), null);
        profile.replaceSports(Set.of(UserSport.of(sportId, SportLevel.INTERMEDIATE)));
        profiles.save(profile);

        profiles.deleteByUserId(userId);

        assertThat(profiles.existsByUserId(userId)).isFalse();
    }

    private long newUser(String email) {
        return users.save(User.register(email, "hash")).getId();
    }

    private long newSport(String name) {
        return sports.save(Sport.of(name)).getId();
    }

    private long newGym(String address) {
        return gyms.save(Gym.of("Зал", "Москва", address)).getId();
    }
}
