package ru.mirea.cargograph;

/** Application entry point. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            new Application().run(options);
        } catch (IllegalArgumentException | java.io.IOException ex) {
            System.err.println("Ошибка: " + ex.getMessage());
            System.exit(2);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("Загрузка прервана.");
            System.exit(130);
        }
    }
}
