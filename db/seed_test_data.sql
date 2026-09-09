-- Seed test data: exactly 15 rows per table for db_Aiperfume
-- IDs use a high range (100+) so they never clash with existing rows.
-- All seeded users share the password: TestPassword123!


SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM tb_support_ticket_notes WHERE id BETWEEN 17000 AND 17015;
DELETE FROM tb_support_tickets WHERE id BETWEEN 16000 AND 16015;
DELETE FROM tb_ai_recommendation_clicks WHERE id BETWEEN 15000 AND 15015;
DELETE FROM tb_ai_recommendations WHERE id BETWEEN 14000 AND 14015;
DELETE FROM tb_ai_messages WHERE id BETWEEN 13000 AND 13015;
DELETE FROM tb_ai_conversations WHERE id BETWEEN 12000 AND 12015;
DELETE FROM tb_payments WHERE id BETWEEN 11000 AND 11015;
DELETE FROM tb_order_items WHERE id BETWEEN 10000 AND 10015;
DELETE FROM tb_orders WHERE id BETWEEN 900 AND 915;
DELETE FROM tb_reviews WHERE id BETWEEN 8000 AND 8015;
DELETE FROM tb_wishlist_items WHERE id BETWEEN 7000 AND 7015;
DELETE FROM tb_wishlists WHERE id BETWEEN 600 AND 615;
DELETE FROM tb_cart_items WHERE id BETWEEN 5000 AND 5015;
DELETE FROM tb_carts WHERE id BETWEEN 400 AND 415;
DELETE FROM tb_fragrance_profiles WHERE id BETWEEN 3000 AND 3015;
DELETE FROM tb_product_images WHERE id BETWEEN 2000 AND 2015;
DELETE FROM tb_product_variants WHERE id BETWEEN 1000 AND 1015;
DELETE FROM tb_products WHERE id BETWEEN 100 AND 115;
DELETE FROM tb_categories WHERE id BETWEEN 100 AND 115;
DELETE FROM tb_brands WHERE id BETWEEN 100 AND 115;
DELETE FROM tb_users WHERE id BETWEEN 100 AND 115;
SET FOREIGN_KEY_CHECKS = 1;
INSERT INTO tb_users (id, address, fullname, email, password, phone, role, is_active, created_at, updated_at, create_at, name) VALUES
(101, NULL, 'Test User 1', 'user1@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230001', 'CUSTOMER', b'1', '2026-09-01 08:01:00', '2026-09-01 08:01:00', '2026-09-01 08:01:00', NULL),
(102, NULL, 'Test User 2', 'user2@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230002', 'CUSTOMER', b'1', '2026-09-01 08:02:00', '2026-09-01 08:02:00', '2026-09-01 08:02:00', NULL),
(103, NULL, 'Test User 3', 'user3@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230003', 'CUSTOMER', b'1', '2026-09-01 08:03:00', '2026-09-01 08:03:00', '2026-09-01 08:03:00', NULL),
(104, NULL, 'Test User 4', 'user4@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230004', 'CUSTOMER', b'1', '2026-09-01 08:04:00', '2026-09-01 08:04:00', '2026-09-01 08:04:00', NULL),
(105, NULL, 'Test User 5', 'user5@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230005', 'CUSTOMER', b'1', '2026-09-01 08:05:00', '2026-09-01 08:05:00', '2026-09-01 08:05:00', NULL),
(106, NULL, 'Test User 6', 'user6@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230006', 'CUSTOMER', b'1', '2026-09-01 08:06:00', '2026-09-01 08:06:00', '2026-09-01 08:06:00', NULL),
(107, NULL, 'Test User 7', 'user7@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230007', 'CUSTOMER', b'1', '2026-09-01 08:07:00', '2026-09-01 08:07:00', '2026-09-01 08:07:00', NULL),
(108, NULL, 'Test User 8', 'user8@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230008', 'CUSTOMER', b'1', '2026-09-01 08:08:00', '2026-09-01 08:08:00', '2026-09-01 08:08:00', NULL),
(109, NULL, 'Test User 9', 'user9@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230009', 'CUSTOMER', b'1', '2026-09-01 08:09:00', '2026-09-01 08:09:00', '2026-09-01 08:09:00', NULL),
(110, NULL, 'Test User 10', 'user10@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230010', 'CUSTOMER', b'1', '2026-09-01 08:10:00', '2026-09-01 08:10:00', '2026-09-01 08:10:00', NULL),
(111, NULL, 'Test User 11', 'user11@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230011', 'CUSTOMER', b'1', '2026-09-01 08:11:00', '2026-09-01 08:11:00', '2026-09-01 08:11:00', NULL),
(112, NULL, 'Test User 12', 'user12@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230012', 'CUSTOMER', b'1', '2026-09-01 08:12:00', '2026-09-01 08:12:00', '2026-09-01 08:12:00', NULL),
(113, NULL, 'Test User 13', 'user13@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230013', 'CUSTOMER', b'1', '2026-09-01 08:13:00', '2026-09-01 08:13:00', '2026-09-01 08:13:00', NULL),
(114, NULL, 'Test User 14', 'user14@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230014', 'CUSTOMER', b'1', '2026-09-01 08:14:00', '2026-09-01 08:14:00', '2026-09-01 08:14:00', NULL),
(115, NULL, 'Test User 15', 'user15@test.com', '$2a$10$rMy7EeqpuvV4ZkecZZc5H.PLTopxwsLpk2DRgllWrOkNvgPM0U6zS', '01230015', 'CUSTOMER', b'1', '2026-09-01 08:15:00', '2026-09-01 08:15:00', '2026-09-01 08:15:00', NULL);

