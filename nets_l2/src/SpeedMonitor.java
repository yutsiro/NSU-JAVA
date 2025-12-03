public class SpeedMonitor {
    private long totalBytes = 0;
    private long startTime;
    private long lastCheckTime;
    private long lastCheckBytes = 0;

    public SpeedMonitor() {
        this.startTime = System.currentTimeMillis();
        this.lastCheckTime = startTime;
    }

    public synchronized void update(long bytesRead) {
        totalBytes += bytesRead;
    }

    public synchronized double getInstantSpeed() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastCheckTime;

        if (timeDiff == 0) {
            return 0.0;
        }

        long bytesDiff = totalBytes - lastCheckBytes;
        double speed = (bytesDiff * 1000.0) / timeDiff; // байт/сек, 1000 для перевода в сек

        lastCheckTime = currentTime;
        lastCheckBytes = totalBytes;

        return speed;
    }

    public synchronized double getAverageSpeed() {
        long currentTime = System.currentTimeMillis();
        long totalTime = currentTime - startTime;

        if (totalTime == 0) {
            return 0.0;
        }

        return (totalBytes * 1000.0) / totalTime; // байт/сек
    }
}
