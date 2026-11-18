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
