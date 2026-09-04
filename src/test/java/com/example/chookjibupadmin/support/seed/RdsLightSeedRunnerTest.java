package com.example.chookjibupadmin.support.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RdsLightSeedRunnerTest {

    @Test
    @DisplayName("세미콜론과 PostgreSQL dollar-quote 블록을 구분해 분리한다")
    void success_SplitSql_DollarQuoteAndSemicolon() {
        String script = """
                BEGIN;
                DO $$
                BEGIN
                    RAISE NOTICE 'seed; keep';
                END
                $$;
                SELECT 1;
                COMMIT;
                """;

        List<String> statements = RdsLightSeedRunner.splitSql(script);

        assertThat(statements).hasSize(4);
        assertThat(statements.get(0)).isEqualTo("BEGIN");
        assertThat(statements.get(1)).contains("DO $$").contains("END").contains("$$");
        assertThat(statements.get(2)).isEqualTo("SELECT 1");
        assertThat(statements.get(3)).isEqualTo("COMMIT");
    }
}
