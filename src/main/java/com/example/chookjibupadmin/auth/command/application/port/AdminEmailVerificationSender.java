package com.example.chookjibupadmin.auth.command.application.port;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;

/**
 * 관리자 이메일 인증 코드를 외부 메일 채널로 전달하는 포트이다.
 */
public interface AdminEmailVerificationSender {

    /**
     * 지정한 이메일 주소로 인증 코드를 발송한다.
     */
    void send(AdminEmail email, String code);
}
