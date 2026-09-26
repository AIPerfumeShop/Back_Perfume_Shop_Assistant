-- Add a separate product concentration field; intensity remains LIGHT/MEDIUM/STRONG.
ALTER TABLE tb_fragrance_profiles
    ADD COLUMN concentration ENUM('PARFUM', 'EDP', 'EDT', 'EDC', 'EAU_FRAICHE') NULL;
