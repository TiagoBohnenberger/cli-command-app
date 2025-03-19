package io.github.tiagobohnenberger.cli.core;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.tiagobohnenberger.cli.util.Conditional;
import io.github.tiagobohnenberger.fntry.Try;

import static io.github.tiagobohnenberger.cli.util.Functions.print;

public class ConsoleReader implements Runnable, Closeable, Conditional {
    private static volatile ConsoleReader INSTANCE;

    private final UserInputHolder holder;
    private final ScannerWrapper scannerWrapper;

    private ConsoleReader(UserInputHolder holder) {
        this.holder = holder;
        this.scannerWrapper = ScannerWrapper.instance();
    }

    public static ConsoleReader instance() {
        var instance = INSTANCE;
        if (instance == null) {
            synchronized (ConsoleReader.class) {
                instance = INSTANCE;
                if (instance == null) {
                    instance = INSTANCE = new ConsoleReader(new UserInputHolder());
                }
            }
        }
        return instance;
    }

    @Override
    public void run() {
        while (holder.isNotExit()) {
            print("Opção: ");
            int userInput = Try.of(
                    () -> Integer.parseInt(scannerWrapper.nextLine())
            ).orElse(-1);

            if (userInput == -1) {
                continue;
            }

            holder.put(userInput);

            synchronized (this) {
                this.notifyAll();

                if (this.isExit()) break;

                Try.just(this::wait);
            }

        }
    }

    @Override
    public void close() {
        scannerWrapper.close();
    }

    public Integer poll() {
        return holder.poll();
    }

    public boolean isNotExit() {
        return holder.isNotExit();
    }

    @Override
    public boolean satisfy() {
        return holder.noResult();
    }

    public String nextLine() {
        return scannerWrapper.nextLine();
    }

    public boolean isExit() {
        return !isNotExit();
    }

    private static class UserInputHolder implements Conditional {
        final AtomicInteger result = new AtomicInteger(0);


        boolean noResult() {
            return !containsResult();
        }

        boolean containsResult() {
            return result.get() != 0;
        }

        void put(int value) {
            result.set(value);
        }

        boolean isNotExit() {
            return result.get() != 5;
        }

        int poll() {
            return result.getAndSet(0);
        }

        @Override
        public boolean satisfy() {
            return noResult();
        }
    }
}