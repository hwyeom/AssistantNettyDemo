package com.example.assistantdemo.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketHandler extends BinaryWebSocketHandler {

    // 연결된 세션을 저장하는 리스트 (여러 클라이언트 관리)
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] payload = message.getPayload().array();  // 바이너리 데이터 추출

        // 예: 받은 이미지를 파일로 저장 (서버에서만 확인 용도)
        try (FileOutputStream fos = new FileOutputStream("received_image.jpg")) {
            fos.write(payload);  // 바이너리 데이터를 파일로 저장
        }

        // 클라이언트로부터 이미지 데이터를 수신하여 다른 세션에 브로드 캐스트
        for(WebSocketSession webSocketSession : sessions){
            if (webSocketSession.isOpen() && !session.getId().equals(webSocketSession.getId())) {
                // 수신한 이미지를 다른 클라이언트로 전송하거나 처리 가능
                webSocketSession.sendMessage(new BinaryMessage(payload));  // 그대로 클라이언트로 전달 (Broadcast 또는 Echo)
            }
        }
    }
}
