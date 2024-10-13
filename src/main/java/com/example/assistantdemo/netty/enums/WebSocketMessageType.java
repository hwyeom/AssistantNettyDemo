package com.example.assistantdemo.netty.enums;

public enum WebSocketMessageType {
    ROBOT_REGISTER,                 // 로봇 등록
    ROBOT_INFO,     // 로봇 정보 -> 서버
    ROBOT_MOUSE_POINTER,            // 원격 제어 마우스 포인터 위치
    ROBOT_LOG,                      // 로봇이 전송하는 로그
    ROBOT_THUMBNAIL_LIST,           // 로봇리스트 서버 -> 웹 로봇 리스트 전송

    WEB_USER_REGISTER,          // 웹 유저 등록
    WEB_USER_THUMBNAIL_REGISTER,    // 웹 썸네일 시청 유저 등록
    WEB_USER_MOUSE_EVENT,           // 웹 유저 마우스 이벤트
    WEB_USER_KEYBOARD_EVENT         // 웹 유저 키보드 이벤트
}
