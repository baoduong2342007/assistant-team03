# Nguồn ảnh minh họa Android

Ảnh `app/src/main/res/drawable-nodpi/family_meal.webp` được tạo bằng công cụ imagegen tích hợp trong giai đoạn prototype và được giữ lại trong dự án Android. Ảnh mang tính minh họa.

Prompt ảnh: “Use case: photorealistic-natural. Asset type: main food photograph for a Vietnamese family meal planner web application. Primary request: authentic home cooked Vietnamese dinner, warmly appetizing editorial food photography. Scene: rustic light warm oak table, round rattan woven tray, off-white ceramic plates and bowls. Subject: a dark brown clay pot of glossy caramelized pork belly and boiled eggs (thit kho trung) prominent in lower center, bowl of light green mustard vegetable soup with minced pork upper left, plate of sauteed morning glory with garlic upper right, small bowl white rice and wooden chopsticks at right. Composition: overhead slightly angled close-up, wide landscape 3:2, food fills frame, artfully casual genuinely Vietnamese family home cooking, subtle linen napkin. Soft natural daylight, cream, leaf green and caramel tones, premium cookbook quality, realistic textures. No lettering, no logos, no people, no watermarks.”

## Ảnh món ăn cho bản demo

ImageGen tích hợp tạo hai lưới ảnh theo công thức có trong danh mục. Mỗi ô được xuất thành một WebP 504 × 504 px tại `app/src/main/res/drawable-nodpi/recipe_<id>.webp`. `RecipePhotos.kt` nối ảnh với ID công thức. Ảnh minh họa hình dáng món, còn nguyên liệu và định lượng lấy từ công thức.

| Bộ ảnh | ID món theo thứ tự từ trái sang phải, từ trên xuống |
| --- | --- |
| Bữa sáng | `bread`, `porridge`, `noodles`, `phoga`, `xoi`, `friedrice` |
| Bữa trưa và tối | `chicken`, `tofu`, `squash`, `cabbage`, `broccoli`, `cucumber` |

### Prompt bữa sáng

```text
Use case: photorealistic-natural.
Asset type: one landscape 3:2 photographic sprite sheet consumed by a Vietnamese home cooking Android app. Make an orderly 3-column by 2-row grid with exactly six equal square cells, full-bleed. Cell boundaries are straight at 1/3 and 2/3 of the image width and halfway down the image height. Each cell is an independent softly lit food photo, shot from about 55 degrees above, centered on the dish on its own beige warm-cream or light oak tabletop. Keep all recognizable food and most of its white ceramic plate or bowl in the middle 70% of its own cell. No object crosses a cell boundary. No borders, no margins, no labels, lettering, icons, decorative elements, people, steam overlays, logos or watermarks. Warm editorial cookbook photography, appetizing yet realistic textures and modest home-cooked portions. Distinct and authentic dishes in the following exact reading order.
Top-left: Bánh mì ốp la. Short crisp Vietnamese baguette split open, filled with a sunny-side-up egg with visible yolk and cucumber slices, on an ivory plate.
Top-middle: Cháo thịt bằm. Pale, softly thick rice congee in a ceramic bowl, small bits of ground pork, fine ginger and scallions on top, plain broth color.
Top-right: Mì trứng rau cải. Yellow curly egg noodles with bright cooked mustard greens, scallions and a softly cooked egg in a light soup, in a ceramic bowl.
Bottom-left: Phở gà. Clear golden chicken stock with flat wide white rice noodles, hand-pulled sliced poached chicken and green scallions in a deep cream bowl.
Bottom-middle: Xôi đậu xanh. Compact slightly domed mound of yellow mung bean sticky rice with visible sticky grains, topped with a few crispy golden fried shallots, on a white plate.
Bottom-right: Cơm chiên trứng. Golden loose fried rice grains with little bits of scrambled egg, cubed carrots and chopped scallions, on a white plate.
```

