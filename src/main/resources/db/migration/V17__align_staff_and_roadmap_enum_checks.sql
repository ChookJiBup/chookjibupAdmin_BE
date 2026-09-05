DO $$
BEGIN
    -- FieldStaffStatus: ACTIVE / INACTIVE / DELETED
    IF to_regclass('field_staff_accounts') IS NOT NULL THEN
        ALTER TABLE field_staff_accounts
            DROP CONSTRAINT IF EXISTS field_staff_accounts_status_check;

        UPDATE field_staff_accounts
        SET status = 'INACTIVE'
        WHERE status = 'SUSPENDED';

        ALTER TABLE field_staff_accounts
            ADD CONSTRAINT field_staff_accounts_status_check
            CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'));
    END IF;

    -- RoadmapStatus (+ legacy pipeline values kept for existing rows)
    IF to_regclass('festival_roadmap') IS NOT NULL THEN
        ALTER TABLE festival_roadmap
            DROP CONSTRAINT IF EXISTS festival_roadmap_status_check;

        ALTER TABLE festival_roadmap
            ADD CONSTRAINT festival_roadmap_status_check
            CHECK (status IN (
                'ANALYZING', 'REVIEW_REQUIRED', 'EDITING', 'PUBLISHED',
                'DRAFT', 'APPROVED', 'ARCHIVED'
            ));
    END IF;

    -- RoadmapNode Admin enums (replace leftover pipeline CHECKs)
    IF to_regclass('roadmap_node') IS NOT NULL THEN
        ALTER TABLE roadmap_node
            DROP CONSTRAINT IF EXISTS roadmap_node_node_type_check;
        ALTER TABLE roadmap_node
            DROP CONSTRAINT IF EXISTS roadmap_node_source_check;
        ALTER TABLE roadmap_node
            DROP CONSTRAINT IF EXISTS roadmap_node_review_status_check;
        ALTER TABLE roadmap_node
            DROP CONSTRAINT IF EXISTS chk_roadmap_node_source_job;

        UPDATE roadmap_node
        SET source = CASE source
            WHEN 'AI_GENERATED' THEN 'AI'
            WHEN 'MANUAL' THEN 'ADMIN'
            ELSE source
        END
        WHERE source IN ('AI_GENERATED', 'MANUAL');

        UPDATE roadmap_node
        SET review_status = CASE review_status
            WHEN 'APPROVED' THEN 'CONFIRMED'
            WHEN 'MODIFIED' THEN 'CONFIRMED'
            WHEN 'REJECTED' THEN 'REVIEW_REQUIRED'
            ELSE review_status
        END
        WHERE review_status IN ('APPROVED', 'MODIFIED', 'REJECTED');

        UPDATE roadmap_node
        SET node_type = CASE node_type
            WHEN 'ENTRANCE_EXIT' THEN 'ENTRANCE'
            WHEN 'EMPTY_SPACE' THEN 'OPEN_SPACE'
            WHEN 'MEDICAL' THEN 'OTHER'
            ELSE node_type
        END
        WHERE node_type IN ('ENTRANCE_EXIT', 'EMPTY_SPACE', 'MEDICAL');

        ALTER TABLE roadmap_node
            ADD CONSTRAINT roadmap_node_node_type_check
            CHECK (node_type IN (
                'BOOTH', 'STAGE', 'RESTROOM', 'ENTRANCE', 'EXIT', 'PATH',
                'BUILDING', 'OPEN_SPACE', 'PARKING', 'INFORMATION', 'QUEUE', 'OTHER'
            ));

        ALTER TABLE roadmap_node
            ADD CONSTRAINT roadmap_node_source_check
            CHECK (source IN ('AI', 'ADMIN'));

        ALTER TABLE roadmap_node
            ADD CONSTRAINT roadmap_node_review_status_check
            CHECK (review_status IN ('REVIEW_REQUIRED', 'CONFIRMED'));
    END IF;
END
$$;
