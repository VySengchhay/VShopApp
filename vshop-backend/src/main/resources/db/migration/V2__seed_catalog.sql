-- Sample catalog so the Android app has data on first run.
-- Prices are small on purpose: good for sandbox testing.

INSERT INTO categories (name, icon, sort_order) VALUES
  ('Coffee & Tea',  'coffee',   1),
  ('Snacks',        'cookie',   2),
  ('Electronics',   'devices',  3),
  ('Fashion',       'checkroom',4),
  ('Home & Living', 'chair',    5);

INSERT INTO products (category_id, name, description, price, image_url, stock) VALUES
  (1, 'Mondulkiri Arabica Beans 250g', 'Medium roast beans grown in Mondulkiri. Notes of cocoa and brown sugar.', 6.50,  'https://picsum.photos/seed/pw-coffee1/600/600', 40),
  (1, 'Iced Latte Drip Bags (10 pcs)', 'Single-serve drip bags for a quick iced latte at home.',                 3.20,  'https://picsum.photos/seed/pw-coffee2/600/600', 60),
  (1, 'Kampot Pepper Green Tea',       'Loose green tea blended with a hint of Kampot pepper.',                    4.00,  'https://picsum.photos/seed/pw-tea1/600/600',    25),
  (2, 'Palm Sugar Cookies',            'Crunchy cookies sweetened with Cambodian palm sugar.',                     1.80,  'https://picsum.photos/seed/pw-snack1/600/600',  80),
  (2, 'Dried Mango Slices 200g',       'Naturally sweet dried mango, no added sugar.',                             2.50,  'https://picsum.photos/seed/pw-snack2/600/600',  70),
  (2, 'Roasted Cashews 250g',          'Salted roasted cashews from Kampong Thom.',                                3.90,  'https://picsum.photos/seed/pw-snack3/600/600',  50),
  (3, 'USB-C Fast Charger 20W',        'Compact wall charger with USB-C Power Delivery.',                          8.90,  'https://picsum.photos/seed/pw-elec1/600/600',   30),
  (3, 'Wireless Earbuds',              'Bluetooth 5.3 earbuds with charging case, 24h battery.',                  19.00,  'https://picsum.photos/seed/pw-elec2/600/600',   15),
  (3, 'Phone Stand (Aluminium)',       'Adjustable desk stand for phones and small tablets.',                      4.50,  'https://picsum.photos/seed/pw-elec3/600/600',   45),
  (4, 'Krama Scarf (Cotton)',          'Classic checked krama, hand-woven cotton.',                                5.00,  'https://picsum.photos/seed/pw-fashion1/600/600',35),
  (4, 'Canvas Tote Bag',               'Sturdy everyday tote with inner pocket.',                                  6.00,  'https://picsum.photos/seed/pw-fashion2/600/600',40),
  (5, 'Rattan Coaster Set (4 pcs)',    'Hand-woven rattan coasters.',                                              3.00,  'https://picsum.photos/seed/pw-home1/600/600',   55),
  (5, 'Scented Candle – Lemongrass',   'Soy wax candle, about 30 hours burn time.',                                7.50,  'https://picsum.photos/seed/pw-home2/600/600',   20),
  (5, 'Ceramic Mug 350ml',             'Glazed stoneware mug, dishwasher safe.',                                   4.80,  'https://picsum.photos/seed/pw-home3/600/600',   30);