### Prompt bữa trưa và tối

```text
Use case: photorealistic-natural.
Asset type: one landscape 3:2 photographic sprite sheet consumed by a Vietnamese home cooking Android app. Make an orderly 3-column by 2-row grid with exactly six equal square cells, full-bleed. Cell boundaries are straight at 1/3 and 2/3 of the image width and halfway down the image height. Each cell is an independent softly lit food photo, shot from about 55 degrees above, centered on the dish on its own beige warm-cream or light oak tabletop. Keep all recognizable food and most of its white ceramic plate or bowl in the middle 70% of its own cell. No object crosses a cell boundary. No borders, no margins, no labels, lettering, icons, decorative elements, people, steam overlays, logos or watermarks. Warm editorial cookbook photography, appetizing yet realistic textures and modest home-cooked portions. Distinct and authentic dishes in the following exact reading order.
Top-left: Gà kho gừng. Vietnamese ginger braised chicken pieces in a glazed caramel-colored sauce with thin golden ginger strips in a shallow cream bowl.
Top-middle: Đậu hũ sốt cà chua. Golden fried tofu cubes braised in visibly chunky red fresh tomato sauce and dotted with green scallions, in a white shallow dish.
Top-right: Canh bí đỏ thịt bằm. Clear light orange soup with large distinct pumpkin cubes, fine crumbles of minced pork and scallions, in a ceramic soup bowl. The broth is clear, not puréed.
Bottom-left: Canh bắp cải cà rốt. Pale clear Vietnamese home soup with cooked pale-green cabbage leaves, thin orange carrot rounds and scallions, in a ceramic bowl.
Bottom-middle: Bông cải xào cà rốt. Green broccoli florets and sliced carrots lightly stir-fried with sliced garlic, in a light flat ceramic dish.
Bottom-right: Dưa leo trộn. Fresh cucumber half-moons with pale-green centers and dark-green skin, thin carrot strips and a very light lime dressing, on a shallow white plate.
```

## Bổ sung ảnh đủ danh mục

ImageGen tích hợp tạo 51 ảnh minh họa theo công thức. Các ô được xuất thành WebP 500 × 500 px, lưu cùng 12 ảnh riêng của đợt trước và ảnh mâm cơm dùng cho ba món. Toàn bộ 66 món có ảnh được đóng gói trong APK.

Các ảnh Wikimedia Commons sau được kiểm tra khi tìm nguồn cho ba món canh. Máy tải về nhận HTTP 403 hoặc 429, nên APK dùng ảnh ImageGen cho ba món này.

