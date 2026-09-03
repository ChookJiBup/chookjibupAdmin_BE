# Generates 시드데이터_RDS경량_통합.sql from rev.5 spec
$outName = ([string][char]0xC2DC) + ([string][char]0xB4DC) + ([string][char]0xB370) + ([string][char]0xC774) + ([string][char]0xD130) + '_RDS' + ([string][char]0xACBD) + ([string][char]0xB7C9) + '_' + ([string][char]0xD1B5) + ([string][char]0xD569) + '.sql'
$out = Join-Path $PSScriptRoot $outName
$corePath = Join-Path $PSScriptRoot '_seed_sql_core.sql'
if (-not (Test-Path -LiteralPath $corePath -PathType Leaf)) {
    throw "Seed SQL core not found: $corePath"
}

$bcrypt = '$2a$10$SquZ7eQJgGuMtAB3lvtureY0RtvrWorpA4ENzrRjqhAb7ONCBWffy'
$sha = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'

$core = [System.IO.File]::ReadAllText($corePath, [System.Text.Encoding]::UTF8)
$core = $core.Replace('__BCRYPT__', $bcrypt).Replace('__SHA256__', $sha)

$header = @"
-- Seed data RDS lightweight integrated script (rev.5)
-- Password plaintext for local fixtures: qwer1234
-- Festivals are referenced from ChookJiBup_data_pipeline (no festival INSERT)
-- Execute: psql -f generated_seed_sql.sql
-- Rerun: remove only the declared seed namespace and mapped festival scope

BEGIN;

DO `$`$
BEGIN
    IF to_regclass('festivals') IS NULL THEN
        RAISE EXCEPTION 'festivals table missing. Run data pipeline first.';
    END IF;
    IF (SELECT COUNT(*) FROM festivals WHERE is_active = true) < 10 THEN
        RAISE EXCEPTION 'Need at least 10 active festivals from pipeline.';
    END IF;
END
`$`$;

"@

$footer = @"

SELECT setval(pg_get_serial_sequence('admin_accounts', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM admin_accounts), 1));
SELECT setval(pg_get_serial_sequence('admin_festival_roles', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM admin_festival_roles), 1));
SELECT setval(pg_get_serial_sequence('field_staff_accounts', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM field_staff_accounts), 1));
SELECT setval(pg_get_serial_sequence('festival_locations', 'location_id'), GREATEST((SELECT COALESCE(MAX(location_id), 1) FROM festival_locations), 1));
SELECT setval(pg_get_serial_sequence('festival_maps', 'map_id'), GREATEST((SELECT COALESCE(MAX(map_id), 1) FROM festival_maps), 1));
SELECT setval(pg_get_serial_sequence('festival_roadmap', 'roadmap_id'), GREATEST((SELECT COALESCE(MAX(roadmap_id), 1) FROM festival_roadmap), 1));
SELECT setval(pg_get_serial_sequence('roadmap_node', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM roadmap_node), 1));
SELECT setval(pg_get_serial_sequence('booth_info', 'booth_id'), GREATEST((SELECT COALESCE(MAX(booth_id), 1) FROM booth_info), 1));
SELECT setval(pg_get_serial_sequence('booth_queue', 'queue_id'), GREATEST((SELECT COALESCE(MAX(queue_id), 1) FROM booth_queue), 1));
SELECT setval(pg_get_serial_sequence('booth_congestion', 'congestion_id'), GREATEST((SELECT COALESCE(MAX(congestion_id), 1) FROM booth_congestion), 1));
SELECT setval(pg_get_serial_sequence('festival_visitor_count', 'visitor_count_id'), GREATEST((SELECT COALESCE(MAX(visitor_count_id), 1) FROM festival_visitor_count), 1));
SELECT setval(pg_get_serial_sequence('festival_visitor_total', 'visitor_total_id'), GREATEST((SELECT COALESCE(MAX(visitor_total_id), 1) FROM festival_visitor_total), 1));

COMMIT;
"@

$full = $header + $core + $footer
if ($full.Contains('__BCRYPT__') -or $full.Contains('__SHA256__')) {
    throw 'Generated SQL still contains unresolved placeholders.'
}
foreach ($required in @('BEGIN;', 'COMMIT;', 'INSERT INTO admin_accounts', 'FROM festivals')) {
    if (-not $full.Contains($required)) {
        throw "Generated SQL is missing required section: $required"
    }
}

$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($out, $full, $utf8)
Write-Output "Wrote $out ($($full.Length) bytes)"
