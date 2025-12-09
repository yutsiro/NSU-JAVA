package com.socksproxy;

public class SocksProxy {
    public static void main(String[] args) {
//        if (args.length != 1) {
//            System.err.println("Usage: java -jar socks-proxy.jar <port>");
//            System.exit(1);
//        }
        try {
//            int port = Integer.parseInt(args[0]);
            int port = 1080;
            ProxyServer server = new ProxyServer(port);
            server.run();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Failed to start proxy server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}