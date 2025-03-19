package io.github.tiagobohnenberger.cli.command;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.github.tiagobohnenberger.cli.core.Eager;
import io.github.tiagobohnenberger.cli.util.Functions;
import io.github.tiagobohnenberger.fntry.Try;
import lombok.extern.log4j.Log4j2;

import static io.github.tiagobohnenberger.cli.util.Functions.println;


@Log4j2
@Eager(priority = 2)
@ApplicationScoped
class MainConsole {

    private final UserInputHandler userInputHandler;

    @Inject
    private MainConsole(UserInputHandler userInputHandler) {
        this.userInputHandler = userInputHandler;
    }

    private void welcome() {
        Try.with("* ADOPET APP //")
                .consume(Functions::printAnsiArt)
                .otherwise(ex -> log.warn("ERROR on ANSI art", ex));

        menu();

        userInputHandler.start();
    }

    static void menu() {
        println("Escolha uma opção:");
        println("1 -> Listar abrigos cadastrados");
        println("2 -> Cadastrar novo abrigo");
        println("3 -> Listar pets do abrigo");
        println("4 -> Importar pets do abrigo");
        println("5 -> Sair");
        println("Digite o número da opção e pressione Enter");
        println();
    }


    @PostConstruct
    private void postConstruct() {
        welcome();
    }

}