package com.example.chookjibupadmin.auth.command.application.port;

/**
 * 비밀번호 재설정 토큰 원문 생성과 저장용 해시 변환 계약이다.
 */
public interface AdminPasswordResetTokenCodec {

    String generate();

    String hash(String rawToken);
}
