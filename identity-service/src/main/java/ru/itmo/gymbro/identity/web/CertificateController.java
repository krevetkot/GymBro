package ru.itmo.gymbro.identity.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.CertificateUseCases;
import ru.itmo.gymbro.identity.dto.CertificateResponse;
import ru.itmo.gymbro.identity.dto.SubmitCertificateRequest;
import ru.itmo.gymbro.identity.model.CertificateStatus;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@Tag(name = "Сертификаты тренеров",
        description = "Пользователь прикладывает сертификат или диплом, администратор его верифицирует. "
                + "После одобрения пользователь получает роль TRAINER.")
@ApiResponse(responseCode = "400", description = "Некорректные поля или параметры запроса")
@ApiResponse(responseCode = "401", description = "Требуется действительный Bearer-токен")
@ApiResponse(responseCode = "403", description = "Недостаточно прав или пользователь заблокирован")
@RequiredArgsConstructor
class CertificateController {

    private static final String TOTAL_COUNT_DESCRIPTION = "Общее количество сертификатов";

    private final CertificateUseCases certificates;

    @PostMapping
    @Operation(summary = "Приложить сертификат",
            description = "Для USER и TRAINER. Сертификат создаётся в статусе PENDING; по одному виду спорта "
                    + "одновременно на проверке может быть только один сертификат.")
    @ApiResponse(responseCode = "201", description = "Сертификат отправлен на проверку")
    @ApiResponse(responseCode = "409", description = "Сертификат по этому виду спорта уже ждёт проверки")
    Mono<ResponseEntity<CertificateResponse>> submit(@Valid @RequestBody SubmitCertificateRequest request) {
        return certificates.submit(request.sportId(), request.fileUrl())
                .map(CertificateResponse::from)
                .map(response -> ResponseEntity.created(URI.create("/api/v1/certificates/" + response.id()))
                        .body(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Мои сертификаты", description = "Сначала новые. size по умолчанию 20, максимум 50.")
    @ApiResponse(responseCode = "200", description = "Страница сертификатов текущего пользователя",
            headers = @Header(name = "X-Total-Count", description = TOTAL_COUNT_DESCRIPTION,
                    schema = @Schema(type = "integer", format = "int64")))
    Mono<ResponseEntity<List<CertificateResponse>>> getMine(@ParameterObject Pageable pageable) {
        return certificates.getMine(pageable)
                .map(page -> PageResponses.withTotalCount(page.map(CertificateResponse::from)));
    }

    @GetMapping
    @Operation(summary = "Сертификаты на проверку (только ADMIN)",
            description = "Сначала новые; status — фильтр, например PENDING. Общее количество — в X-Total-Count.")
    @ApiResponse(responseCode = "200", description = "Страница сертификатов",
            headers = @Header(name = "X-Total-Count", description = TOTAL_COUNT_DESCRIPTION,
                    schema = @Schema(type = "integer", format = "int64")))
    Mono<ResponseEntity<List<CertificateResponse>>> getPage(
            @Parameter(description = "Фильтр по статусу") @RequestParam(required = false) CertificateStatus status,
            @ParameterObject Pageable pageable) {
        return certificates.getPage(status, pageable)
                .map(page -> PageResponses.withTotalCount(page.map(CertificateResponse::from)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Одобрить сертификат (только ADMIN)",
            description = "В одной транзакции сертификат получает статус APPROVED, а автор — роль TRAINER. "
                    + "Новая роль появится в токене после повторного входа.")
    @ApiResponse(responseCode = "200", description = "Сертификат одобрен")
    @ApiResponse(responseCode = "404", description = "Сертификат не найден")
    @ApiResponse(responseCode = "409", description = "Сертификат уже рассмотрен или автор заблокирован")
    Mono<CertificateResponse> approve(@PathVariable @Positive long id) {
        return certificates.approve(id).map(CertificateResponse::from);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Отклонить сертификат (только ADMIN)")
    @ApiResponse(responseCode = "200", description = "Сертификат отклонён")
    @ApiResponse(responseCode = "404", description = "Сертификат не найден")
    @ApiResponse(responseCode = "409", description = "Сертификат уже рассмотрен")
    Mono<CertificateResponse> reject(@PathVariable @Positive long id) {
        return certificates.reject(id).map(CertificateResponse::from);
    }
}
