-- Seed the supplied 25-product development catalog into the MySQL backend.
-- Re-runnable: brands/categories are reused; products, variants and images are
-- matched by brand/name, SKU/size and URL respectively. Existing history stays.

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_real_catalog (
  brand_name VARCHAR(100) NOT NULL,
  product_name VARCHAR(200) NOT NULL,
  category_name VARCHAR(100) NOT NULL,
  size_ml INT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL,
  sku VARCHAR(100) NOT NULL,
  image_url VARCHAR(1500) NOT NULL,
  gender VARCHAR(20) NOT NULL,
  description TEXT NOT NULL,
  fragrance_family VARCHAR(100) NOT NULL,
  top_notes TEXT NOT NULL,
  heart_notes TEXT NOT NULL,
  base_notes TEXT NOT NULL
);

INSERT INTO tmp_real_catalog VALUES
('Chanel','N°5 Eau de Parfum','Women''s Perfume',100,185,25,'CHN-N5-100','https://www.chanel.com/images//t_fragrance//q_auto,f_jpg,fl_lossy,dpr_2/w_1920/n-5-eau-de-parfum-spray-3-4fl-oz--packshot-default-125530-8820672233502.jpg','WOMEN','An iconic floral aldehydic fragrance with a luminous citrus opening, an elegant bouquet of rose and jasmine, and a soft vanilla-rich drydown. Timeless, sophisticated, and unmistakably Chanel.','Floral Aldehydic','Lemon, Bergamot, Aldehydes','May Rose, Jasmine, Ylang-Ylang','Bourbon Vanilla, Sandalwood, Vetiver'),
('Chanel','Coco Mademoiselle Eau de Parfum','Women''s Perfume',100,180,30,'CHN-CM-100','https://www.chanel.com/images//t_one//w_0.51,h_0.51,c_crop/q_auto:good,f_jpg,fl_lossy,dpr_1.2/w_1240/coco-mademoiselle-eau-de-parfum-spray-3-4fl-oz--packshot-default-116520-8841592537118.jpg','WOMEN','A refined modern classic blending sparkling citrus with an elegant rose-and-jasmine heart and a warm patchouli-vanilla base. Sophisticated, polished, and versatile.','Chypre Floral','Orange, Bergamot, Mandarin','Rose, Jasmine, Ylang-Ylang','Patchouli, Vetiver, Vanilla, White Musk'),
('Dior','Sauvage Eau de Toilette','Men''s Perfume',100,145,35,'DIO-SVG-100','https://www.dior.com/dw/image/v2/BGXS_PRD/on/demandware.static/-/Sites-master_dior/default/dwfc145bb2/Y0685240/Y0685240_F068524009_E01_RHC.jpg?sw=800','MEN','A fresh, powerful fragrance built around Calabrian bergamot and spicy pepper, followed by an ambery-woody trail of ambroxan, elemi, and woods. Clean, energetic, and confident.','Aromatic','Calabrian Bergamot, Sichuan Pepper','Lavender, Elemi','Ambroxan, Cedar, Woody Notes'),
('YSL','Black Opium Eau de Parfum','Women''s Perfume',90,137,25,'YSL-BO-090','https://www.yslbeauty.co.uk/dw/image/v2/AAQP_PRD/on/demandware.static/-/Sites-ysl-master-catalog/default/dwb7d6ff19/images/PACKSHOTS/FRAGRANCE/FOR_HER/WW-40701YSL_black_opium_edp/3365440787919_black-opium-eau-de-parfum_50ml_Alt1.jpg?q=85&sfrm=png&sh=320&sm=cut&sw=320','WOMEN','An addictive coffee-floral fragrance combining roasted coffee with luminous white flowers and a creamy vanilla finish. Bold, sensual, and designed for an evening-ready signature.','Amber Vanilla','Pink Pepper, Pear, Orange Blossom','Coffee, Jasmine, Bitter Almond','Vanilla, Patchouli, Cedar'),
('YSL','Libre Eau de Parfum','Women''s Perfume',90,160,30,'YSL-LIB-090','https://www.yslbeauty.com/dw/image/v2/BDCR_PRD/on/demandware.static/-/Sites-ysl-master-catalog/en/dw469e11e9/square/Fragrance/Libre_EDP/v23614272648418_libre_eau_de_parfum_50ml.webp?q=85&sfrm=png&sh=320&sm=cut&sw=320','WOMEN','A modern floral fragrance balancing aromatic French lavender with radiant Moroccan orange blossom, supported by warm vanilla and musk. Confident, elegant, and contemporary.','Floral','Lavender, Mandarin Orange, Bergamot','Orange Blossom, Jasmine, Lavender','Madagascar Vanilla, Musk, Cedar'),
('Lancôme','La Vie Est Belle Eau de Parfum','Women''s Perfume',100,145,28,'LAN-LVB-100','https://f.nooncdn.com/p/pzsku/Z37FBD6B5BF5331130229Z/45/_/1779712323/dcfa6e65-b31f-48f3-83d1-c5592fdf8075.jpg','WOMEN','A sweet, elegant floral gourmand centered on iris, jasmine, and orange blossom, softened by praline and vanilla over a warm patchouli base. Feminine, joyful, and comforting.','Gourmand Floral','Blackcurrant, Pear','Iris, Jasmine, Orange Blossom','Praline, Vanilla, Patchouli, Tonka Bean'),
('Carolina Herrera','Good Girl Eau de Parfum','Women''s Perfume',80,140,22,'CH-GG-080','https://www.carolinaherrera.com/dw/image/v2/BBJB_PRD/on/demandware.static/-/Sites-master-catalog/default/dw7e5b0c8c/images/hi-res/GoodGirl_80ml.jpg?q=85&sfrm=jpg','WOMEN','A sensual ambery floral built around the contrast of bright jasmine, creamy tuberose, bitter almond, coffee, cocoa, and roasted tonka bean. Dramatic, sophisticated, and distinctive.','Amber Floral','Almond, Coffee','Jasmine Sambac, Tuberose','Tonka Bean, Cocoa, Vanilla'),
('Giorgio Armani','Sì Eau de Parfum','Women''s Perfume',100,150,24,'ARM-SI-100','https://img.kingpowerclick.com/cdn-cgi/image/format=auto/kingpower-com/image/upload/w_640/v1773799963/prod/1076256-L1.jpg','WOMEN','An elegant modern chypre combining juicy blackcurrant with delicate rose and freesia, resting on a sophisticated vanilla, amber, and woody base. Refined and effortlessly feminine.','Chypre','Blackcurrant, Pear','Rose, Freesia','Vanilla, Amber, Patchouli, Woody Notes'),
('Tom Ford','Black Orchid Parfum','Unisex Perfume',100,195,15,'TF-BO-100','https://www.tomfordbeauty.com/cdn/shop/files/tf_sku_T90F01_2000x2000_0.png?v=1790416930&width=2000','UNISEX','A dark, luxurious floral-amber fragrance that magnifies black orchid with black truffle, rum, plum, ylang-ylang, and patchouli. Rich, mysterious, and opulent.','Amber Floral','Black Truffle, Black Plum','Ylang-Ylang, Black Orchid, Rum Absolute','Patchouli, Woody Notes'),
('Jo Malone','English Pear & Freesia Cologne','Women''s Perfume',100,165,20,'JM-EPF-100','https://media.thairath.co.th/image/yKDTvAV06dp1V2k1vVcd4QzKs1a2hqtUWhqzRK3CanvKIQ4G88e9vA1.webp?width=320','WOMEN','A fresh, elegant fragrance inspired by ripe English pears and delicate white freesia, softened by a deep patchouli base. Fresh, graceful, and understated.','Fruity Floral','King William Pear','Freesia','Patchouli, Amber, Woods'),
('Maison Francis Kurkdjian','Baccarat Rouge 540 Eau de Parfum','Unisex Perfume',70,325,12,'MFK-BR540-070','https://www.franciskurkdjian.com/dw/image/v2/BJSB_PRD/on/demandware.static/-/Sites-mfk-master-catalog/default/dwa46019b3/BACCARAT_ROUGE_540/FRAGRANCE/3700559603116_BR540_EDP_70ML_1.png?q=85&sfrm=png&sh=520&strip=true&sw=520','UNISEX','A luminous woody-amber fragrance known for its airy sweetness and striking mineral character. Saffron and jasmine meet amberwood and cedar for a sophisticated, highly recognizable trail.','Woody Amber','Saffron, Jasmine','Amberwood, Ambergris Accord','Cedar, Fir Resin'),
('Dior','Miss Dior Eau de Parfum','Women''s Perfume',100,165,25,'DIO-MD-100','https://fenwick.co.uk/cdn/shop/files/A539756.jpg','WOMEN','A contemporary floral vanilla fragrance centered on honeyed Centifolia rose, creamy vanilla, almond-toned tonka bean, and milky sandalwood. Romantic, warm, and elegant.','Floral','Blood Orange, Mandarin','Centifolia Rose, Jasmine','Vanilla, Tonka Bean, Sandalwood, Patchouli'),
('Viktor&Rolf','Flowerbomb Eau de Parfum','Women''s Perfume',100,180,20,'VR-FB-100','https://media.douglas.si/media/image/08/0e/5c/L4072812.jpg','WOMEN','An opulent floral bouquet blending tea and bergamot with jasmine, rose, freesia, and orchid, grounded by warm patchouli and musk. Rich, feminine, and memorable.','Floral','Tea, Bergamot, Osmanthus','Jasmine, Rose, Freesia, Orchid','Patchouli, Musk, Vanilla'),
('Marc Jacobs','Daisy Eau de Toilette','Women''s Perfume',100,140,25,'MJ-DAISY-100','https://commons.wikimedia.org/wiki/Special:Redirect/file/Daisy_by_Marc_Jacobs.jpg','WOMEN','A fresh and youthful floral fragrance opening with wild berries and violet leaves, unfolding into white violet and jasmine before settling into a soft sandalwood and musk base.','Floral','Violet Leaves, Wild Strawberries','Violet Petals, Jasmine, Gardenia','Musk, Sandalwood, Vanilla'),
('Burberry','Her Eau de Parfum','Women''s Perfume',100,155,22,'BUR-HER-100','https://assets.burberry.com/is/image/Burberryltd/F69B2B97-3537-4C68-B79E-6DC8650F9CCA?%24BBY_V3_SL_1%24=&hei=1500&wid=1501','WOMEN','A modern fruity-floral fragrance pairing bright berries with violet and jasmine, finished with a warm amber, musk, and woody trail. Playful, polished, and contemporary.','Fruity Floral','Strawberry, Raspberry, Blackberry','Violet, Jasmine','Musk, Amber, Woods'),
('Prada','Paradoxe Eau de Parfum','Women''s Perfume',90,160,25,'PRA-PDX-090','https://www.prada-beauty.com/dw/image/v2/AAFM_PRD/on/demandware.static/-/Sites-prada-master-catalog/default/dw46272785/fragrance/women/MPL01610/MPL01610-NEW/2026-10ml-paradoxe-eau-de-parfum-main.webp?q=70&sfrm=jpg&sh=358&sm=cut&sw=358','WOMEN','A luminous floral-amber fragrance that contrasts fresh neroli with warm amber and intense musk. Modern, clean, and subtly sensual.','Floral Amber','Fresh Neroli Bud, Bergamot','Sensual Amber, Orange Blossom','Intense Musk, Vanilla'),
('Gucci','Flora Gorgeous Gardenia Eau de Parfum','Women''s Perfume',100,150,20,'GUC-FGG-100','https://p.turbosquid.com/ts-thumb/VJ/li0jvT/JY/1/png/1714041837/1920x1080/fit_q87/5022ff492b5941b53934b38bf6830b339d2b163d/1.jpg','WOMEN','A joyful white floral fragrance led by gardenia and jasmine, introduced by sparkling fruit and softened with a delicate brown-sugar sweetness. Bright, feminine, and playful.','White Floral','Pear Blossom, Italian Mandarin, Red Berries','White Gardenia, Jasmine Grandiflorum, Frangipani','Brown Sugar, Patchouli'),
('Jean Paul Gaultier','Le Male Eau de Toilette','Men''s Perfume',100,135,25,'JPG-LM-100','https://medias.jeanpaulgaultier.com/cdn-cgi/image/width%3D3840%2Cquality%3D90%2Cformat%3Davif/medias/sys_master/images/hd3/h4c/10688839483422/flacon-pdp-le-male-edt-jean-paul-gaultier/flacon-pdp-le-male-edt-jean-paul-gaultier.png','MEN','An iconic aromatic amber fragrance combining cool mint with comforting lavender and a sensual vanilla drydown. Fresh at first, warm and creamy as it develops.','Amber Fougere','Mint','Lavender','Vanilla'),
('Rabanne','1 Million Eau de Toilette','Men''s Perfume',100,120,30,'RBN-1M-100','https://img.kingpowerclick.com/cdn-cgi/image/format=auto/kingpower-com/image/upload/w_640/v1761040457/prod/219954-L1.jpg','MEN','A bold spicy-woody fragrance combining bright citrus and mint with cinnamon and rose before settling into leather, amber, and woody notes. Warm, confident, and attention-grabbing.','Spicy Leather','Blood Mandarin, Grapefruit, Mint','Cinnamon, Rose, Spices','Leather, Amber, Patchouli, Woods'),
('Versace','Eros Eau de Parfum','Men''s Perfume',100,125,30,'VER-EROS-100','https://www.versace.com/dw/image/v2/BGWN_PRD/on/demandware.static/-/Sites-ver-master-catalog/default/dwc8c800a3/original/90_R740110-R100MLS_RNUL_20_ErosEDP100ml-Eros-Versace-online-store_1_5.jpg?q=85&strip=true&sw=850','MEN','A vibrant aromatic fragrance combining mint and citrus with aromatic herbs and a warm vanilla-tonka base. Fresh, sweet, and powerful with a distinctly Mediterranean character.','Aromatic Fougere','Mint, Lemon, Green Apple','Geranium, Ambroxan, Clary Sage','Vanilla, Tonka Bean, Cedar, Amber'),
('Creed','Aventus Eau de Parfum','Men''s Perfume',100,495,10,'CRD-AVE-100','https://www.creedfragrances.co.uk/cdn/shop/files/Aventus_100ml_Mobile_1.jpg?v=1788440017&width=375','MEN','A sophisticated fruity-woody fragrance famous for its pineapple opening, smoky birch heart, and refined musk, oakmoss, and ambergris-inspired base. Confident and luxurious.','Fruity Woody','Pineapple, Bergamot, Blackcurrant, Apple','Birch, Pink Pepper, Jasmine, Patchouli','Musk, Oakmoss, Ambergris, Vanilla'),
('Hermès','Terre d''Hermès Eau de Toilette','Men''s Perfume',100,125,20,'HER-TDH-100','https://img.kingpowerclick.com/cdn-cgi/image/format=auto/kingpower-com/image/upload/w_640/v1539235469/prod/115275-L1.jpg','MEN','A refined woody-citrus composition pairing bright orange with pepper and mineral facets, followed by cedar and vetiver. Dry, elegant, earthy, and distinctly masculine.','Woody Citrus','Orange, Grapefruit','Pepper, Geranium, Flint','Cedar, Vetiver, Patchouli, Benzoin'),
('Narciso Rodriguez','For Her Eau de Parfum','Women''s Perfume',100,140,20,'NR-FH-100','https://product-images.metro.ca/images/h3b/h49/13795284058142.jpg','WOMEN','A sophisticated musky floral fragrance built around rose and peach, with a heart of musk and a warm patchouli, amber, and sandalwood foundation. Elegant, sensual, and understated.','Musky Floral','Peach, Rose','Musk, Rose, Amber','Patchouli, Sandalwood'),
('Dolce & Gabbana','Light Blue Eau de Toilette','Women''s Perfume',100,110,30,'DGLB-100','https://img.kingpowerclick.com/cdn-cgi/image/format=auto/kingpower-com/image/upload/w_640/v1755655841/prod/1095900-L1.jpg','WOMEN','A bright Mediterranean citrus fragrance blending crisp lemon and green apple with delicate floral notes and a clean cedarwood and musk base. Fresh, breezy, and ideal for warm weather.','Citrus','Sicilian Lemon, Apple, Cedar Leaves, Bellflower','White Rose, Jasmine, Bamboo','Cedarwood, Musk, Amber'),
('Givenchy','L''Interdit Eau de Parfum','Women''s Perfume',80,135,20,'GIV-LINT-080','https://www.scentdecant.com/cdn/shop/files/L-Interdit-edp-2018.jpg?v=1784169705','WOMEN','A striking white-floral fragrance contrasting orange blossom, jasmine, and tuberose with dark vetiver and patchouli. Elegant yet daring, with a memorable floral-woody trail.','Floral Woody','Pear, Bergamot','Orange Blossom, Tuberose, Jasmine','Patchouli, Vetiver, Vanilla, Ambroxan');

