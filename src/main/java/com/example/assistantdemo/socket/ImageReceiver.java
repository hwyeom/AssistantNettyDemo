//package com.example.assistantdemo.socket;
//
//import lombok.extern.slf4j.Slf4j;
//
//import java.awt.image.BufferedImage;
//import java.io.*;
//import java.net.Socket;
//import javax.imageio.ImageIO;
//
//@Slf4j
//public class ImageReceiver implements Runnable {
//    private final Socket clientSocket;
//
//    public ImageReceiver(Socket socket) {
//        this.clientSocket = socket;
//    }
//
//    @Override
//    public void run() {
//        receive(clientSocket);
//    }
//
//    private void receive(Socket socket) {
//        try {
//            // 1. 이미지 크기 수신
//            byte[] lengthBuffer = new byte[4];
//            int bytesRead = socket.getInputStream().read(lengthBuffer);
//            if (bytesRead < 4) {
//                log.error("이미지 크기 수신 실패. 읽은 바이트 수: {}", bytesRead);
//                return;
//            }
//
//            // 바이트 배열을 정수로 변환 (Big-endian)
//            int length = ((lengthBuffer[0] & 0xFF) << 24) |
//                    ((lengthBuffer[1] & 0xFF) << 16) |
//                    ((lengthBuffer[2] & 0xFF) << 8) |
//                    (lengthBuffer[3] & 0xFF);
//
//            // 길이가 음수인지 확인
//            if (length < 0) {
//                log.error("수신된 이미지 길이가 음수입니다: {}", length);
//                return;
//            }
//
//            log.info("수신할 이미지 크기: {}", length);
//
//            // 2. 이미지 데이터 수신
//            byte[] buffer = new byte[length];
//            int totalRead = 0;
//
//            while (totalRead < length) {
//                bytesRead = socket.getInputStream().read(buffer, totalRead, length - totalRead);
//                if (bytesRead == -1) break; // 더 이상 읽을 수 없는 경우 종료
//                totalRead += bytesRead;
//            }
//
//            if (totalRead < length) {
//                log.error("수신한 이미지 데이터 크기({})가 예상 크기({})보다 작습니다.", totalRead, length);
//                return;
//            }
//
//
//            // 3. 이미지 변환 및 처리
//            BufferedImage image = convertToBufferedImage(buffer);
//            if (image != null) {
//                saveImageToFile(image, "received_image.jpg"); // 이미지 파일로 저장
//                // 이벤트 핸들러 호출 (여기서는 단순히 로그로 대체)
//                log.info("이미지를 수신했습니다.");
//                // 실제 이벤트 핸들러 호출 부분은 추가로 구현할 수 있습니다.
//            }
//
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    private BufferedImage convertToBufferedImage(byte[] data) {
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
//            return ImageIO.read(bais);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//
//    private void saveImageToFile(BufferedImage image, String filePath) {
//        try {
//            ImageIO.write(image, "jpg", new File(filePath));
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//}
