package ru.itmo.gymbro;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "ru.itmo.gymbro", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "ru.itmo.gymbro";

    @ArchTest
    static final ArchRule modelKnowsNothingAboutOuterLayers = noClasses()
            .that().resideInAPackage(ROOT + "..model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "..repository..",
                    ROOT + "..service..",
                    ROOT + "..web..",
                    ROOT + "..dto..",
                    "org.springframework.web..")
            .as("сущности не зависят от репозиториев, сервисов и веба")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule serviceKnowsNothingAboutWeb = noClasses()
            .that().resideInAPackage(ROOT + "..service..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "..web..",
                    "org.springframework.web..")
            .as("бизнес-логика не зависит от HTTP")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersLiveInWebPackage = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..web..")
            .as("контроллеры лежат только в пакете web")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule identityInternalsAreClosed = internalsAreClosed("identity");

    @ArchTest
    static final ArchRule catalogInternalsAreClosed = internalsAreClosed("catalog");

    @ArchTest
    static final ArchRule profileInternalsAreClosed = internalsAreClosed("profile");

    @ArchTest
    static final ArchRule matchingInternalsAreClosed = internalsAreClosed("matching");

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles()
            .as("между модулями нет циклических зависимостей")
            .allowEmptyShould(true);

    private static ArchRule internalsAreClosed(String module) {
        String modulePackage = ROOT + "." + module;
        return noClasses()
                .that().resideOutsideOfPackage(modulePackage + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        modulePackage + ".service..",
                        modulePackage + ".repository..",
                        modulePackage + ".web..",
                        modulePackage + ".dto..")
                .as("модуль %s доступен снаружи только через %s.api и %s.model"
                        .formatted(module, modulePackage, modulePackage))
                .allowEmptyShould(true);
    }
}
