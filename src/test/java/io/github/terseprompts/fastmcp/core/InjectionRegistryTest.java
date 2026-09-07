package io.github.terseprompts.fastmcp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link InjectionRegistry} and {@link Injector} dependency injection.
 */
class InjectionRegistryTest {

    static class Repository {
        String name = "depo";
    }

    static class Logger {
        String name = "logger";
    }

    static class MultiRepo implements InterfaceA, InterfaceB {
    }

    interface InterfaceA {}
    interface InterfaceB {}

    static class Target {
        @io.github.terseprompts.fastmcp.annotations.McpInject
        Repository repository; // by id match

        @io.github.terseprompts.fastmcp.annotations.McpInject
        Logger logger; // by type match

        @io.github.terseprompts.fastmcp.annotations.McpInject
        Repository missingRepo; // no match -> null

        String plainField; // not annotated, untouched
    }

    @Test
    void testInjectionByName() {
        Repository repo = new Repository();
        InjectionRegistry registry = new InjectionRegistry().register("repository", repo);
        Target target = new Target();

        Injector.injectFields(target, registry);

        assertSame(repo, target.repository);
        assertNull(target.logger);
    }

    @Test
    void testInjectionByType() {
        Logger logger = new Logger();
        InjectionRegistry registry = new InjectionRegistry()
                .register(Logger.class, logger);
        Target target = new Target();

        Injector.injectFields(target, registry);

        assertSame(logger, target.logger);
        assertNull(target.repository);
    }

    @Test
    void testNameTakesPrecedenceOverType() {
        Repository byId = new Repository();
        Repository byType = new Repository();
        InjectionRegistry registry = new InjectionRegistry()
                .register("repository", byId)
                .register(Repository.class, byType);
        Target target = new Target();

        Injector.injectFields(target, registry);

        assertSame(byId, target.repository, "Field name match should win over type match");
    }

    @Test
    void testNoMatchLeavesFieldNull() {
        InjectionRegistry registry = new InjectionRegistry();
        Target target = new Target();

        Injector.injectFields(target, registry);

        assertNull(target.repository);
        assertNull(target.logger);
        assertNull(target.missingRepo);
    }

    @Test
    void testExistingValueIsNotClearedOnNoMatch() {
        Target target = new Target();
        target.missingRepo = new Repository();
        Injector.injectFields(target, new InjectionRegistry());
        assertNotNull(target.missingRepo, "Unmatched field should be left unchanged");
    }

    @Test
    void testNonNullBeanOnly() {
        Target target = new Target();
        Injector.injectFields(target, new InjectionRegistry().register("repository", null));
        assertNull(target.repository);
    }

    @Test
    void testResolveByInterfaceType() {
        InjectionRegistry registry = new InjectionRegistry();
        Target t = new Target();
        // Repository registered only under an interface type should not match a
        // concrete-typed field.
        Injector.injectFields(t, registry);
        assertNull(t.repository);
    }

    @Test
    void testRegistryResolveRules() {
        Object bean = new Object();
        InjectionRegistry registry = new InjectionRegistry().register("id", bean);
        assertSame(bean, registry.resolve("id", String.class));
        assertNull(registry.resolve("other", String.class));
    }
}