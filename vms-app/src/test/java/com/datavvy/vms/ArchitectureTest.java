package com.datavvy.vms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Module boundary rules. These are what let several teams work in one repo
 * without stepping on each other: a module may use another module only
 * through its {@code api} package. Fails the build on violation.
 */
class ArchitectureTest {

    private static final String ROOT = "com.datavvy.vms";
    private static final List<String> MODULES = List.of(
            "auth", "catalog", "vehicle", "document", "maintenance", "driver", "dashboard");
    private static final String[] INTERNAL = {"domain", "application", "web", "config"};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @TestFactory
    List<DynamicTest> modulesUseEachOtherOnlyThroughApi() {
        return MODULES.stream()
                .flatMap(from -> MODULES.stream().filter(to -> !to.equals(from)).map(to -> DynamicTest.dynamicTest(
                        from + " -> " + to + " only via api",
                        () -> noClasses().that().resideInAPackage(ROOT + "." + from + "..")
                                .should().dependOnClassesThat().resideInAnyPackage(internalOf(to))
                                .because("modules talk to each other only through their api package (ADR-0002)")
                                .check(classes))))
                .toList();
    }

    @Test
    void platformDependsOnNoModule() {
        noClasses().that().resideInAPackage(ROOT + ".platform..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        MODULES.stream().map(m -> ROOT + "." + m + "..").toArray(String[]::new))
                .check(classes);
    }

    @Test
    void apiPackagesDoNotLeakInternals() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..application..", "..web..")
                .because("api types are the published contract; entities stay private to their module")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnWeb() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..web..", "..application..")
                .check(classes);
    }

    @Test
    void controllersLiveInWebPackages() {
        classes().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..web..")
                .check(classes);
    }

    private static String[] internalOf(String module) {
        String[] pkgs = new String[INTERNAL.length];
        for (int i = 0; i < INTERNAL.length; i++) {
            pkgs[i] = ROOT + "." + module + "." + INTERNAL[i] + "..";
        }
        return pkgs;
    }
}