-- Reuse existing brand and category rows, or add them if missing.
INSERT INTO tb_brands (name, is_active, created_at, updated_at)
SELECT DISTINCT brand_name, 1, NOW(), NOW() FROM tmp_real_catalog
ON DUPLICATE KEY UPDATE is_active = 1, updated_at = NOW();

INSERT INTO tb_categories (name, description, image_url, is_active, created_at, updated_at)
SELECT category_name,
       CONCAT('Perfumes in the ', LOWER(category_name), ' collection.'),
       MIN(image_url), 1, NOW(), NOW()
FROM tmp_real_catalog
GROUP BY category_name
ON DUPLICATE KEY UPDATE description = VALUES(description),
                        image_url = VALUES(image_url), is_active = 1,
                        updated_at = NOW();

-- Retire only the old placeholder fixture products, preserving order history.
UPDATE tb_products p
JOIN tb_brands b ON b.id = p.brand_id
SET p.is_active = 0, p.updated_at = NOW()
WHERE p.name LIKE 'Santal 33 Eau de Parfum %' AND b.name LIKE '% Test';
UPDATE tb_product_variants v
JOIN tb_products p ON p.id = v.product_id
JOIN tb_brands b ON b.id = p.brand_id
SET v.is_active = 0, v.updated_at = NOW()
WHERE p.name LIKE 'Santal 33 Eau de Parfum %' AND b.name LIKE '% Test';
UPDATE tb_categories c SET c.is_active = 0, c.updated_at = NOW()
WHERE c.name LIKE 'Category %';
UPDATE tb_brands b SET b.is_active = 0, b.updated_at = NOW()
WHERE b.name LIKE '% Test';

