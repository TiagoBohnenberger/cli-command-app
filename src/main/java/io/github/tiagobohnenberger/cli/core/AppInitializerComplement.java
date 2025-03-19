package io.github.tiagobohnenberger.cli.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;

import io.github.tiagobohnenberger.cli.util.ClassUtils;
import io.github.tiagobohnenberger.cli.util.Functions;
import io.github.tiagobohnenberger.fntry.Try;
import lombok.extern.log4j.Log4j2;

@Log4j2
@SuppressWarnings("unused")
class AppInitializerComplement implements Extension {
    private static final Set<Class<? extends Annotation>> annotations = Set.of(Eager.class, ApplicationScoped.class);

    private final List<Bean<?>> eagerBeansList = new ArrayList<>();

    private static <T> boolean isAnnotationsPresentOn(ProcessBean<T> bean) {
        return annotations.stream().allMatch(ann ->
                bean.getAnnotated().isAnnotationPresent(ann));
    }

    private <T> void init(@Observes ProcessBean<T> bean) {
        if (isAnnotationsPresentOn(bean)) {
            eagerBeansList.add(bean.getBean());
        }
    }

    private void onApplicationStartup(@Observes Startup event, BeanManager beanManager, Properties properties) throws IOException, URISyntaxException {
        loadExternalConfigurations(properties);
        sortByHighestPrecedence();
        initEagerBeans(beanManager);
    }

    private void loadExternalConfigurations(Properties properties) throws IOException, URISyntaxException {
        String path = "application.properties";
        ClassLoader cl = ClassUtils.getDefaultClassLoader();

        if (cl == null) {
            return;
        }

        try (InputStream externalConfig = cl.getResourceAsStream(path)) {
            if (externalConfig != null) {
                properties.load(new InputStreamReader(externalConfig));
                log.debug("External config file \"{}\" loaded", path);
                return;
            }
        }
        URL url = cl.getResource(path);

        if (url == null) {
            return;
        }

        Path path1 = Paths.get(url.toURI());

        properties.load(Files.newBufferedReader(path1));
    }

    private void beforeShutdown(@Observes BeforeShutdown event) {
        Try.with("* ATÉ MAIS //")
                .consume(__ -> System.out.println())
                .consume(Functions::printAnsiArt)
                .orElse(Functions::println);

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initEagerBeans(BeanManager beanManager) {
        for (Bean<?> bean : eagerBeansList) {
            // note: toString() is important to instantiate the bean
            beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
        }
    }

    private void sortByHighestPrecedence() {
        eagerBeansList.sort(Comparator.comparingInt(bean ->
                bean.getBeanClass().getAnnotation(Eager.class).priority()));
    }
}