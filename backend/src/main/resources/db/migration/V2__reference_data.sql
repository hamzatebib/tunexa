-- Gouvernorats (codes ISO 3166-2:TN). Zones de livraison : 1 = Grand Tunis, 2 = Nord / Centre / Sahel, 3 = Sud
INSERT INTO governorates (code, name_fr, name_ar, name_en, shipping_zone) VALUES
('TN-11', 'Tunis',       'تونس',      'Tunis',       1),
('TN-12', 'Ariana',      'أريانة',    'Ariana',      1),
('TN-13', 'Ben Arous',   'بن عروس',   'Ben Arous',   1),
('TN-14', 'Manouba',     'منوبة',     'Manouba',     1),
('TN-21', 'Nabeul',      'نابل',      'Nabeul',      2),
('TN-22', 'Zaghouan',    'زغوان',     'Zaghouan',    2),
('TN-23', 'Bizerte',     'بنزرت',     'Bizerte',     2),
('TN-31', 'Béja',        'باجة',      'Beja',        2),
('TN-32', 'Jendouba',    'جندوبة',    'Jendouba',    2),
('TN-33', 'Le Kef',      'الكاف',     'Kef',         2),
('TN-34', 'Siliana',     'سليانة',    'Siliana',     2),
('TN-41', 'Kairouan',    'القيروان',  'Kairouan',    2),
('TN-42', 'Kasserine',   'القصرين',   'Kasserine',   2),
('TN-43', 'Sidi Bouzid', 'سيدي بوزيد', 'Sidi Bouzid', 2),
('TN-51', 'Sousse',      'سوسة',      'Sousse',      2),
('TN-52', 'Monastir',    'المنستير',  'Monastir',    2),
('TN-53', 'Mahdia',      'المهدية',   'Mahdia',      2),
('TN-61', 'Sfax',        'صفاقس',     'Sfax',        2),
('TN-71', 'Gafsa',       'قفصة',      'Gafsa',       3),
('TN-72', 'Tozeur',      'توزر',      'Tozeur',      3),
('TN-73', 'Kébili',      'قبلي',      'Kebili',      3),
('TN-81', 'Gabès',       'قابس',      'Gabes',       3),
('TN-82', 'Médenine',    'مدنين',     'Medenine',    3),
('TN-83', 'Tataouine',   'تطاوين',    'Tataouine',   3);

-- Départements (niveau 1)
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order) VALUES
(NULL, 'high-tech',     'High-tech',          'إلكترونيات',          'Electronics',        1900, 1),
(NULL, 'electromenager','Électroménager',     'أجهزة منزلية',        'Home appliances',    1900, 2),
(NULL, 'mode',          'Mode',               'موضة',                'Fashion',            1900, 3),
(NULL, 'beaute',        'Beauté & santé',     'جمال وصحة',           'Beauty & health',    1900, 4),
(NULL, 'maison',        'Maison & cuisine',   'المنزل والمطبخ',      'Home & kitchen',     1900, 5),
(NULL, 'bebe',          'Bébé & enfants',     'الرضع والأطفال',      'Baby & kids',        1900, 6),
(NULL, 'sport',         'Sport & loisirs',    'رياضة وترفيه',        'Sports & outdoors',  1900, 7),
(NULL, 'epicerie',      'Épicerie',           'بقالة',               'Grocery',             700, 8),
(NULL, 'scolaire',      'Livres & scolaire',  'كتب وأدوات مدرسية',   'Books & school',      700, 9),
(NULL, 'artisanat',     'Artisanat tunisien', 'صناعات تقليدية',      'Tunisian crafts',    1900, 10);

-- Sous-catégories (niveau 2)
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'smartphones', 'Smartphones', 'هواتف ذكية', 'Smartphones', 1900, 1 FROM categories WHERE slug = 'high-tech';
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'ordinateurs', 'Ordinateurs portables', 'حواسيب محمولة', 'Laptops', 1900, 2 FROM categories WHERE slug = 'high-tech';
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'tv-audio', 'TV & audio', 'تلفزيون وصوت', 'TV & audio', 1900, 3 FROM categories WHERE slug = 'high-tech';
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'gros-electromenager', 'Gros électroménager', 'أجهزة كبيرة', 'Large appliances', 1900, 1 FROM categories WHERE slug = 'electromenager';
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'petit-electromenager', 'Petit électroménager', 'أجهزة صغيرة', 'Small appliances', 1900, 2 FROM categories WHERE slug = 'electromenager';
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'mode-homme', 'Homme', 'رجال', 'Men', 1900, 1 FROM categories WHERE slug = 'mode';
INSERT INTO categories (parent_id, slug, name_fr, name_ar, name_en, vat_bp, sort_order)
SELECT id, 'mode-femme', 'Femme', 'نساء', 'Women', 1900, 2 FROM categories WHERE slug = 'mode';