-- Update matching real records; insert missing products without duplicating them.
UPDATE tb_products p
JOIN tb_brands b ON b.id = p.brand_id
JOIN tmp_real_catalog s ON s.brand_name = b.name AND s.product_name = p.name
JOIN tb_categories c ON c.name = s.category_name
SET p.category_id = c.id, p.is_active = 1,
    p.description = s.description,
    p.updated_at = NOW();

INSERT INTO tb_products (name, description, is_active, created_at, updated_at, brand_id, category_id)
SELECT s.product_name, s.description, 1, NOW(), NOW(), b.id, c.id
FROM tmp_real_catalog s
JOIN tb_brands b ON b.name = s.brand_name
JOIN tb_categories c ON c.name = s.category_name
WHERE NOT EXISTS (
  SELECT 1 FROM tb_products p WHERE p.name = s.product_name AND p.brand_id = b.id
);

-- Keep the supplied single size/price/stock variant as the active catalog option.
UPDATE tb_product_variants v
JOIN tb_products p ON p.id = v.product_id
JOIN tb_brands b ON b.id = p.brand_id
JOIN tmp_real_catalog s ON s.brand_name = b.name AND s.product_name = p.name
SET v.is_active = 0, v.updated_at = NOW();
UPDATE tb_product_variants v
JOIN tb_products p ON p.id = v.product_id
JOIN tb_brands b ON b.id = p.brand_id
JOIN tmp_real_catalog s ON s.brand_name = b.name AND s.product_name = p.name
SET v.sku = s.sku, v.size_ml = s.size_ml, v.price = s.price,
    v.stock = s.stock, v.is_active = 1, v.updated_at = NOW()
