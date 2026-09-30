package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.*;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.*;
import com.example.chookjibupadmin.festival.query.application.InternalFestivalQueryApplicationService;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** TEST_FESTIVAL_PG_URL로 지정한 테스트 DB에 임의 이름의 독립 스키마를 만든다. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TEST_FESTIVAL_PG_URL", matches = ".+")
class FestivalProgressPostgresIntegrationTest {
    private static final String SCHEMA = "progress_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = System.getenv("TEST_FESTIVAL_PG_URL");
    @Autowired FestivalService festivals;
    @Autowired InternalFestivalQueryApplicationService queries;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        try (var connection = DriverManager.getConnection(URL); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        }
        properties.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private static Connection connection() throws Exception {
        var connection = DriverManager.getConnection(URL);
        connection.setSchema(SCHEMA);
        return connection;
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (var connection = DriverManager.getConnection(URL); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void migrationJpaSchedulerAndLegacyPipelineAgree() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            // 관리자 전용 DB처럼 파이프라인 상태 컬럼과 enum이 없는 상태에서 시작한다.
            statement.execute("ALTER TABLE festivals DROP COLUMN progress_status");
            statement.execute("ALTER TABLE festivals DROP COLUMN progress_status_override");
            statement.execute(new ClassPathResource("db/migration/V31__festival_progress_status_override.sql")
                    .getContentAsString(StandardCharsets.UTF_8));
            // UTC DB 세션에서도 서울 날짜를 사용해야 한다.
            statement.execute("SET TIME ZONE 'UTC'");
            var result = statement.executeQuery("SELECT festival_effective_progress((statement_timestamp() AT TIME ZONE 'Asia/Seoul')::date,(statement_timestamp() AT TIME ZONE 'Asia/Seoul')::date,NULL)::text");
            result.next();
            assertThat(result.getString(1)).isEqualTo("ongoing");
        }
        var tx = new TransactionTemplate(transactionManager);
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        UUID id = tx.execute(status -> {
            var festival = festivals.save(Festival.create(1L, UUID.randomUUID(),
                    FestivalName.of("진행상태 검증축제"), FestivalDescription.of("검증용 축제"),
                    FestivalAddress.of("서울특별시 마포구"), FestivalPeriod.of(today, today),
                    FestivalOperationTime.of(LocalTime.of(9, 0), LocalTime.of(18, 0))));
            entityManager.flush();
            entityManager.refresh(festival);
            assertThat(festival.getStoredProgressStatus()).isEqualTo("ongoing");
            festival.changeProgressStatus(FestivalProgressStatus.COMPLETED);
            return festival.getPublicId();
        });
        assertStored("completed");
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("UPDATE festivals SET progress_status='ongoing', progress_status_updated_at=now()");
        }
        assertStored("completed");
        tx.executeWithoutResult(status -> {
            // 실제 PostgreSQL enum에 upper/cast 및 Querydsl 필터가 동작하는지 확인한다.
            assertThat(queries.searchFestivals(FestivalProgressStatus.COMPLETED, null, 0, 20).getContent())
                    .hasSize(1);
            festivals.getByPublicIdForUpdate(id).changeProgressStatus(null);
        });
        assertStored("ongoing");
        // 수동 변경 트랜잭션과 기존 배치가 같은 행을 갱신해도 수동값이 남는다.
        try (var manual = connection(); var statement = manual.createStatement();
                var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            manual.setAutoCommit(false);
            statement.execute("UPDATE festivals SET progress_status_override='COMPLETED'");
            var batch = executor.submit(() -> {
                try (var legacy = connection(); var update = legacy.createStatement()) {
                    update.execute("UPDATE festivals SET progress_status='ongoing'");
                }
                return null;
            });
            manual.commit();
            batch.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertStored("completed");
        tx.executeWithoutResult(status -> festivals.getByPublicIdForUpdate(id).changeProgressStatus(null));
        assertStored("ongoing");
        // 서버 중단 중 자정이 지난 경우를 모사한다. 트리거를 우회한 stale 값만 만든다.
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE festivals DISABLE TRIGGER trg_festivals_progress");
            statement.execute("UPDATE festivals SET progress_status='upcoming'");
            statement.execute("ALTER TABLE festivals ENABLE TRIGGER trg_festivals_progress");
        }
        assertThat(festivals.synchronizeProgressStatuses()).isEqualTo(1);
        assertStored("ongoing");
        assertThat(festivals.synchronizeProgressStatuses()).isZero();
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("UPDATE festivals SET start_date=start_date-2, end_date=end_date-1");
        }
        assertStored("completed");
    }

    private void assertStored(String expected) throws Exception {
        try (var connection = connection(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT progress_status::text FROM festivals")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo(expected);
        }
    }
}
