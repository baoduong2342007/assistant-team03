export interface MenuItem {
  label: string;
  href?: string;
  badge?: string;
}

export interface MegaColumn {
  title: string;
  items: MenuItem[];
  hasViewAll?: boolean;
  viewAllHref?: string;
  viewAllText?: string;
}

export interface NavigationConfig {
  brand: {
    title: string;
    badge?: string;
  };
  recipesMegaMenu: {
    label: string;
    columns: MegaColumn[];
    bottomCta: {
      label: string;
      href: string;
    };
  };
  mealPlansDropdown: {
    label: string;
    items: MenuItem[];
  };
  myPantry: {
    label: string;
  };
  exploreMoreDropdown: {
    label: string;
    items: MenuItem[];
  };
  forBusiness: {
    label: string;
    badge?: string;
  };
  actions: {
    searchPlaceholder: string;
    searchPrompt: string;
    searchButton: string;
    cartTitle: string;
    cartEmpty: string;
    signUp: string;
    signIn: string;
  };
}

export const navigationData: NavigationConfig = {
  brand: {
    title: 'BẾP NHÀ MÌNH',
    badge: 'AI Dinh Dưỡng',
  },
  recipesMegaMenu: {
    label: 'Công Thức',
    columns: [
      {
        title: 'Phổ Biến',
        items: [
          { label: 'Món Ăn Gia Đình' },
          { label: 'Nhanh & Tiện Lợi' },
          { label: 'Nấu Một Nồi' },
          { label: 'Tốt Sức Khỏe' },
          { label: 'Rau Trộn & Salad' },
          { label: 'Nước Sốt & Gia Vị' },
        ],
        hasViewAll: false,
      },
      {
        title: 'Bữa Ăn',
        items: [
          { label: 'Bữa Sáng' },
          { label: 'Bữa Xế (Brunch)' },
          { label: 'Bữa Trưa' },
          { label: 'Bữa Tối' },
          { label: 'Món Tráng Miệng' },
          { label: 'Đồ Ăn Vặt' },
        ],
        hasViewAll: false,
      },
      {
        title: 'Chế Độ Ăn',
        items: [
          { label: 'Ăn Chay Thanh Đạm' },
          { label: 'Giảm Tinh Bột (Low-Carb)' },
          { label: 'Không Sữa Động Vật' },
          { label: 'Thuần Chay (Vegan)' },
          { label: 'Keto Năng Lượng' },
          { label: 'Không Chứa Gluten' },
          { label: 'Không Hải Sản Vỏ Cứng' },
        ],
        hasViewAll: false,
      },
      {
        title: 'Nguyên Liệu',
        items: [
          { label: 'Thịt Gà' },
          { label: 'Thịt Bò' },
          { label: 'Cơm & Các Loại Gạo' },
          { label: 'Đậu Phụ & Tempeh' },
          { label: 'Cá Hồi Tươi' },
          { label: 'Thịt Heo' },
          { label: 'Cá & Hải Sản' },
          { label: 'Khoai Tây & Các Loại Củ' },
        ],
        hasViewAll: true,
        viewAllHref: '#all-ingredients',
        viewAllText: 'XEM TẤT CẢ >',
      },
      {
        title: 'Loại Món',
        items: [
          { label: 'Món Ăn Kèm' },
          { label: 'Món Khai Vị' },
          { label: 'Mì Ý & Mì Sợi' },
          { label: 'Bánh Mì & Món Cuốn' },
          { label: 'Thức Uống & Sinh Tố' },
          { label: 'Canh & Món Hầm' },
          { label: 'Sốt Chấm & Phết' },
          { label: 'Bánh Mì Nướng' },
        ],
        hasViewAll: true,
        viewAllHref: '#all-dish-types',
        viewAllText: 'XEM TẤT CẢ >',
      },
      {
        title: 'Ẩm Thực',
        items: [
          { label: 'Món Ngon Ba Miền' },
          { label: 'Ẩm Thực Châu Á' },
          { label: 'Ẩm Thực Trung Đông' },
          { label: 'Ẩm Thực Hàn Quốc' },
          { label: 'Ẩm Thực Tây Ban Nha' },
          { label: 'Ẩm Thực Mỹ Latinh' },
          { label: 'Ẩm Thực Ý' },
          { label: 'Ẩm Thực Địa Trung Hải' },
        ],
        hasViewAll: true,
        viewAllHref: '#all-cuisines',
        viewAllText: 'XEM TẤT CẢ >',
      },
    ],
    bottomCta: {
      label: 'XEM TẤT CẢ CÔNG THỨC',
      href: '#all-recipes',
    },
  },
  mealPlansDropdown: {
    label: 'Thực Đơn',
    items: [
      { label: 'Cá Nhân Hóa Thực Đơn', href: '#personalize' },
      { label: 'Xem Thêm Thực Đơn', href: '#more-plans' },
    ],
  },
  myPantry: {
    label: 'Tủ Bếp',
  },
  exploreMoreDropdown: {
    label: 'Khám Phá Thêm',
    items: [
      { label: 'Đầu Bếp & Tác Giả', href: '#creators' },
      { label: 'Lớp Học Nấu Ăn', href: '#classes' },
      { label: 'Cẩm Nang Nguyên Liệu', href: '#guides' },
      { label: 'Bộ Sưu Tập Món Ngon', href: '#collections' },
      { label: 'Bài Viết & Mẹo Bếp', href: '#articles' },
      { label: 'Thực Đơn Tiệc & Lễ Hội', href: '#festive' },
    ],
  },
  forBusiness: {
    label: 'Dành Cho Doanh Nghiệp',
    badge: 'MỚI',
  },
  actions: {
    searchPlaceholder: 'Tìm kiếm công thức, nguyên liệu (ví dụ: Thịt gà, Cá hồi, Canh chua)...',
    searchPrompt: 'Gợi ý:',
    searchButton: 'Tìm',
    cartTitle: 'Giỏ Nguyên Liệu',
    cartEmpty: 'Chưa có nguyên liệu nào',
    signUp: 'ĐĂNG KÝ',
    signIn: 'ĐĂNG NHẬP',
  },
};