INSERT INTO tb_brands (id, name, description, is_active, logo_url, created_at, updated_at) VALUES
(101, 'Le Labo Test', 'Luxury Le Labo fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(102, 'Byredo Test', 'Luxury Byredo fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(103, 'Diptyque Test', 'Luxury Diptyque fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(104, 'Maison Margiela Test', 'Luxury Maison Margiela fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(105, 'Jo Malone Test', 'Luxury Jo Malone fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(106, 'Tom Ford Test', 'Luxury Tom Ford fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(107, 'Creed Test', 'Luxury Creed fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(108, 'Acqua di Parma Test', 'Luxury Acqua di Parma fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(109, 'Penhaligon''s Test', 'Luxury Penhaligon''s fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(110, 'Frederic Malle Test', 'Luxury Frederic Malle fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(111, 'Memo Paris Test', 'Luxury Memo Paris fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(112, 'Xerjoff Test', 'Luxury Xerjoff fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(113, 'Amouage Test', 'Luxury Amouage fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(114, 'Kilian Test', 'Luxury Kilian fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(115, 'Aerin Test', 'Luxury Aerin fragrance house (test data).', b'1', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00');

INSERT INTO tb_categories (id, name, description, image_url, is_active, created_at, updated_at) VALUES
(101, 'Category Woody', 'Test perfume family: Woody.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(102, 'Category Floral', 'Test perfume family: Floral.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(103, 'Category Oriental', 'Test perfume family: Oriental.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(104, 'Category Fresh', 'Test perfume family: Fresh.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(105, 'Category Chypre', 'Test perfume family: Chypre.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(106, 'Category Citrus', 'Test perfume family: Citrus.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(107, 'Category Gourmand', 'Test perfume family: Gourmand.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(108, 'Category Green', 'Test perfume family: Green.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(109, 'Category Spicy', 'Test perfume family: Spicy.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(110, 'Category Leather', 'Test perfume family: Leather.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(111, 'Category Aquatic', 'Test perfume family: Aquatic.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(112, 'Category Powdery', 'Test perfume family: Powdery.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(113, 'Category Fruity', 'Test perfume family: Fruity.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(114, 'Category Amber', 'Test perfume family: Amber.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00'),
(115, 'Category Boozy', 'Test perfume family: Boozy.', NULL, b'1', '2026-09-01 09:10:00', '2026-09-01 09:10:00');

INSERT INTO tb_products (id, name, description, is_active, created_at, updated_at, brand_id, category_id) VALUES
(101, 'Santal 33 Eau de Parfum 1', 'Test scent for product 1 by Le Labo (Woody family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 101, 101),
(102, 'Santal 33 Eau de Parfum 2', 'Test scent for product 2 by Byredo (Floral family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 102, 102),
(103, 'Santal 33 Eau de Parfum 3', 'Test scent for product 3 by Diptyque (Oriental family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 103, 103),
(104, 'Santal 33 Eau de Parfum 4', 'Test scent for product 4 by Maison Margiela (Fresh family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 104, 104),
(105, 'Santal 33 Eau de Parfum 5', 'Test scent for product 5 by Jo Malone (Chypre family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 105, 105),
(106, 'Santal 33 Eau de Parfum 6', 'Test scent for product 6 by Tom Ford (Citrus family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 106, 106),
(107, 'Santal 33 Eau de Parfum 7', 'Test scent for product 7 by Creed (Gourmand family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 107, 107),
(108, 'Santal 33 Eau de Parfum 8', 'Test scent for product 8 by Acqua di Parma (Green family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 108, 108),
(109, 'Santal 33 Eau de Parfum 9', 'Test scent for product 9 by Penhaligon''s (Spicy family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 109, 109),
(110, 'Santal 33 Eau de Parfum 10', 'Test scent for product 10 by Frederic Malle (Leather family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 110, 110),
(111, 'Santal 33 Eau de Parfum 11', 'Test scent for product 11 by Memo Paris (Aquatic family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 111, 111),
(112, 'Santal 33 Eau de Parfum 12', 'Test scent for product 12 by Xerjoff (Powdery family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 112, 112),
(113, 'Santal 33 Eau de Parfum 13', 'Test scent for product 13 by Amouage (Fruity family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 113, 113),
(114, 'Santal 33 Eau de Parfum 14', 'Test scent for product 14 by Kilian (Amber family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 114, 114),
(115, 'Santal 33 Eau de Parfum 15', 'Test scent for product 15 by Aerin (Boozy family).', b'1', '2026-09-01 09:20:00', '2026-09-01 09:20:00', 115, 115);

INSERT INTO tb_product_variants (id, product_id, is_active, price, size_ml, sku, stock, created_at, updated_at) VALUES
(1001, 101, b'1', 86.0, 50, 'SKU-P101-50', 21, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1002, 102, b'1', 87.0, 100, 'SKU-P102-100', 22, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1003, 103, b'1', 88.0, 50, 'SKU-P103-50', 23, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1004, 104, b'1', 89.0, 100, 'SKU-P104-100', 24, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1005, 105, b'1', 90.0, 50, 'SKU-P105-50', 25, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1006, 106, b'1', 91.0, 100, 'SKU-P106-100', 26, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1007, 107, b'1', 92.0, 50, 'SKU-P107-50', 27, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1008, 108, b'1', 93.0, 100, 'SKU-P108-100', 28, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1009, 109, b'1', 94.0, 50, 'SKU-P109-50', 29, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1010, 110, b'1', 95.0, 100, 'SKU-P110-100', 30, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1011, 111, b'1', 96.0, 50, 'SKU-P111-50', 31, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1012, 112, b'1', 97.0, 100, 'SKU-P112-100', 32, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1013, 113, b'1', 98.0, 50, 'SKU-P113-50', 33, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1014, 114, b'1', 99.0, 100, 'SKU-P114-100', 34, '2026-09-01 09:30:00', '2026-09-01 09:30:00'),
(1015, 115, b'1', 100.0, 50, 'SKU-P115-50', 35, '2026-09-01 09:30:00', '2026-09-01 09:30:00');

INSERT INTO tb_product_images (id, image_url, display_order, is_primary, created_at, product_id) VALUES
(2001, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-101.jpg', 1, b'1', '2026-09-01 09:40:00', 101),
(2002, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-102.jpg', 2, b'1', '2026-09-01 09:40:00', 102),
(2003, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-103.jpg', 3, b'1', '2026-09-01 09:40:00', 103),
(2004, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-104.jpg', 4, b'1', '2026-09-01 09:40:00', 104),
(2005, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-105.jpg', 5, b'1', '2026-09-01 09:40:00', 105),
(2006, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-106.jpg', 6, b'1', '2026-09-01 09:40:00', 106),
(2007, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-107.jpg', 7, b'1', '2026-09-01 09:40:00', 107),
(2008, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-108.jpg', 8, b'1', '2026-09-01 09:40:00', 108),
(2009, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-109.jpg', 9, b'1', '2026-09-01 09:40:00', 109),
(2010, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-110.jpg', 10, b'1', '2026-09-01 09:40:00', 110),
(2011, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-111.jpg', 11, b'1', '2026-09-01 09:40:00', 111),
(2012, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-112.jpg', 12, b'1', '2026-09-01 09:40:00', 112),
(2013, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-113.jpg', 13, b'1', '2026-09-01 09:40:00', 113),
(2014, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-114.jpg', 14, b'1', '2026-09-01 09:40:00', 114),
(2015, 'https://pub-0a39e0e9d8984d8b94b2a3630b71d4a7.r2.dev/products/sample-115.jpg', 15, b'1', '2026-09-01 09:40:00', 115);

INSERT INTO tb_fragrance_profiles (id, frag_notes, fragrance_family, gender, intensity, product_id) VALUES
(3001, 'sandalwood, cardamom, iris', 'Woody', 'UNISEX', 'LIGHT', 101),
(3002, 'sandalwood, cardamom, iris', 'Floral', 'MEN', 'MEDIUM', 102),
(3003, 'sandalwood, cardamom, iris', 'Oriental', 'WOMEN', 'STRONG', 103),
(3004, 'sandalwood, cardamom, iris', 'Fresh', 'UNISEX', 'LIGHT', 104),
(3005, 'sandalwood, cardamom, iris', 'Chypre', 'MEN', 'MEDIUM', 105),
(3006, 'sandalwood, cardamom, iris', 'Citrus', 'WOMEN', 'STRONG', 106),
(3007, 'sandalwood, cardamom, iris', 'Gourmand', 'UNISEX', 'LIGHT', 107),
(3008, 'sandalwood, cardamom, iris', 'Green', 'MEN', 'MEDIUM', 108),
(3009, 'sandalwood, cardamom, iris', 'Spicy', 'WOMEN', 'STRONG', 109),
(3010, 'sandalwood, cardamom, iris', 'Leather', 'UNISEX', 'LIGHT', 110),
(3011, 'sandalwood, cardamom, iris', 'Aquatic', 'MEN', 'MEDIUM', 111),
(3012, 'sandalwood, cardamom, iris', 'Powdery', 'WOMEN', 'STRONG', 112),
(3013, 'sandalwood, cardamom, iris', 'Fruity', 'UNISEX', 'LIGHT', 113),
(3014, 'sandalwood, cardamom, iris', 'Amber', 'MEN', 'MEDIUM', 114),
(3015, 'sandalwood, cardamom, iris', 'Boozy', 'WOMEN', 'STRONG', 115);

INSERT INTO tb_carts (id, user_id, created_at, updated_at) VALUES
(401, 101, '2026-09-02 10:01:00', '2026-09-02 10:01:00'),
(402, 102, '2026-09-02 10:02:00', '2026-09-02 10:02:00'),
(403, 103, '2026-09-02 10:03:00', '2026-09-02 10:03:00'),
(404, 104, '2026-09-02 10:04:00', '2026-09-02 10:04:00'),
(405, 105, '2026-09-02 10:05:00', '2026-09-02 10:05:00'),
(406, 106, '2026-09-02 10:06:00', '2026-09-02 10:06:00'),
(407, 107, '2026-09-02 10:07:00', '2026-09-02 10:07:00'),
(408, 108, '2026-09-02 10:08:00', '2026-09-02 10:08:00'),
(409, 109, '2026-09-02 10:09:00', '2026-09-02 10:09:00'),
(410, 110, '2026-09-02 10:10:00', '2026-09-02 10:10:00'),
(411, 111, '2026-09-02 10:11:00', '2026-09-02 10:11:00'),
(412, 112, '2026-09-02 10:12:00', '2026-09-02 10:12:00'),
(413, 113, '2026-09-02 10:13:00', '2026-09-02 10:13:00'),
(414, 114, '2026-09-02 10:14:00', '2026-09-02 10:14:00'),
(415, 115, '2026-09-02 10:15:00', '2026-09-02 10:15:00');

INSERT INTO tb_cart_items (id, cart_id, variant_id, quantity, created_at, updated_at) VALUES
(5001, 401, 1001, 2, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5002, 402, 1002, 3, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5003, 403, 1003, 1, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5004, 404, 1004, 2, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5005, 405, 1005, 3, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5006, 406, 1006, 1, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5007, 407, 1007, 2, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5008, 408, 1008, 3, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5009, 409, 1009, 1, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5010, 410, 1010, 2, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5011, 411, 1011, 3, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5012, 412, 1012, 1, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5013, 413, 1013, 2, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5014, 414, 1014, 3, '2026-09-02 10:30:00', '2026-09-02 10:30:00'),
(5015, 415, 1015, 1, '2026-09-02 10:30:00', '2026-09-02 10:30:00');

INSERT INTO tb_wishlists (id, user_id, created_at) VALUES
(601, 101, '2026-09-03 11:01:00'),
(602, 102, '2026-09-03 11:02:00'),
(603, 103, '2026-09-03 11:03:00'),
(604, 104, '2026-09-03 11:04:00'),
(605, 105, '2026-09-03 11:05:00'),
(606, 106, '2026-09-03 11:06:00'),
(607, 107, '2026-09-03 11:07:00'),
(608, 108, '2026-09-03 11:08:00'),
(609, 109, '2026-09-03 11:09:00'),
(610, 110, '2026-09-03 11:10:00'),
(611, 111, '2026-09-03 11:11:00'),
(612, 112, '2026-09-03 11:12:00'),
(613, 113, '2026-09-03 11:13:00'),
(614, 114, '2026-09-03 11:14:00'),
(615, 115, '2026-09-03 11:15:00');

INSERT INTO tb_wishlist_items (id, wishlist_id, product_id, created_at) VALUES
(7001, 601, 101, '2026-09-03 11:30:00'),
(7002, 602, 102, '2026-09-03 11:30:00'),
(7003, 603, 103, '2026-09-03 11:30:00'),
(7004, 604, 104, '2026-09-03 11:30:00'),
(7005, 605, 105, '2026-09-03 11:30:00'),
(7006, 606, 106, '2026-09-03 11:30:00'),
(7007, 607, 107, '2026-09-03 11:30:00'),
(7008, 608, 108, '2026-09-03 11:30:00'),
(7009, 609, 109, '2026-09-03 11:30:00'),
(7010, 610, 110, '2026-09-03 11:30:00'),
(7011, 611, 111, '2026-09-03 11:30:00'),
(7012, 612, 112, '2026-09-03 11:30:00'),
(7013, 613, 113, '2026-09-03 11:30:00'),
(7014, 614, 114, '2026-09-03 11:30:00'),
(7015, 615, 115, '2026-09-03 11:30:00');

INSERT INTO tb_reviews (id, comment, created_at, is_approved, rating, updated_at, product_id, user_id, deleted_at, is_deleted, moderation_note) VALUES
(8001, 'Great scent, long lasting. (test review 1)', '2026-09-04 12:01:00', b'1', 4, '2026-09-04 12:01:00', 101, 101, NULL, b'0', NULL),
(8002, 'Great scent, long lasting. (test review 2)', '2026-09-04 12:02:00', b'1', 5, '2026-09-04 12:02:00', 102, 102, NULL, b'0', NULL),
(8003, 'Great scent, long lasting. (test review 3)', '2026-09-04 12:03:00', b'1', 3, '2026-09-04 12:03:00', 103, 103, NULL, b'0', NULL),
(8004, 'Great scent, long lasting. (test review 4)', '2026-09-04 12:04:00', b'1', 4, '2026-09-04 12:04:00', 104, 104, NULL, b'0', NULL),
(8005, 'Great scent, long lasting. (test review 5)', '2026-09-04 12:05:00', b'1', 5, '2026-09-04 12:05:00', 105, 105, NULL, b'0', NULL),
(8006, 'Great scent, long lasting. (test review 6)', '2026-09-04 12:06:00', b'1', 3, '2026-09-04 12:06:00', 106, 106, NULL, b'0', NULL),
(8007, 'Great scent, long lasting. (test review 7)', '2026-09-04 12:07:00', b'1', 4, '2026-09-04 12:07:00', 107, 107, NULL, b'0', NULL),
(8008, 'Great scent, long lasting. (test review 8)', '2026-09-04 12:08:00', b'1', 5, '2026-09-04 12:08:00', 108, 108, NULL, b'0', NULL),
(8009, 'Great scent, long lasting. (test review 9)', '2026-09-04 12:09:00', b'1', 3, '2026-09-04 12:09:00', 109, 109, NULL, b'0', NULL),
(8010, 'Great scent, long lasting. (test review 10)', '2026-09-04 12:10:00', b'1', 4, '2026-09-04 12:10:00', 110, 110, NULL, b'0', NULL),
(8011, 'Great scent, long lasting. (test review 11)', '2026-09-04 12:11:00', b'1', 5, '2026-09-04 12:11:00', 111, 111, NULL, b'0', NULL),
(8012, 'Great scent, long lasting. (test review 12)', '2026-09-04 12:12:00', b'1', 3, '2026-09-04 12:12:00', 112, 112, NULL, b'0', NULL),
(8013, 'Great scent, long lasting. (test review 13)', '2026-09-04 12:13:00', b'1', 4, '2026-09-04 12:13:00', 113, 113, NULL, b'0', NULL),
(8014, 'Great scent, long lasting. (test review 14)', '2026-09-04 12:14:00', b'1', 5, '2026-09-04 12:14:00', 114, 114, NULL, b'0', NULL),
(8015, 'Great scent, long lasting. (test review 15)', '2026-09-04 12:15:00', b'1', 3, '2026-09-04 12:15:00', 115, 115, NULL, b'0', NULL);

INSERT INTO tb_orders (id, created_at, phone, shipping_address, status, total_amount, updated_at, user_id, cancel_reason) VALUES
(901, '2026-09-05 13:01:00', '01230001', 'Town Hall, Phnom Penh', 'PENDING', 730.00, '2026-09-06 13:01:00', 101, NULL),
(902, '2026-09-05 13:02:00', '01230002', 'Town Hall, Phnom Penh', 'CONFIRMED', 740.00, '2026-09-06 13:02:00', 102, NULL),
(903, '2026-09-05 13:03:00', '01230003', 'Town Hall, Phnom Penh', 'PROCESSING', 750.00, '2026-09-06 13:03:00', 103, NULL),
(904, '2026-09-05 13:04:00', '01230004', 'Town Hall, Phnom Penh', 'SHIPPED', 760.00, '2026-09-06 13:04:00', 104, NULL),
(905, '2026-09-05 13:05:00', '01230005', 'Town Hall, Phnom Penh', 'DELIVERED', 770.00, '2026-09-06 13:05:00', 105, NULL),
(906, '2026-09-05 13:06:00', '01230006', 'Town Hall, Phnom Penh', 'CANCELLED', 780.00, '2026-09-06 13:06:00', 106, 'cancelled test' ),
(907, '2026-09-05 13:07:00', '01230007', 'Town Hall, Phnom Penh', 'PENDING', 790.00, '2026-09-06 13:07:00', 107, NULL),
(908, '2026-09-05 13:08:00', '01230008', 'Town Hall, Phnom Penh', 'CONFIRMED', 800.00, '2026-09-06 13:08:00', 108, NULL),
(909, '2026-09-05 13:09:00', '01230009', 'Town Hall, Phnom Penh', 'PROCESSING', 810.00, '2026-09-06 13:09:00', 109, NULL),
(910, '2026-09-05 13:10:00', '01230010', 'Town Hall, Phnom Penh', 'SHIPPED', 820.00, '2026-09-06 13:10:00', 110, NULL),
(911, '2026-09-05 13:11:00', '01230011', 'Town Hall, Phnom Penh', 'DELIVERED', 830.00, '2026-09-06 13:11:00', 111, NULL),
(912, '2026-09-05 13:12:00', '01230012', 'Town Hall, Phnom Penh', 'CANCELLED', 840.00, '2026-09-06 13:12:00', 112, 'cancelled test' ),
(913, '2026-09-05 13:13:00', '01230013', 'Town Hall, Phnom Penh', 'PENDING', 850.00, '2026-09-06 13:13:00', 113, NULL),
(914, '2026-09-05 13:14:00', '01230014', 'Town Hall, Phnom Penh', 'CONFIRMED', 860.00, '2026-09-06 13:14:00', 114, NULL),
(915, '2026-09-05 13:15:00', '01230015', 'Town Hall, Phnom Penh', 'PROCESSING', 870.00, '2026-09-06 13:15:00', 115, NULL);

INSERT INTO tb_order_items (id, brand, product_name, quantity, unit_price, subtotal, variant_size, order_id, variant_id) VALUES
(10001, 'TB-Le Labo', 'Santal 33 Eau de Parfum 1', 1, 86.00, 86.00, '50 ML', 901, 1001),
(10002, 'TB-Byredo', 'Santal 33 Eau de Parfum 2', 1, 87.00, 87.00, '100 ML', 902, 1002),
(10003, 'TB-Diptyque', 'Santal 33 Eau de Parfum 3', 1, 88.00, 88.00, '50 ML', 903, 1003),
(10004, 'TB-Maison Margiela', 'Santal 33 Eau de Parfum 4', 1, 89.00, 89.00, '100 ML', 904, 1004),
(10005, 'TB-Jo Malone', 'Santal 33 Eau de Parfum 5', 1, 90.00, 90.00, '50 ML', 905, 1005),
(10006, 'TB-Tom Ford', 'Santal 33 Eau de Parfum 6', 1, 91.00, 91.00, '100 ML', 906, 1006),
(10007, 'TB-Creed', 'Santal 33 Eau de Parfum 7', 1, 92.00, 92.00, '50 ML', 907, 1007),
(10008, 'TB-Acqua di Parma', 'Santal 33 Eau de Parfum 8', 1, 93.00, 93.00, '100 ML', 908, 1008),
(10009, 'TB-Penhaligon''s', 'Santal 33 Eau de Parfum 9', 1, 94.00, 94.00, '50 ML', 909, 1009),
(10010, 'TB-Frederic Malle', 'Santal 33 Eau de Parfum 10', 1, 95.00, 95.00, '100 ML', 910, 1010),
(10011, 'TB-Memo Paris', 'Santal 33 Eau de Parfum 11', 1, 96.00, 96.00, '50 ML', 911, 1011),
(10012, 'TB-Xerjoff', 'Santal 33 Eau de Parfum 12', 1, 97.00, 97.00, '100 ML', 912, 1012),
(10013, 'TB-Amouage', 'Santal 33 Eau de Parfum 13', 1, 98.00, 98.00, '50 ML', 913, 1013),
(10014, 'TB-Kilian', 'Santal 33 Eau de Parfum 14', 1, 99.00, 99.00, '100 ML', 914, 1014),
(10015, 'TB-Aerin', 'Santal 33 Eau de Parfum 15', 1, 100.00, 100.00, '50 ML', 915, 1015);

INSERT INTO tb_payments (id, amount, created_at, paid_at, payment_method, status, transaction_id, order_id, error_message) VALUES
(11001, 730.00, '2026-09-05 13:01:00', '2026-09-05 14:01:00', 'ABA', 'SUCCESSFUL', 'TXN-11001', 901, NULL),
(11002, 740.00, '2026-09-05 13:02:00', '2026-09-05 14:02:00', 'ACLEDA', 'PENDING', 'TXN-11002', 902, NULL),
(11003, 750.00, '2026-09-05 13:03:00', '2026-09-05 14:03:00', 'CASH', 'FAILED', 'TXN-11003', 903, 'gateway timeout test'),
(11004, 760.00, '2026-09-05 13:04:00', '2026-09-05 14:04:00', 'KHQR', 'SUCCESSFUL', 'TXN-11004', 904, NULL),
(11005, 770.00, '2026-09-05 13:05:00', '2026-09-05 14:05:00', 'ABA', 'REFUNDED', 'TXN-11005', 905, NULL),
(11006, 780.00, '2026-09-05 13:06:00', '2026-09-05 14:06:00', 'ACLEDA', 'SUCCESSFUL', 'TXN-11006', 906, NULL),
(11007, 790.00, '2026-09-05 13:07:00', '2026-09-05 14:07:00', 'CASH', 'PENDING', 'TXN-11007', 907, NULL),
(11008, 800.00, '2026-09-05 13:08:00', '2026-09-05 14:08:00', 'KHQR', 'FAILED', 'TXN-11008', 908, 'gateway timeout test'),
(11009, 810.00, '2026-09-05 13:09:00', '2026-09-05 14:09:00', 'ABA', 'SUCCESSFUL', 'TXN-11009', 909, NULL),
(11010, 820.00, '2026-09-05 13:10:00', '2026-09-05 14:10:00', 'ACLEDA', 'REFUNDED', 'TXN-11010', 910, NULL),
(11011, 830.00, '2026-09-05 13:11:00', '2026-09-05 14:11:00', 'CASH', 'SUCCESSFUL', 'TXN-11011', 911, NULL),
(11012, 840.00, '2026-09-05 13:12:00', '2026-09-05 14:12:00', 'KHQR', 'PENDING', 'TXN-11012', 912, NULL),
(11013, 850.00, '2026-09-05 13:13:00', '2026-09-05 14:13:00', 'ABA', 'FAILED', 'TXN-11013', 913, 'gateway timeout test'),
(11014, 860.00, '2026-09-05 13:14:00', '2026-09-05 14:14:00', 'ACLEDA', 'SUCCESSFUL', 'TXN-11014', 914, NULL),
(11015, 870.00, '2026-09-05 13:15:00', '2026-09-05 14:15:00', 'CASH', 'REFUNDED', 'TXN-11015', 915, NULL);

INSERT INTO tb_ai_conversations (id, created_at, title, updated_at, user_id, user_name) VALUES
(12001, '2026-09-07 15:01:00', 'Conversation 1 - scent discovery', '2026-09-07 15:01:00', 101, 'Test User 1'),
(12002, '2026-09-07 15:02:00', 'Conversation 2 - scent discovery', '2026-09-07 15:02:00', 102, 'Test User 2'),
(12003, '2026-09-07 15:03:00', 'Conversation 3 - scent discovery', '2026-09-07 15:03:00', 103, 'Test User 3'),
(12004, '2026-09-07 15:04:00', 'Conversation 4 - scent discovery', '2026-09-07 15:04:00', 104, 'Test User 4'),
(12005, '2026-09-07 15:05:00', 'Conversation 5 - scent discovery', '2026-09-07 15:05:00', 105, 'Test User 5'),
(12006, '2026-09-07 15:06:00', 'Conversation 6 - scent discovery', '2026-09-07 15:06:00', 106, 'Test User 6'),
(12007, '2026-09-07 15:07:00', 'Conversation 7 - scent discovery', '2026-09-07 15:07:00', 107, 'Test User 7'),
(12008, '2026-09-07 15:08:00', 'Conversation 8 - scent discovery', '2026-09-07 15:08:00', 108, 'Test User 8'),
(12009, '2026-09-07 15:09:00', 'Conversation 9 - scent discovery', '2026-09-07 15:09:00', 109, 'Test User 9'),
(12010, '2026-09-07 15:10:00', 'Conversation 10 - scent discovery', '2026-09-07 15:10:00', 110, 'Test User 10'),
(12011, '2026-09-07 15:11:00', 'Conversation 11 - scent discovery', '2026-09-07 15:11:00', 111, 'Test User 11'),
(12012, '2026-09-07 15:12:00', 'Conversation 12 - scent discovery', '2026-09-07 15:12:00', 112, 'Test User 12'),
(12013, '2026-09-07 15:13:00', 'Conversation 13 - scent discovery', '2026-09-07 15:13:00', 113, 'Test User 13'),
(12014, '2026-09-07 15:14:00', 'Conversation 14 - scent discovery', '2026-09-07 15:14:00', 114, 'Test User 14'),
(12015, '2026-09-07 15:15:00', 'Conversation 15 - scent discovery', '2026-09-07 15:15:00', 115, 'Test User 15');

INSERT INTO tb_ai_messages (id, created_at, message, sender, updated_at, conversation_id) VALUES
(13001, '2026-09-07 15:01:30', 'Where can I find a fragrance test for product 1? (test message)', 'USER', '2026-09-07 15:01:30', 12001),
(13002, '2026-09-07 15:02:30', 'Where can I find a fragrance test for product 2? (test message)', 'USER', '2026-09-07 15:02:30', 12002),
(13003, '2026-09-07 15:03:30', 'Where can I find a fragrance test for product 3? (test message)', 'USER', '2026-09-07 15:03:30', 12003),
(13004, '2026-09-07 15:04:30', 'Where can I find a fragrance test for product 4? (test message)', 'USER', '2026-09-07 15:04:30', 12004),
(13005, '2026-09-07 15:05:30', 'Where can I find a fragrance test for product 5? (test message)', 'USER', '2026-09-07 15:05:30', 12005),
(13006, '2026-09-07 15:06:30', 'Where can I find a fragrance test for product 6? (test message)', 'USER', '2026-09-07 15:06:30', 12006),
(13007, '2026-09-07 15:07:30', 'Where can I find a fragrance test for product 7? (test message)', 'USER', '2026-09-07 15:07:30', 12007),
(13008, '2026-09-07 15:08:30', 'Where can I find a fragrance test for product 8? (test message)', 'USER', '2026-09-07 15:08:30', 12008),
(13009, '2026-09-07 15:09:30', 'Where can I find a fragrance test for product 9? (test message)', 'USER', '2026-09-07 15:09:30', 12009),
(13010, '2026-09-07 15:10:30', 'Where can I find a fragrance test for product 10? (test message)', 'USER', '2026-09-07 15:10:30', 12010),
(13011, '2026-09-07 15:11:30', 'Where can I find a fragrance test for product 11? (test message)', 'USER', '2026-09-07 15:11:30', 12011),
(13012, '2026-09-07 15:12:30', 'Where can I find a fragrance test for product 12? (test message)', 'USER', '2026-09-07 15:12:30', 12012),
(13013, '2026-09-07 15:13:30', 'Where can I find a fragrance test for product 13? (test message)', 'USER', '2026-09-07 15:13:30', 12013),
(13014, '2026-09-07 15:14:30', 'Where can I find a fragrance test for product 14? (test message)', 'USER', '2026-09-07 15:14:30', 12014),
(13015, '2026-09-07 15:15:30', 'Where can I find a fragrance test for product 15? (test message)', 'USER', '2026-09-07 15:15:30', 12015);

INSERT INTO tb_ai_recommendations (id, created_at, position, reason, conversation_id, product_id) VALUES
(14001, '2026-09-07 15:01:45', 1, 'Recommended for your woody preference (test).', 12001, 101),
(14002, '2026-09-07 15:02:45', 2, 'Recommended for your floral preference (test).', 12002, 102),
(14003, '2026-09-07 15:03:45', 3, 'Recommended for your oriental preference (test).', 12003, 103),
(14004, '2026-09-07 15:04:45', 4, 'Recommended for your fresh preference (test).', 12004, 104),
(14005, '2026-09-07 15:05:45', 5, 'Recommended for your chypre preference (test).', 12005, 105),
(14006, '2026-09-07 15:06:45', 6, 'Recommended for your citrus preference (test).', 12006, 106),
(14007, '2026-09-07 15:07:45', 7, 'Recommended for your gourmand preference (test).', 12007, 107),
(14008, '2026-09-07 15:08:45', 8, 'Recommended for your green preference (test).', 12008, 108),
(14009, '2026-09-07 15:09:45', 9, 'Recommended for your spicy preference (test).', 12009, 109),
(14010, '2026-09-07 15:10:45', 10, 'Recommended for your leather preference (test).', 12010, 110),
(14011, '2026-09-07 15:11:45', 11, 'Recommended for your aquatic preference (test).', 12011, 111),
(14012, '2026-09-07 15:12:45', 12, 'Recommended for your powdery preference (test).', 12012, 112),
(14013, '2026-09-07 15:13:45', 13, 'Recommended for your fruity preference (test).', 12013, 113),
(14014, '2026-09-07 15:14:45', 14, 'Recommended for your amber preference (test).', 12014, 114),
(14015, '2026-09-07 15:15:45', 15, 'Recommended for your boozy preference (test).', 12015, 115);

INSERT INTO tb_ai_recommendation_clicks (id, clicked_at, recommendation_id, user_id) VALUES
(15001, '2026-09-07 16:01:00', 14001, 101),
(15002, '2026-09-07 16:02:00', 14002, 102),
(15003, '2026-09-07 16:03:00', 14003, 103),
(15004, '2026-09-07 16:04:00', 14004, 104),
(15005, '2026-09-07 16:05:00', 14005, 105),
(15006, '2026-09-07 16:06:00', 14006, 106),
(15007, '2026-09-07 16:07:00', 14007, 107),
(15008, '2026-09-07 16:08:00', 14008, 108),
(15009, '2026-09-07 16:09:00', 14009, 109),
(15010, '2026-09-07 16:10:00', 14010, 110),
(15011, '2026-09-07 16:11:00', 14011, 111),
(15012, '2026-09-07 16:12:00', 14012, 112),
(15013, '2026-09-07 16:13:00', 14013, 113),
(15014, '2026-09-07 16:14:00', 14014, 114),
(15015, '2026-09-07 16:15:00', 14015, 115);

INSERT INTO tb_support_tickets (id, agent_id, agent_name, created_at, first_replied_at, order_id, priority, reason, resolved_at, status, summary, ticket_number, updated_at, conversation_id, user_id) VALUES
(16001, NULL, 'AI Support Team', '2026-09-08 09:01:00', NULL, 901, 'LOW', 'Question about order delivery (test ticket 1).', NULL, 'OPEN', 'Customer asking about Le Labo delivery (test).', 'CS-16001', '2026-09-08 09:01:00', 12001, 101),
(16002, NULL, 'AI Support Team', '2026-09-08 09:02:00', NULL, 902, 'NORMAL', 'Question about order delivery (test ticket 2).', NULL, 'PENDING', 'Customer asking about Byredo delivery (test).', 'CS-16002', '2026-09-08 09:02:00', 12002, 102),
(16003, NULL, 'AI Support Team', '2026-09-08 09:03:00', '2026-09-08 09:00:00', 903, 'URGENT', 'Question about order delivery (test ticket 3).', NULL, 'IN_PROGRESS', 'Customer asking about Diptyque delivery (test).', 'CS-16003', '2026-09-08 09:03:00', 12003, 103),
(16004, NULL, 'AI Support Team', '2026-09-08 09:04:00', '2026-09-08 09:00:00', 904, 'NORMAL', 'Question about order delivery (test ticket 4).', '2026-09-08 18:00:00', 'RESOLVED', 'Customer asking about Maison Margiela delivery (test).', 'CS-16004', '2026-09-08 09:04:00', 12004, 104),
(16005, NULL, 'AI Support Team', '2026-09-08 09:05:00', NULL, 905, 'LOW', 'Question about order delivery (test ticket 5).', NULL, 'OPEN', 'Customer asking about Jo Malone delivery (test).', 'CS-16005', '2026-09-08 09:05:00', 12005, 105),
(16006, NULL, 'AI Support Team', '2026-09-08 09:06:00', NULL, 906, 'LOW', 'Question about order delivery (test ticket 6).', NULL, 'OPEN', 'Customer asking about Tom Ford delivery (test).', 'CS-16006', '2026-09-08 09:06:00', 12006, 106),
(16007, NULL, 'AI Support Team', '2026-09-08 09:07:00', NULL, 907, 'NORMAL', 'Question about order delivery (test ticket 7).', NULL, 'PENDING', 'Customer asking about Creed delivery (test).', 'CS-16007', '2026-09-08 09:07:00', 12007, 107),
(16008, NULL, 'AI Support Team', '2026-09-08 09:08:00', '2026-09-08 09:00:00', 908, 'URGENT', 'Question about order delivery (test ticket 8).', NULL, 'IN_PROGRESS', 'Customer asking about Acqua di Parma delivery (test).', 'CS-16008', '2026-09-08 09:08:00', 12008, 108),
(16009, NULL, 'AI Support Team', '2026-09-08 09:09:00', '2026-09-08 09:00:00', 909, 'NORMAL', 'Question about order delivery (test ticket 9).', '2026-09-08 18:00:00', 'RESOLVED', 'Customer asking about Penhaligon''s delivery (test).', 'CS-16009', '2026-09-08 09:09:00', 12009, 109),
(16010, NULL, 'AI Support Team', '2026-09-08 09:10:00', NULL, 910, 'LOW', 'Question about order delivery (test ticket 10).', NULL, 'OPEN', 'Customer asking about Frederic Malle delivery (test).', 'CS-16010', '2026-09-08 09:10:00', 12010, 110),
(16011, NULL, 'AI Support Team', '2026-09-08 09:11:00', NULL, 911, 'LOW', 'Question about order delivery (test ticket 11).', NULL, 'OPEN', 'Customer asking about Memo Paris delivery (test).', 'CS-16011', '2026-09-08 09:11:00', 12011, 111),
(16012, NULL, 'AI Support Team', '2026-09-08 09:12:00', NULL, 912, 'NORMAL', 'Question about order delivery (test ticket 12).', NULL, 'PENDING', 'Customer asking about Xerjoff delivery (test).', 'CS-16012', '2026-09-08 09:12:00', 12012, 112),
(16013, NULL, 'AI Support Team', '2026-09-08 09:13:00', '2026-09-08 09:00:00', 913, 'URGENT', 'Question about order delivery (test ticket 13).', NULL, 'IN_PROGRESS', 'Customer asking about Amouage delivery (test).', 'CS-16013', '2026-09-08 09:13:00', 12013, 113),
(16014, NULL, 'AI Support Team', '2026-09-08 09:14:00', '2026-09-08 09:00:00', 914, 'NORMAL', 'Question about order delivery (test ticket 14).', '2026-09-08 18:00:00', 'RESOLVED', 'Customer asking about Kilian delivery (test).', 'CS-16014', '2026-09-08 09:14:00', 12014, 114),
(16015, NULL, 'AI Support Team', '2026-09-08 09:15:00', NULL, 915, 'LOW', 'Question about order delivery (test ticket 15).', NULL, 'OPEN', 'Customer asking about Aerin delivery (test).', 'CS-16015', '2026-09-08 09:15:00', 12015, 115);

INSERT INTO tb_support_ticket_notes (id, author_name, content, created_at, updated_at, ticket_id) VALUES
(17001, 'AI Support Team', 'Chat history attached to ticket (test note 1).', '2026-09-08 10:01:00', '2026-09-08 10:01:00', 16001),
(17002, 'AI Support Team', 'Chat history attached to ticket (test note 2).', '2026-09-08 10:02:00', '2026-09-08 10:02:00', 16002),
(17003, 'AI Support Team', 'Chat history attached to ticket (test note 3).', '2026-09-08 10:03:00', '2026-09-08 10:03:00', 16003),
(17004, 'AI Support Team', 'Chat history attached to ticket (test note 4).', '2026-09-08 10:04:00', '2026-09-08 10:04:00', 16004),
(17005, 'AI Support Team', 'Chat history attached to ticket (test note 5).', '2026-09-08 10:05:00', '2026-09-08 10:05:00', 16005),
(17006, 'AI Support Team', 'Chat history attached to ticket (test note 6).', '2026-09-08 10:06:00', '2026-09-08 10:06:00', 16006),
(17007, 'AI Support Team', 'Chat history attached to ticket (test note 7).', '2026-09-08 10:07:00', '2026-09-08 10:07:00', 16007),
(17008, 'AI Support Team', 'Chat history attached to ticket (test note 8).', '2026-09-08 10:08:00', '2026-09-08 10:08:00', 16008),
(17009, 'AI Support Team', 'Chat history attached to ticket (test note 9).', '2026-09-08 10:09:00', '2026-09-08 10:09:00', 16009),
(17010, 'AI Support Team', 'Chat history attached to ticket (test note 10).', '2026-09-08 10:10:00', '2026-09-08 10:10:00', 16010),
(17011, 'AI Support Team', 'Chat history attached to ticket (test note 11).', '2026-09-08 10:11:00', '2026-09-08 10:11:00', 16011),
(17012, 'AI Support Team', 'Chat history attached to ticket (test note 12).', '2026-09-08 10:12:00', '2026-09-08 10:12:00', 16012),
(17013, 'AI Support Team', 'Chat history attached to ticket (test note 13).', '2026-09-08 10:13:00', '2026-09-08 10:13:00', 16013),
(17014, 'AI Support Team', 'Chat history attached to ticket (test note 14).', '2026-09-08 10:14:00', '2026-09-08 10:14:00', 16014),
(17015, 'AI Support Team', 'Chat history attached to ticket (test note 15).', '2026-09-08 10:15:00', '2026-09-08 10:15:00', 16015);

INSERT INTO tb_settings (id, created_at, description, setting_key, updated_at, value) VALUES
(18001, '2026-09-01 07:00:00', 'Test setting 1 - site_name.', 'site_name', '2026-09-01 07:00:00', 'AI Perfume Shop'),
(18002, '2026-09-01 07:00:00', 'Test setting 2 - currency.', 'currency', '2026-09-01 07:00:00', 'USD'),
(18003, '2026-09-01 07:00:00', 'Test setting 3 - support_email.', 'support_email', '2026-09-01 07:00:00', 'support@perfumeshop.test'),
(18004, '2026-09-01 07:00:00', 'Test setting 4 - order_prefix.', 'order_prefix', '2026-09-01 07:00:00', 'ORD'),
(18005, '2026-09-01 07:00:00', 'Test setting 5 - ticket_prefix.', 'ticket_prefix', '2026-09-01 07:00:00', 'CS'),
(18006, '2026-09-01 07:00:00', 'Test setting 6 - tax_rate.', 'tax_rate', '2026-09-01 07:00:00', '0.10'),
(18007, '2026-09-01 07:00:00', 'Test setting 7 - shipping_fee.', 'shipping_fee', '2026-09-01 07:00:00', '5.00'),
(18008, '2026-09-01 07:00:00', 'Test setting 8 - free_shipping_threshold.', 'free_shipping_threshold', '2026-09-01 07:00:00', '150.00'),
(18009, '2026-09-01 07:00:00', 'Test setting 9 - ai_model.', 'ai_model', '2026-09-01 07:00:00', 'gpt-4o-mini'),
(18010, '2026-09-01 07:00:00', 'Test setting 10 - ai_temperature.', 'ai_temperature', '2026-09-01 07:00:00', '0.3'),
(18011, '2026-09-01 07:00:00', 'Test setting 11 - maintenance_mode.', 'maintenance_mode', '2026-09-01 07:00:00', 'false'),
(18012, '2026-09-01 07:00:00', 'Test setting 12 - telegram_enabled.', 'telegram_enabled', '2026-09-01 07:00:00', 'true'),
(18013, '2026-09-01 07:00:00', 'Test setting 13 - max_review_length.', 'max_review_length', '2026-09-01 07:00:00', '500'),
(18014, '2026-09-01 07:00:00', 'Test setting 14 - stock_low_threshold.', 'stock_low_threshold', '2026-09-01 07:00:00', '10'),
(18015, '2026-09-01 07:00:00', 'Test setting 15 - vat_number.', 'vat_number', '2026-09-01 07:00:00', 'KH-000-001');

