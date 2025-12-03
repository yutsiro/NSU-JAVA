import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileClient {
    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//        System.out.print("Введите путь к файлу: ");
//        String filePath = scanner.nextLine().trim();
//
//        System.out.print("Введите адрес сервера: ");
//        String serverInput = scanner.nextLine().trim();
//        String server = serverInput.isEmpty() ? "localhost" : serverInput;
//
//        System.out.print("Введите порт сервера: ");
//        String portInput = scanner.nextLine().trim();
//        int port = portInput.isEmpty() ? 12345 : Integer.parseInt(portInput);
//
//        scanner.close();


//        String filePath = "C:\\Users\\yutsiro\\IdeaProjects\\nets_l2\\src\\test.txt";
        String filePath = "src\\image1.jpg";
        String server = "localhost";
        int port = 12345;

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("Файл не найден: " + filePath);
            return;
        }

        FileClient client = new FileClient();
        try {
            client.sendFile(server, port, file);
        } catch (IOException e) {
            System.err.println("Ошибка при отправке файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendFile(String server, int port, File file) throws IOException {
        System.out.println("Подключение к серверу " + server + ":" + port);

        try (
                Socket socket = new Socket(server, port);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                FileInputStream fis = new FileInputStream(file);
        ) {
            System.out.println("Соединение установлено");

            //длина имени файла 4 байта
            String fileName = file.getName();
            byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);

            ByteBuffer nameLengthBuffer = ByteBuffer.allocate(4);
            nameLengthBuffer.putInt(nameBytes.length);
            out.write(nameLengthBuffer.array());
//            out.write(intToBytes(nameBytes.length));

            //имя файла
            out.write(nameBytes);
            System.out.println("Отправка файла: " + fileName);

            //размер файла 8 байт
            long fileSize = file.length();
            ByteBuffer fileSizeBuffer = ByteBuffer.allocate(8);
            fileSizeBuffer.putLong(fileSize);
            out.write(fileSizeBuffer.array());
//            out.write(longToBytes(fileSize));
            System.out.println("Размер файла: " + fileSize + " байт");

            //содержимое файла передаем через буфер по частям
            byte[] buffer = new byte[8192];
            long totalSent = 0;
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalSent += bytesRead;

                try {//слип для задержки и теста мультитрединга
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                //считаем прогресс
                if (fileSize > 0) {
                    int progress = (int)((totalSent * 100) / fileSize);
                    System.out.println("\rПрогресс: " + progress + "%");
                }
            }
            System.out.println("\nОтправлено байт: " + totalSent);

            out.flush();

            readResult(in);

        }
    }

    public void readResult(InputStream in) throws IOException {
        int result = in.read();

        if (result == 1) {
            System.out.println("\n✓ Файл успешно передан и сохранён на сервере");
        } else if (result == 0) {
            System.out.println("\n✗ Ошибка: файл не был сохранён на сервере");
        } else {
            System.out.println("\n✗ Неожиданный ответ от сервера");
        }
    }

    //преобразование int в 4 байта (Big Endian)
    private byte[] intToBytes(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value
        };
    }

    //преобразование long в 8 байт (Big Endian)
    private byte[] longToBytes(long value) {
        return new byte[] {
                (byte)(value >> 56),
                (byte)(value >> 48),
                (byte)(value >> 40),
                (byte)(value >> 32),
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value
        };
    }
}