WHERE v.size_ml = s.size_ml;
INSERT INTO tb_product_variants (product_id, sku, size_ml, price, stock, is_active, created_at, updated_at)
SELECT p.id, s.sku, s.size_ml, s.price, s.stock, 1, NOW(), NOW()
FROM tmp_real_catalog s
JOIN tb_brands b ON b.name = s.brand_name
JOIN tb_products p ON p.brand_id = b.id AND p.name = s.product_name
WHERE NOT EXISTS (
  SELECT 1 FROM tb_product_variants v WHERE v.product_id = p.id AND v.size_ml = s.size_ml
);

-- Set the provided bottle image as each product's primary image.
UPDATE tb_product_images i
JOIN tb_products p ON p.id = i.product_id
JOIN tb_brands b ON b.id = p.brand_id
JOIN tmp_real_catalog s ON s.brand_name = b.name AND s.product_name = p.name
SET i.is_primary = 0;
INSERT INTO tb_product_images (image_url, display_order, is_primary, created_at, product_id)
SELECT s.image_url, 1, 1, NOW(), p.id
FROM tmp_real_catalog s
JOIN tb_brands b ON b.name = s.brand_name
JOIN tb_products p ON p.brand_id = b.id AND p.name = s.product_name
WHERE NOT EXISTS (
  SELECT 1 FROM tb_product_images i WHERE i.product_id = p.id AND i.image_url = s.image_url
);
UPDATE tb_product_images i
JOIN tb_products p ON p.id = i.product_id
JOIN tb_brands b ON b.id = p.brand_id
JOIN tmp_real_catalog s ON s.brand_name = b.name AND s.product_name = p.name AND s.image_url = i.image_url
SET i.is_primary = 1, i.display_order = 1;

-- Gender and note pyramids follow the supplied table. Broad scent families are
-- mapped from its professional descriptions to support fragrance matching.
INSERT INTO tb_fragrance_profiles (gender, fragrance_family, frag_notes, product_id)
SELECT s.gender, s.fragrance_family, CONCAT_WS(', ', s.top_notes, s.heart_notes, s.base_notes), p.id
FROM tmp_real_catalog s
JOIN tb_brands b ON b.name = s.brand_name
JOIN tb_products p ON p.brand_id = b.id AND p.name = s.product_name
WHERE NOT EXISTS (SELECT 1 FROM tb_fragrance_profiles f WHERE f.product_id = p.id);
UPDATE tb_fragrance_profiles f
JOIN tb_products p ON p.id = f.product_id
JOIN tb_brands b ON b.id = p.brand_id
JOIN tmp_real_catalog s ON s.brand_name = b.name AND s.product_name = p.name
SET f.gender = s.gender, f.fragrance_family = s.fragrance_family,
    f.frag_notes = CONCAT_WS(', ', s.top_notes, s.heart_notes, s.base_notes);

DROP TEMPORARY TABLE tmp_real_catalog;
COMMIT;
