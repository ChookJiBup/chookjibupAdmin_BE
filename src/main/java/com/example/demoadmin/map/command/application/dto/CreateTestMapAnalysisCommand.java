package com.example.demoadmin.map.command.application.dto;

public record CreateTestMapAnalysisCommand(
        String originalFileName,
        String storagePath,
        int width,
        int height
) {

    private static final String TEST_FILE_NAME = "김밥축제_지적편집도.png";
    private static final String TEST_STORAGE_PATH =
            "images/김밥축제_지적편집도.png";
    private static final int TEST_IMAGE_WIDTH = 1745;
    private static final int TEST_IMAGE_HEIGHT = 1577;

    /**
     * 프로젝트에서 관리하는 고정 테스트 배치도 분석 명령을 생성한다.
     */
    public static CreateTestMapAnalysisCommand testFixture() {
        return new CreateTestMapAnalysisCommand(
                TEST_FILE_NAME,
                TEST_STORAGE_PATH,
                TEST_IMAGE_WIDTH,
                TEST_IMAGE_HEIGHT
        );
    }
}
