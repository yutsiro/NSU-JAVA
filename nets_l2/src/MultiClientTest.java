import java.io.*;
import java.util.concurrent.*;

public class MultiClientTest {
    public static void main(String[] args) throws Exception {
        String server = "localhost";
        int port = 12345;
        int clientCount = 3;

        createTestFiles(clientCount);

        try (ExecutorService executor = Executors.newFixedThreadPool(clientCount)) {

            System.out.println("Запуск " + clientCount + " клиентов параллельно...\n");

            for (int i = 1; i <= clientCount; i++) {
                final int clientNum = i;
                executor.submit(() -> {
                    try {
                        System.out.println("Клиент " + clientNum + " начал работу");
                        File file = new File("test" + clientNum + ".txt");
                        FileClient client = new FileClient();
                        client.sendFile(server, port, file);
                        System.out.println("Клиент " + clientNum + " завершил работу");
                    } catch (IOException e) {
                        System.err.println("Клиент " + clientNum + " ошибка: " + e.getMessage());
                    }
                });

                //задержка между запусками
                Thread.sleep(300);
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }

        System.out.println("\nВсе клиенты завершены");
    }

    private static void createTestFiles(int count) throws IOException {
        for (int i = 1; i <= count; i++) {
            File file = new File("test" + i + ".txt");
            try (FileWriter writer = new FileWriter(file)) {
                // файл размером около 1 МБ для заметной передачи
                for (int j = 0; j < 10000; j++) {
                    writer.write("Это тестовая строка номер " + j + " для файла " + i + "\n");
                }
            }
            System.out.println("Создан файл: " + file.getName() + " (" + file.length() + " байт)");
        }
        System.out.println();
    }
}
