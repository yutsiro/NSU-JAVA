import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private File uploadsDir;
    private SpeedMonitor speedMonitor;
    private String clientId;

    public ClientHandler(Socket clientSocket, File uploadsDir) {
        this.clientSocket = clientSocket;
        this.uploadsDir = uploadsDir;
        this.speedMonitor = new SpeedMonitor();
        this.clientId = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
    }

    @Override
    public void run() {
        try (
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();
        ) {
            System.out.println("[" + clientId + "] Начало приёма файла");

            // таймер для вывода скорости каждые 3 секунды
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {// планирует задачу на выполнение с интервалом времени
                @Override
                public void run() {
                    printSpeed();
                }
            }, 3000, 3000);

            boolean success = receiveFile(in);

            // останавливаем таймер и выводим финальную скорость
            timer.cancel();
            printSpeed();

            sendResult(out, success);

            System.out.println("[" + clientId + "] Соединение закрыто\n");

        } catch (IOException e) {
            System.err.println("[" + clientId + "] Ошибка: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                //
            }
        }
    }

    private boolean receiveFile(InputStream in) throws IOException {
        //длина имени файла (4 байта)
        byte[] nameLenBytes = new byte[4];
        if (in.read(nameLenBytes) != 4) {
            System.err.println("[" + clientId + "] Ошибка чтения длины имени файла");
            return false;
        }
//        int nameLength = bytesToInt(nameLenBytes); //переводим в число
        int nameLength = ByteBuffer.wrap(nameLenBytes).getInt();

        if (nameLength <= 0 || nameLength > 4096) {
            System.err.println("[" + clientId + "] Некорректная длина имени: " + nameLength);
            return false;
        }

        //читаем имя файла
        byte[] nameBytes = new byte[nameLength];
        int totalRead = 0;
        while (totalRead < nameLength) {
            int read = in.read(nameBytes, totalRead, nameLength - totalRead);
            if (read == -1) {
                System.err.println("[" + clientId + "] Соединение прервано при чтении имени файла");
                return false;
            }
            totalRead += read;
        }
        String fileName = new String(nameBytes, StandardCharsets.UTF_8);//
        System.out.println("[" + clientId + "] Имя файла: " + fileName);

        //читаем размер файла (8 байт)
        byte[] sizeBytes = new byte[8];
        if (in.read(sizeBytes) != 8) {
            System.err.println("[" + clientId + "] Ошибка чтения размера файла");
            return false;
        }
//        long fileSize = bytesToLong(sizeBytes);
        long fileSize = ByteBuffer.wrap(sizeBytes).getLong();
        System.out.println("[" + clientId + "] Размер файла: " + fileSize + " байт");

        if (fileSize < 0 || fileSize > 1_099_511_627_776L) { // 1 TB
            System.err.println("[" + clientId + "] Некорректный размер файла");
            return false;
        }

        //сохраняем файл
        return saveFile(fileName, in, fileSize);
    }

    private boolean saveFile(String fileName, InputStream in, long expectedSize) {
        //убираем опасные символы из имени файла
        String safeName = sanitizeFileName(fileName);
        File outputFile = new File(uploadsDir, safeName);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            long totalReceived = 0;
            int bytesRead;

            while (totalReceived < expectedSize && (bytesRead = in.read(buffer)) != -1) {
                //определяем колчиество байт для записи: мин(сколько реально прочитано, сколько осталось)
                int toWrite = (int) Math.min(bytesRead, expectedSize - totalReceived);
                fos.write(buffer, 0, toWrite);
                totalReceived += toWrite;
                speedMonitor.update(toWrite);
            }

            System.out.println("[" + clientId + "] Получено байт: " + totalReceived);

            if (totalReceived == expectedSize) {
                System.out.println("[" + clientId + "] Файл успешно сохранён: " + outputFile.getPath());
                return true;
            } else {
                System.err.println("[" + clientId + "] Размер не совпадает! Ожидалось: " +
                        expectedSize + ", получено: " + totalReceived);
                outputFile.delete();
                return false;
            }

        } catch (IOException e) {
            System.err.println("[" + clientId + "] Ошибка сохранения файла: " + e.getMessage());
            return false;
        }
    }

    private void sendResult(OutputStream out, boolean success) throws IOException {
        out.write(success ? 1 : 0);
        out.flush();
    }

    private void printSpeed() {
        double instant = speedMonitor.getInstantSpeed();
        double average = speedMonitor.getAverageSpeed();
        System.out.printf("[%s] Скорость: мгновенная = %.2f КБ/с, средняя = %.2f КБ/с%n",
                clientId, instant / 1024, average / 1024);
    }

    private String sanitizeFileName(String fileName) {
        //убрать путь если передан абсолютный путь до файла
        fileName = new File(fileName).getName();
        //все неподходящие символы реплейсятся на подчеркивание
        fileName = fileName.replaceAll("[^a-zA-Z0-9._\\-а-яА-ЯёЁ]", "_");

        return fileName;
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }

    //преобразование 8 байт в long (Big Endian)
    private long bytesToLong(byte[] bytes) {
        return ((long)(bytes[0] & 0xFF) << 56) |
                ((long)(bytes[1] & 0xFF) << 48) |
                ((long)(bytes[2] & 0xFF) << 40) |
                ((long)(bytes[3] & 0xFF) << 32) |
                ((long)(bytes[4] & 0xFF) << 24) |
                ((long)(bytes[5] & 0xFF) << 16) |
                ((long)(bytes[6] & 0xFF) << 8) |
                (long)(bytes[7] & 0xFF);
    }
}
