package io.github.tiagobohnenberger.cli.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.github.tiagobohnenberger.cli.command.CLICommand;
import io.github.tiagobohnenberger.cli.command.CommandKeyAlreadyRegisteredException;
import io.github.tiagobohnenberger.cli.command.CommandOption;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;

@Log4j2
@Eager(priority = Eager.Priority.HIGHEST)
@ApplicationScoped
public class CommandOptionsRegistry {
    private final Map<Integer, Command> container = new HashMap<>();
    private final Instance<Object> commands;

    @Inject
    private CommandOptionsRegistry(@CLICommand Instance<Object> commands) {
        this.commands = commands;
        registerCommandBeans();
    }

    public Optional<Command> get(Integer userOption) {
        return Optional.ofNullable(container.get(userOption));
    }

    private void registerCommandBeans() {
        commands.forEach(this::registerCommandMethod);
        log.debug("Command options registry created");
    }

    private void registerCommandMethod(Object commandBean) {
        for (Method method : commandBean.getClass().getDeclaredMethods()) {
            CommandOption commandOption = method.getAnnotation(CommandOption.class);
            if (commandOption != null) {

                int commandOptionValue = commandOption.value();
                if (container.containsKey(commandOptionValue)) {
                    throw new CommandKeyAlreadyRegisteredException("Chave com valor \"" + commandOptionValue + "\" já registrada. Utilizar uma chave diferente.");
                }

                container.put(commandOptionValue, new Command(commandBean, method, commandOptionValue));
            }
        }
    }


    public record Command(Object bean, @Nullable Method method, int commandOptionValue) {

        public void execute() {
            if (method != null) {
                method.setAccessible(true);
                try {
                    method.invoke(bean);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    final var exception = ObjectUtils.defaultIfNull(e.getCause(), e);
                    log.error("""
                            Erro ao executar comando "{}"
                            """, commandOptionValue, exception);
                } catch (Exception e) {
                    log.error("""
                            Erro genérico ao executar o comando "{}"
                            """, commandOptionValue, e);
                }
            }
        }
    }
}