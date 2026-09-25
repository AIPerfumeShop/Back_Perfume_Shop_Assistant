-- Apply before deploying a build that includes CustomerFragranceProfile.
-- Production runs Hibernate in validate mode and does not mutate its schema.
CREATE TABLE IF NOT EXISTS tb_customer_fragrance_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sweetness INT NOT NULL DEFAULT 50,
    floral INT NOT NULL DEFAULT 50,
    fresh INT NOT NULL DEFAULT 50,
    woody INT NOT NULL DEFAULT 50,
    intensity VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    personality VARCHAR(120) NOT NULL DEFAULT 'Balanced Explorer',
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_fragrance_profile_user (user_id),
    CONSTRAINT fk_customer_fragrance_profile_user
        FOREIGN KEY (user_id) REFERENCES tb_users (id)
) ENGINE=InnoDB;
