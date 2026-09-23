package ru.itmo.gymbro.shared.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SliceResponseTest {

    @Test
    @DisplayName("Срез переносит элементы, номер страницы и признак продолжения")
    void copiesSliceState() {
        SliceImpl<String> slice = new SliceImpl<>(List.of("a", "b"), PageRequest.of(3, 2), true);

        SliceResponse<String> response = SliceResponse.from(slice);

        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getPage()).isEqualTo(3);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("Последний срез сообщает, что продолжения нет")
    void reportsLastSlice() {
        SliceImpl<String> slice = new SliceImpl<>(List.of("z"), PageRequest.of(0, 20), false);

        assertThat(SliceResponse.from(slice).isHasNext()).isFalse();
    }

    @Test
    @DisplayName("В JSON нет общего количества записей")
    void serializesWithoutTotalCount() {
        SliceImpl<String> slice = new SliceImpl<>(List.of("a"), PageRequest.of(0, 1), true);

        JsonNode json = JsonMapper.builder().build().valueToTree(SliceResponse.from(slice));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder("items", "page", "size", "hasNext");
        assertThat(json.get("hasNext").asBoolean()).isTrue();
    }
}
