package com.example.chookjibupadmin.auth.command.application.port;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;

/**
 * 관리자 비밀번호 재설정 링크 발송 계약이다.
 */
public interface AdminPasswordResetEmailSender {

    void send(AdminEmail email, String resetUrl);
}