- [Canh chua cá diêu hồng của Phương Huy](https://commons.wikimedia.org/wiki/File:Canh_chua_c%C3%A1_di%C3%AAu_h%E1%BB%93ng.jpg), phạm vi công cộng.
- [Canh khổ qua nhồi thịt của Ngô Trung](https://commons.wikimedia.org/wiki/File:Canh_kh%E1%BB%95_qua.JPG), CC BY-SA 3.0.
- [Canh cà chua trứng của Nguyen Thi Quynh Tram](https://commons.wikimedia.org/wiki/File:Canh_ca_chua_trung.png), CC BY 3.0.

### Lưới 1

ID theo thứ tự: `fish`, `shrimp`, `eggs`, `boiled`, `steamed_egg`, `lean_pork`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: pepper-braised basa catfish steak with caramel glaze, finely cracked black pepper, shallot.
Top center: glossy sweet-savory caramelized pink shrimp with pan-reduced sauce and fried garlic.
Top right: golden Vietnamese scallion omelet sliced in wedges.
Bottom left: boiled mixed vegetables: whole green okra pods, carrots and pale cabbage.
Bottom center: silky steamed egg custard with crumbled minced pork and sliced green scallions.
Bottom right: bite-size lean pork cubes braised in caramel fish sauce with cracked black pepper.
```

### Lưới 2

ID theo thứ tự: `ribs`, `beef_tomato`, `boiled_chicken`, `chicken_mushroom`, `mackerel`, `snakehead`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: Vietnamese sweet-sour pork spare rib segments glazed with red tomatoes and garlic.
Top center: thin stir-fried beef slices and soft red tomato wedges with scallions.
Top right: chopped boiled Vietnamese chicken with yellow skin and pale tender meat.
Bottom left: stir-fried chicken pieces and straw mushrooms with soy glaze.
Bottom center: thick cross-cut mackerel steaks in chunky red tomato sauce.
Bottom right: Vietnamese cá lóc kho tộ, cross-cut freshwater snakehead fish steaks in dark caramel sauce in a clay pot.
```

### Lưới 3

ID theo thứ tự: `steamed_fish`, `steamed_shrimp`, `stuffed_tofu`, `tofu_mushroom`, `egg_tomato`, `boiled_pork`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: whole red tilapia steamed with golden ginger matchsticks and green scallions.
Top center: steamed whole pink shrimp with thin pale ginger strips.
Top right: fried rectangular tofu pockets visibly stuffed with ground pork, chunky tomato sauce.
Bottom left: tofu cubes and round straw mushrooms braised in dark soy sauce.
Bottom center: scrambled egg in distinct yellow curds with soft red tomato wedges and green scallion.
Bottom right: thin slices of boiled lean pork with a narrow white edge, neatly fanned on ceramic plate.
```

### Lưới 4

ID theo thứ tự: `beef_onion`, `mushroom_braise`, `beef_stew`, `eggplant_tofu`, `winter_melon`, `malabar`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: stir-fried beef slices and translucent white onion petals.
Top center: whole and halved gray-brown straw mushrooms braised in black pepper soy sauce.
Top right: home-cooked beef stew with tender beef chunks, carrots and potatoes.
Bottom left: long purple-skinned eggplant chunks and golden tofu in gentle tomato braise.
Bottom center: clear winter melon soup with pale green translucent cubes and pink shrimp.
Bottom right: clear home soup with deep green glossy Malabar spinach and crumbled minced pork.
```

### Lưới 5

ID theo thứ tự: `luffa`, `katuk`, `amaranth`, `gourd`, `napa_tofu`, `rib_potato`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: clear soup with grooves on pale green luffa chunks, pink shrimp and scallions.
Top center: clear soup with small dark green oval katuk leaves and minced pork.
Top right: deep burgundy amaranth-leaf soup with pink shrimp, distinctly red-violet broth.
Bottom left: clear soup with thin smooth pale green bottle-gourd slices and ground pork.
Bottom center: clear napa cabbage soup with tofu cubes, carrot slices and scallions.
Bottom right: clear pork rib soup with potato chunks, carrots and scallions.
```

### Lưới 6

ID theo thứ tự: `chayote_soup`, `mushroom_soup`, `fish_tomato_soup`, `radish_soup`, `choy_sum`, `bok_choy`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: clear soup with pale green chayote wedges and carrot rounds.
Top center: broth with halved straw mushrooms, white tofu squares, tomatoes and scallions.
Top right: Vietnamese clear hot soup with snakehead fish steaks, red tomatoes, green scallion and ginger.
Bottom left: clear soup with white daikon cubes, minced pork and scallions.
Bottom center: glossy stir-fried choy sum stems and leaves with thin garlic slices.
Bottom right: short stalks of tender baby bok choy stir-fried with halved straw mushrooms and garlic.
```

### Lưới 7

ID theo thứ tự: `pumpkin_stirfry`, `chayote`, `bean_sprout`, `green_beans`, `cabbage_tomato`, `sweet_potato_leaves`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: sautéed yellow-orange slices of pumpkin and sliced garlic, no broth.
Top center: shredded pale-green chayote and bright orange carrot stir-fried with garlic.
Top right: white mung bean sprouts stir-fried with golden tofu strips and scallion.
Bottom left: short cuts of green beans stir-fried with sliced garlic.
Bottom center: stir-fried broad white cabbage strips and soft red tomato wedges.
Bottom right: a plate of boiled deep-green heart-shaped sweet potato leaves and tender stems.
```

### Lưới 8

ID theo thứ tự: `gourd_egg`, `luffa_stirfry`, `kohlrabi`, `steamed_broccoli`, `bitter_melon_egg`, `boiled_spinach`.

```text
Use case: photorealistic-natural. Android recipe photo asset sheet. Generate a 1536 x 1024 landscape image with exactly six equally sized full-bleed square cells in a 3-column by 2-row grid. Hard straight boundaries at x=512 / 1024, y=512. Each cell is a different individual authentic Vietnamese home cooked dish, photographed independently on warm light oak with white ceramic, natural diffuse window daylight, framed from about 55 degrees above, centered within its own square. The food is identifiable and occupies most of each square; no overlap, no labels, no letters, no hands, no watermarks, no side dishes. In precise reading order:
Top left: stir-fried thin pale bottle gourd half moons mixed with small morsels of scrambled egg.
Top center: stir-fried cut sections of soft pale ridged luffa and garlic.
Top right: stir-fried pale kohlrabi batons with carrot matchsticks and garlic.
Bottom left: plain steamed green broccoli and white cauliflower florets.
Bottom center: ridged green bitter melon half-moons stir-fried with scrambled egg.
Bottom right: a plate of boiled Vietnamese water spinach leaves with long hollow stems and a lime wedge.
```

### Lưới ba món canh

Ô hàng trên, từ trái qua phải: `sour`, `bitter_melon`, `egg_soup`.

```text
Use case photorealistic-natural. A 1536 x 1024 photographic asset sheet with 6 equal, square, full-bleed cells in a precise 3-column by 2-row grid. Hard straight boundaries at x=512 and 1024, y=512. Independent authentic Vietnamese food photos in simple white ceramic on light oak, natural window light, camera about 55 degrees above, soup centered within its square with margin. Left column both rows: canh chua cá basa, clear golden Vietnamese sweet-sour soup with thick white basa fish steaks, sliced okra, red tomato, golden pineapple chunks and herbs. Middle column both rows: canh khổ qua nhồi thịt, clear broth with thick bright green ridged bitter melon cylinders, visibly filled with cooked ground pork and chopped scallion. Right column both rows: canh trứng cà chua, light clear tomato broth with white and yellow ribbons of egg, bright red tomato wedges and green scallion. Top row is the primary composition; bottom row offers a second independent framing for the same dish. No labels, lettering, hands, cutlery or logo.
```


### Hình chào trợ lý — 24/11/2026

Tệp: `app/src/main/res/drawable-nodpi/assistant_hero.webp`. Minh họa trang trí do công cụ `image_gen` tích hợp tạo; không phải ảnh món ăn thực tế. Xuất WebP chất lượng 90, đóng gói trong APK, không tải từ mạng khi mở chat.

Prompt cuối:

```text
Use case: stylized-concept. Asset type: square hero illustration for a premium Vietnamese cooking assistant mobile app, NOT a screenshot or mockup. Primary request: an elegant flowing translucent silk-like ribbon of emerald green and warm gold light floating above a small ivory ceramic bowl of fresh Vietnamese ingredients, surrounded by a few floating cherry tomatoes, leafy herbs, a carrot and garlic. Sculptural luminous mesh ribbon with graceful S curve, refined appetizing 3D food illustration. Centered composition, full object visible with generous padding on all sides, subtle soft shadow, soft warm ivory background exactly #FFFCF7 blending at edges. Warm golden highlights, forest green accents, tasteful magical cooking atmosphere. No text, no lettering, no UI, no logos, no border. Save generated image as a file if possible.
```
