//package com.example.assistantdemo.socket;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.*;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//@Service
//@Slf4j
//public class ImageSocketServer  {
//    @Value("${server.port:9090}")
//    private int port;
//    private ServerSocket serverSocket;
//
//    @PostConstruct
//    public void init() throws IOException {
//        serverSocket = new ServerSocket();
//        serverSocket.bind(new InetSocketAddress(port));
//        new Thread(this::acceptConnections).start();
//    }
//
//    @PreDestroy
//    public void cleanup() {
//        close();
//    }
//
//    private void acceptConnections() {
//        while (true) {
//            try {
//                Socket clientSocket = serverSocket.accept();
//                log.info("클라이언트가 연결되었습니다: {}", clientSocket.getInetAddress());
//                new Thread(() -> receiveImage(clientSocket)).start();
//            } catch (IOException e) {
//                log.error(e.getMessage(), e);
//                close();
//                break;
//            }
//        }
//    }
//
//    private void receiveImage(Socket clientSocket) {
//        try {
//            byte[] lengthBuffer = new byte[4];
//            clientSocket.getInputStream().read(lengthBuffer);
//
//            log.info("getReceiveBufferSize : {}" , clientSocket.getReceiveBufferSize());
//            int length = clientSocket.getReceiveBufferSize(); // byteArrayToInt(lengthBuffer);
//            log.info("이미지 사이즈 : {}", length);
//
//            byte[] buffer = new byte[length];
//            int trans = 0;
//
//            while (trans < length) {
//                trans += clientSocket.getInputStream().read(buffer, trans, length - trans);
//            }
//
//            BufferedImage image = convertToBufferedImage(buffer);
//            // 이벤트 핸들러 호출 (이 부분은 필요에 따라 구현)
//            // onImageReceived(image);
//
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    private BufferedImage convertToBufferedImage(byte[] data) throws IOException {
//        try (InputStream is = new ByteArrayInputStream(data)) {
//            return ImageIO.read(is);
//        }
//    }
//
//    private int byteArrayToInt(byte[] byteArray) {
//        if (byteArray.length != 4) {
//            throw new IllegalArgumentException("byte 배열의 길이는 4여야 합니다.");
//        }
//
//        return (byteArray[0] << 24) | (byteArray[1] << 16) | (byteArray[2] << 8) | (byteArray[3] & 0xFF);
//    }
//
//    public void close() {
//        try {
//            if (serverSocket != null) {
//                serverSocket.close();
//            }
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
////    @Override
////    public void run(String... args) throws Exception {
////        try (ServerSocket serverSocket = new ServerSocket(port)) {
////            log.info("서버가 시작되었습니다. 포트: {}", port);
////            while (true) {
////                Socket clientSocket = serverSocket.accept();
////                log.info("클라이언트가 연결되었습니다: {}", clientSocket.getInetAddress());
////                new Thread(new ImageReceiver(clientSocket)).start();
////            }
////        }
////    }
//
//
//}
//
////@Slf4j
////class ClientHandler implements Runnable {
////    private final Socket clientSocket;
////
////    public ClientHandler(Socket socket) {
////        this.clientSocket = socket;
////    }
////
////    @Override
////    public void run() {
////        try (InputStream inputStream = clientSocket.getInputStream();
////             DataInputStream dataInputStream = new DataInputStream(inputStream);
////             FileOutputStream fos = new FileOutputStream("received_image.jpg")) {
////
////            // 1. 이미지 크기 수신
////            int length = dataInputStream.readInt(); // 이미지 크기 (4바이트 정수)
////            log.info("수신할 이미지 크기: {}", length);
////
////            // 2. 이미지 데이터 수신
////            byte[] buffer = new byte[4096];
////            int bytesRead;
////            int totalRead = 0;
////
////            while (totalRead < length && (bytesRead = dataInputStream.read(buffer, 0, Math.min(buffer.length, length - totalRead))) != -1) {
////                fos.write(buffer, 0, bytesRead);
////                totalRead += bytesRead;
////            }
////
////            fos.flush();
////            log.info("이미지 수신 완료");
////        } catch (IOException e) {
////            log.info(e.getMessage(), e);
////        } finally {
////            try {
////                clientSocket.close(); // 소켓 닫기
////            } catch (IOException e) {
////                log.info(e.getMessage(), e);
////            }
////        }
////    }
////}
