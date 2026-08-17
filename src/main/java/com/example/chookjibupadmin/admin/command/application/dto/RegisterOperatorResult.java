package com.example.chookjibupadmin.admin.command.application.dto;

import java.util.UUID;
/**
 * 운영자 등록 결과이다.
 */
public record RegisterOperatorResult(
        UUID adminId,
        String email,
        String name,
        String companyName,
        boolean created,
        String temporaryPassword
) {

    public static RegisterOperatorResult assigned(
            UUID adminId,
            String email,
            String name,
            String companyName
    ) {
        return new RegisterOperatorResult(
                adminId,
                email,
                name,
                companyName,
                false,
                null
        );
    }

    public static RegisterOperatorResult created(
            UUID adminId,
            String email,
            String name,
            String companyName,
            String temporaryPassword
    ) {
        return new RegisterOperatorResult(
                adminId,
                email,
                name,
                companyName,
                true,
                temporaryPassword
        );
    }
}
