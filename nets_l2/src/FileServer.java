import java.io.IOException;
import java.io.File;
import java.net.*;

public class FileServer {
    public static void main(String[] args) {
        try {
            int port = 12345;
            FileServer server = new FileServer();
            server.runServer(port);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: порт должен быть числом");
        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runServer(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) { //слушает входящие соединения
            System.out.println("Сервер запущен на порту: " + port);

            File uploadsDir = new File("uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdir()) {
                    throw new IOException("Не удалось создать директорию uploads");
                }
                System.out.println("Создана директория uploads/");
            }

            System.out.println("Ожидание подключения клиентов...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("\n[Новое подключение] Клиент: " +
                            clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                    Thread clientThread = new Thread(new ClientHandler(clientSocket, uploadsDir));
                    clientThread.start();
                } catch (IOException e) {
                    System.err.println("Ошибка при приёме клиента: " + e.getMessage());
                }
            }
        }
    }
}
