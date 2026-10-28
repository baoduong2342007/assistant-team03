import React, { useState, useRef, useEffect } from 'react';
import {
  Search,
  ShoppingCart,
  ChevronDown,
  Menu,
  X,
  BookOpen,
  ExternalLink,
  Info,
  ChefHat,
} from 'lucide-react';
import { navigationData, MegaColumn } from '../data/navigationData';

interface NavbarProps {
  currentView?: string;
  onNavigate?: (view: any) => void;
  onOpenIntro?: () => void;
  onOpenAbout?: () => void;
}

type DropdownType = 'recipes' | 'mealPlans' | 'exploreMore' | null;

export const Navbar: React.FC<NavbarProps> = ({
  currentView = 'landing',
  onNavigate,
  onOpenIntro,
  onOpenAbout,
}) => {
  // State for desktop dropdowns
  const [activeDropdown, setActiveDropdown] = useState<DropdownType>(null);
  const [isScrolled, setIsScrolled] = useState(false);
  const [isVisible, setIsVisible] = useState(true);
  const lastScrollYRef = useRef(0);

  // State for mobile drawer and mobile accordions
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [mobileExpandedSection, setMobileExpandedSection] = useState<string | null>(null);

  // State for interactive search bar modal/popover
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Cart item count state
  const [cartCount, setCartCount] = useState(2);
  const [isCartOpen, setIsCartOpen] = useState(false);

  // Refs for accessibility and hover intent
  const navRef = useRef<HTMLElement>(null);
  const closeTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const triggerRefs = {
    recipes: useRef<HTMLButtonElement>(null),
    mealPlans: useRef<HTMLButtonElement>(null),
    exploreMore: useRef<HTMLButtonElement>(null),
  };

  // Scroll detection: ẩn thanh điều hướng khi lướt xuống, hiện lại khi lướt lên hoặc về đầu trang
  useEffect(() => {
    const handleScroll = () => {
      if (isMobileMenuOpen) return;

      const currentScrollY = Math.max(0, window.scrollY);
      setIsScrolled(currentScrollY > 10);

      // Nếu đang ở gần đầu trang (<= 40px), luôn luôn hiển thị
      if (currentScrollY <= 40) {
        setIsVisible(true);
      } else if (currentScrollY > lastScrollYRef.current + 8) {
        // Đang lướt xuống (xuống cuối trang): ẩn thanh điều hướng lên trên
        setIsVisible(false);
        setActiveDropdown(null);
        setIsSearchOpen(false);
        setIsCartOpen(false);
      } else if (currentScrollY < lastScrollYRef.current - 8) {
        // Đang lướt ngược lên (về lại đầu trang): cho thanh điều hướng hiện lại
        setIsVisible(true);
      }

      lastScrollYRef.current = currentScrollY;
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isMobileMenuOpen]);

  // Accessibility: Escape key handler and outside click detection
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (activeDropdown) {
          const trigger = triggerRefs[activeDropdown]?.current;
          setActiveDropdown(null);
          trigger?.focus();
        }
        setIsMobileMenuOpen(false);
        setIsSearchOpen(false);
        setIsCartOpen(false);
      }
    };

    const handleClickOutside = (e: MouseEvent) => {
      if (navRef.current && !navRef.current.contains(e.target as Node)) {
        setActiveDropdown(null);
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [activeDropdown]);

  // Lock body scroll when mobile menu is open
  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isMobileMenuOpen]);

  // Hover Intent with 120ms buffer for mouse movement between trigger and panel
  const handleMouseEnter = (menu: DropdownType) => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
      closeTimeoutRef.current = null;
    }
    setActiveDropdown(menu);
  };

  const handleMouseLeave = () => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
    }
    closeTimeoutRef.current = setTimeout(() => {
      setActiveDropdown(null);
    }, 120);
  };

  const handleDropdownToggle = (menu: DropdownType) => {
    if (activeDropdown === menu) {
      setActiveDropdown(null);
    } else {
      if (closeTimeoutRef.current) clearTimeout(closeTimeoutRef.current);
      setActiveDropdown(menu);
    }
  };

  const handleBrandClick = (e: React.MouseEvent) => {
    e.preventDefault();
    setActiveDropdown(null);
    setIsMobileMenuOpen(false);
    if (onNavigate) {
      onNavigate('landing');
    }
  };

  const handleSignUpClick = () => {
    setActiveDropdown(null);
    setIsMobileMenuOpen(false);
    if (onNavigate) {
      onNavigate('onboarding');
    }
  };

  const handleSignInClick = () => {
    setActiveDropdown(null);
    setIsMobileMenuOpen(false);
    if (onNavigate) {
      onNavigate('chat');
    }
  };

  const toggleMobileSection = (sectionId: string) => {
    setMobileExpandedSection(prev => (prev === sectionId ? null : sectionId));
  };

  return (
    <>
      <header
        ref={navRef}
        className={`sticky top-0 z-50 w-full bg-white transform transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
          isVisible ? 'translate-y-0 opacity-100' : '-translate-y-full opacity-0 pointer-events-none'
        } ${
          isScrolled ? 'shadow-sm border-b border-gray-200' : 'border-b border-gray-100'
        }`}
      >
        {/* Main Navbar Bar */}
        <div className="max-w-7xl mx-auto h-[72px] px-3 sm:px-6 lg:px-8 flex items-center justify-between gap-2 sm:gap-4">
          
          {/* =========================================
              1. BÊN TRÁI: LOGO THƯƠNG HIỆU
             ========================================= */}
          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            {/* Nút Hamburger menu trên thiết bị di động */}
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen(true)}
              className="lg:hidden p-2 -ml-1 text-stone-700 hover:text-stone-900 hover:bg-stone-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#014043] transition-colors shrink-0"
              aria-label="Mở menu điều hướng"
            >
              <Menu className="w-6 h-6" />
            </button>

            {/* Logo Bếp Nhà Mình */}
            <a
              href="/"
              onClick={handleBrandClick}
              className="flex items-center gap-2 sm:gap-2.5 text-left group focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#014043] rounded-lg py-1 px-1 transition-transform duration-300 hover:scale-[1.02] shrink-0"
              aria-label="Bếp Nhà Mình - Trang chủ"
            >
              <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-amber-600 via-orange-500 to-amber-500 text-white flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform duration-300 shrink-0">
                <span className="text-lg">🍲</span>
              </div>
              <div className="flex flex-col shrink-0">
                <span className="font-extrabold text-[#1f2937] tracking-tight text-base sm:text-lg xl:text-xl uppercase group-hover:text-[#014043] transition-colors duration-300 leading-tight whitespace-nowrap">
                  {navigationData.brand.title}
                </span>
                {navigationData.brand.badge && (
                  <span className="text-[10px] font-semibold text-amber-700 tracking-wider uppercase whitespace-nowrap leading-none mt-0.5">
                    {navigationData.brand.badge}
                  </span>
                )}
              </div>
            </a>
          </div>

          {/* =========================================
              2. Ở GIỮA: CÁC MỤC ĐIỀU HƯỚNG DESKTOP
             ========================================= */}
          <nav
            className="hidden lg:flex items-center gap-3.5 xl:gap-6 2xl:gap-8 shrink-0"
            aria-label="Điều hướng chính"
          >
            {/* Mục 1: Công Thức (Kích hoạt Mega Menu) */}
            <div
              className="relative shrink-0"
              onMouseEnter={() => handleMouseEnter('recipes')}
              onMouseLeave={handleMouseLeave}
            >
              <button
                ref={triggerRefs.recipes}
                type="button"
                onClick={() => handleDropdownToggle('recipes')}
                aria-expanded={activeDropdown === 'recipes'}
                aria-haspopup="menu"
                className={`flex items-center gap-1 xl:gap-1.5 py-4 text-[13.5px] xl:text-[15px] font-semibold whitespace-nowrap transition-all duration-500 ease-out cursor-pointer hover:-translate-y-0.5 shrink-0 ${
                  activeDropdown === 'recipes'
                    ? 'text-[#014043]'
                    : 'text-[#1f2937] hover:text-[#014043]'
                }`}
              >
                <span className="whitespace-nowrap">{navigationData.recipesMegaMenu.label}</span>
                <ChevronDown
                  className={`w-4 h-4 shrink-0 transition-transform duration-500 ease-out ${
                    activeDropdown === 'recipes' ? 'transform rotate-180 text-[#014043]' : 'text-stone-400'
                  }`}
                />
              </button>
            </div>

            {/* Mục 2: Thực Đơn (Dropdown chuẩn) */}
            <div
              className="relative shrink-0"
              onMouseEnter={() => handleMouseEnter('mealPlans')}
              onMouseLeave={handleMouseLeave}
            >
              <button
                ref={triggerRefs.mealPlans}
                type="button"
                onClick={() => handleDropdownToggle('mealPlans')}
                aria-expanded={activeDropdown === 'mealPlans'}
                aria-haspopup="menu"
                className={`flex items-center gap-1 xl:gap-1.5 py-4 text-[13.5px] xl:text-[15px] font-semibold whitespace-nowrap transition-all duration-500 ease-out cursor-pointer hover:-translate-y-0.5 shrink-0 ${
                  activeDropdown === 'mealPlans'
                    ? 'text-[#014043]'
                    : 'text-[#1f2937] hover:text-[#014043]'
                }`}
              >
                <span className="whitespace-nowrap">{navigationData.mealPlansDropdown.label}</span>
                <ChevronDown
                  className={`w-4 h-4 shrink-0 transition-transform duration-500 ease-out ${
                    activeDropdown === 'mealPlans' ? 'transform rotate-180 text-[#014043]' : 'text-stone-400'
                  }`}
                />
              </button>

              {/* Dropdown Thực Đơn: Hiệu ứng dropdown & pull up trong 0.5s */}
              <div
                role="menu"
                aria-label="Menu Thực Đơn"
                className={`absolute top-full left-0 mt-1 w-60 bg-white rounded-xl shadow-xl border border-stone-100 py-2.5 z-50 transform transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                  activeDropdown === 'mealPlans'
                    ? 'opacity-100 translate-y-0 pointer-events-auto visible'
                    : 'opacity-0 -translate-y-3 pointer-events-none invisible'
                }`}
              >
                {navigationData.mealPlansDropdown.items.map((item, idx) => (
                  <button
                    key={idx}
                    role="menuitem"
                    type="button"
                    onClick={() => {
                      setActiveDropdown(null);
                      if (!onNavigate) return;
                      if (item.href === '#personalize' || item.href === '/meal-planner') {
                        onNavigate('meal-planner');
                      } else if (item.href === '#more-plans' || item.href === '/meal-planner/app') {
                        onNavigate('meal-planner-app');
                      } else {
                        onNavigate('meal-planner');
                      }
                    }}
                    className="w-full text-left px-5 py-2.5 text-sm font-semibold text-[#1f2937] hover:text-[#014043] hover:bg-stone-50 cursor-pointer block transition-colors duration-200 whitespace-nowrap"
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Mục 3: Tủ Bếp (Liên kết trực tiếp) */}
            <button
              type="button"
              onClick={() => {
                setActiveDropdown(null);
                if (onNavigate) onNavigate('chat');
              }}
              className="py-4 text-[13.5px] xl:text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-all duration-500 ease-out hover:-translate-y-0.5 cursor-pointer whitespace-nowrap shrink-0"
            >
              {navigationData.myPantry.label}
            </button>

            {/* Mục 4: Khám Phá Thêm (Dropdown chuẩn) */}
            <div
              className="relative shrink-0"
              onMouseEnter={() => handleMouseEnter('exploreMore')}
              onMouseLeave={handleMouseLeave}
            >
              <button
                ref={triggerRefs.exploreMore}
                type="button"
                onClick={() => handleDropdownToggle('exploreMore')}
                aria-expanded={activeDropdown === 'exploreMore'}
                aria-haspopup="menu"
                className={`flex items-center gap-1 xl:gap-1.5 py-4 text-[13.5px] xl:text-[15px] font-semibold whitespace-nowrap transition-all duration-500 ease-out cursor-pointer hover:-translate-y-0.5 shrink-0 ${
                  activeDropdown === 'exploreMore'
                    ? 'text-[#014043]'
                    : 'text-[#1f2937] hover:text-[#014043]'
                }`}
              >
                <span className="whitespace-nowrap">{navigationData.exploreMoreDropdown.label}</span>
                <ChevronDown
                  className={`w-4 h-4 shrink-0 transition-transform duration-500 ease-out ${
                    activeDropdown === 'exploreMore' ? 'transform rotate-180 text-[#014043]' : 'text-stone-400'
                  }`}
                />
              </button>

              {/* Dropdown Khám Phá Thêm: Hiệu ứng dropdown & pull up trong 0.5s */}
              <div
                role="menu"
                aria-label="Menu Khám Phá Thêm"
                className={`absolute top-full left-0 mt-1 w-64 bg-white rounded-xl shadow-xl border border-stone-100 py-2.5 z-50 transform transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                  activeDropdown === 'exploreMore'
                    ? 'opacity-100 translate-y-0 pointer-events-auto visible'
                    : 'opacity-0 -translate-y-3 pointer-events-none invisible'
                }`}
              >
                {navigationData.exploreMoreDropdown.items.map((item, idx) => (
                  <button
                    key={idx}
                    role="menuitem"
                    type="button"
                    onClick={() => setActiveDropdown(null)}
                    className="w-full text-left px-5 py-2.5 text-sm font-semibold text-[#1f2937] hover:text-[#014043] hover:bg-stone-50 cursor-pointer block transition-colors duration-200 whitespace-nowrap"
                  >
                    {item.label}
                  </button>
                ))}

                {/* Phân cách liên kết ứng dụng */}
                <div className="my-2 border-t border-stone-100" />
                
                {onOpenIntro && (
                  <button
                    role="menuitem"
                    type="button"
                    onClick={() => {
                      setActiveDropdown(null);
                      onOpenIntro();
                    }}
                    className="w-full text-left px-5 py-2 text-sm font-medium text-stone-600 hover:text-stone-900 hover:bg-stone-50 flex items-center gap-2 transition-colors duration-200 whitespace-nowrap"
                  >
                    <Info className="w-4 h-4 text-amber-600 shrink-0" />
                    <span>Giới Thiệu Trợ Lý AI</span>
                  </button>
                )}

                {onOpenAbout && (
                  <button
                    role="menuitem"
                    type="button"
                    onClick={() => {
                      setActiveDropdown(null);
                      onOpenAbout();
                    }}
                    className="w-full text-left px-5 py-2 text-sm font-medium text-stone-600 hover:text-stone-900 hover:bg-stone-50 flex items-center gap-2 transition-colors duration-200 whitespace-nowrap"
                  >
                    <ChefHat className="w-4 h-4 text-orange-600 shrink-0" />
                    <span>Về Chúng Tôi</span>
                  </button>
                )}
              </div>
            </div>

            {/* Mục 5: Dành Cho Doanh Nghiệp */}
            <a
              href="#for-business"
              onClick={(e) => {
                e.preventDefault();
                setActiveDropdown(null);
              }}
              className="flex items-center gap-1 xl:gap-1.5 py-4 text-[13.5px] xl:text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-all duration-500 ease-out hover:-translate-y-0.5 group cursor-pointer whitespace-nowrap shrink-0"
            >
              <span className="whitespace-nowrap">{navigationData.forBusiness.label}</span>
              <span className="inline-flex items-center text-xs shrink-0">
                <ExternalLink className="w-3.5 h-3.5 text-stone-500 group-hover:text-[#014043] transition-colors" />
              </span>
              {navigationData.forBusiness.badge && (
                <span className="text-[10px] uppercase font-bold px-1.5 py-0.5 rounded-full bg-orange-100 text-orange-700 animate-pulse whitespace-nowrap shrink-0">
                  {navigationData.forBusiness.badge}
                </span>
              )}
            </a>
          </nav>

          {/* =========================================
              3. BÊN PHẢI: HÀNH ĐỘNG NGƯỜI DÙNG
             ========================================= */}
          <div className="flex items-center gap-2 sm:gap-2.5 xl:gap-3.5 shrink-0">
            {/* Nút Tìm kiếm */}
            <button
              type="button"
              onClick={() => setIsSearchOpen(prev => !prev)}
              aria-label="Tìm kiếm công thức và nguyên liệu"
              className="w-9 h-9 xl:w-10 xl:h-10 rounded-full border border-stone-200 hover:border-stone-800 flex items-center justify-center text-stone-700 hover:text-stone-950 transition-all duration-300 hover:scale-105 focus:outline-none focus:ring-2 focus:ring-[#014043] shrink-0"
            >
              <Search className="w-4 h-4" />
            </button>

            {/* Nút Giỏ Nguyên Liệu */}
            <button
              type="button"
              onClick={() => setIsCartOpen(prev => !prev)}
              aria-label={`Giỏ nguyên liệu có ${cartCount} món`}
              className="relative w-9 h-9 xl:w-10 xl:h-10 rounded-full border border-stone-200 hover:border-stone-800 flex items-center justify-center text-stone-700 hover:text-stone-950 transition-all duration-300 hover:scale-105 focus:outline-none focus:ring-2 focus:ring-[#014043] shrink-0"
            >
              <ShoppingCart className="w-4 h-4" />
              {cartCount > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-[#014043] text-white text-[10px] font-bold flex items-center justify-center border-2 border-white">
                  {cartCount}
                </span>
              )}
            </button>

            {/* Nút ĐĂNG KÝ */}
            <button
              type="button"
              onClick={handleSignUpClick}
              className="px-4 sm:px-5 xl:px-6 py-2 sm:py-2.5 rounded-full bg-[#014043] hover:bg-[#002f32] active:scale-95 text-white font-bold text-xs sm:text-[13px] tracking-wider uppercase shadow-sm hover:shadow transition-all duration-300 cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#014043] whitespace-nowrap shrink-0"
            >
              {navigationData.actions.signUp}
            </button>

            {/* Nút ĐĂNG NHẬP */}
            <button
              type="button"
              onClick={handleSignInClick}
              className="hidden sm:inline-flex px-4 sm:px-5 xl:px-6 py-2 sm:py-2.5 rounded-full border border-stone-300 hover:border-stone-800 hover:bg-stone-50 active:scale-95 text-[#1f2937] font-bold text-xs sm:text-[13px] tracking-wider uppercase transition-all duration-300 cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-stone-400 whitespace-nowrap shrink-0"
            >
              {navigationData.actions.signIn}
            </button>
          </div>
        </div>

        {/* =========================================
            4. MEGA MENU "CÔNG THỨC" (FULL WIDTH)
            Hiệu ứng dropdown & pull up trong 0.5s
           ========================================= */}
        <div
          role="menu"
          aria-label="Menu Công Thức"
          onMouseEnter={() => handleMouseEnter('recipes')}
          onMouseLeave={handleMouseLeave}
          className={`absolute top-full left-0 right-0 w-full bg-white border-t border-gray-100 shadow-2xl z-40 transform transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
            activeDropdown === 'recipes'
              ? 'opacity-100 translate-y-0 pointer-events-auto visible'
              : 'opacity-0 -translate-y-4 pointer-events-none invisible'
          }`}
        >
          <div className="max-w-7xl mx-auto px-6 sm:px-8 lg:px-10 pt-8 pb-6">
            
            {/* Lưới 6 cột */}
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-y-8 gap-x-6 lg:gap-x-8">
              {navigationData.recipesMegaMenu.columns.map((column: MegaColumn, colIdx: number) => (
                <div key={colIdx} className="flex flex-col">
                  {/* Tiêu đề cột */}
                  <h3 className="font-bold text-[#1f2937] text-base mb-3.5 tracking-tight">
                    {column.title}
                  </h3>

                  {/* Danh sách món ăn/chủ đề */}
                  <ul className="space-y-2.5" role="none">
                    {column.items.map((item, itemIdx) => (
                      <li key={itemIdx} role="none">
                        <button
                          role="menuitem"
                          type="button"
                          onClick={() => {
                            setActiveDropdown(null);
                            if (onNavigate) onNavigate('chat');
                          }}
                          className="text-[14px] text-stone-500 hover:text-stone-950 font-normal transition-colors duration-200 text-left block w-full truncate"
                        >
                          {item.label}
                        </button>
                      </li>
                    ))}

                    {/* Liên kết XEM TẤT CẢ > */}
                    {column.hasViewAll && (
                      <li role="none" className="pt-1.5">
                        <button
                          role="menuitem"
                          type="button"
                          onClick={() => {
                            setActiveDropdown(null);
                            if (onNavigate) onNavigate('chat');
                          }}
                          className="text-xs font-bold text-[#1f2937] hover:text-[#014043] tracking-wider uppercase inline-flex items-center gap-1 group transition-colors duration-200"
                        >
                          <span>{column.viewAllText || 'XEM TẤT CẢ >'}</span>
                          <span className="text-[10px] group-hover:translate-x-0.5 transition-transform duration-200">
                            &gt;
                          </span>
                        </button>
                      </li>
                    )}
                  </ul>
                </div>
              ))}
            </div>

            {/* Thanh dưới cùng & Nút kêu gọi hành động */}
            <div className="mt-8 pt-5 border-t border-gray-100 flex items-center justify-center">
              <button
                type="button"
                onClick={() => {
                  setActiveDropdown(null);
                  if (onNavigate) onNavigate('chat');
                }}
                className="group inline-flex items-center gap-2 text-xs sm:text-sm font-bold tracking-widest uppercase text-[#014043] hover:text-black py-1.5 px-4 rounded-full hover:bg-stone-50 transition-all duration-300"
              >
                <BookOpen className="w-4 h-4 text-[#014043] group-hover:text-black transition-colors" />
                <span className="underline underline-offset-4 decoration-1 decoration-[#014043]/40 group-hover:decoration-black">
                  {navigationData.recipesMegaMenu.bottomCta.label}
                </span>
              </button>
            </div>

          </div>
        </div>

        {/* =========================================
            5. THANH TÌM KIẾM THẢ XUỐNG (INTERACTIVE)
            Hiệu ứng dropdown & pull up trong 0.5s
           ========================================= */}
        <div
          className={`absolute top-full left-0 right-0 w-full bg-white border-t border-gray-100 shadow-lg px-4 py-4 z-40 transform transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
            isSearchOpen
              ? 'opacity-100 translate-y-0 pointer-events-auto visible'
              : 'opacity-0 -translate-y-3 pointer-events-none invisible'
          }`}
        >
          <div className="max-w-3xl mx-auto flex items-center gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={navigationData.actions.searchPlaceholder}
                className="w-full pl-11 pr-4 py-2.5 rounded-full border border-stone-200 bg-stone-50 text-sm focus:outline-none focus:ring-2 focus:ring-[#014043] focus:bg-white transition-all duration-200"
                autoFocus={isSearchOpen}
              />
            </div>
            <button
              type="button"
              onClick={() => {
                if (searchQuery.trim() && onNavigate) {
                  onNavigate('chat');
                }
                setIsSearchOpen(false);
              }}
              className="px-5 py-2.5 rounded-full bg-[#014043] text-white text-xs sm:text-sm font-bold hover:bg-[#002f32] transition-colors duration-200"
            >
              {navigationData.actions.searchButton}
            </button>
            <button
              type="button"
              onClick={() => setIsSearchOpen(false)}
              className="p-2 text-stone-400 hover:text-stone-700 rounded-full transition-colors"
              aria-label="Đóng tìm kiếm"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          
          {/* Gợi ý tìm kiếm */}
          <div className="max-w-3xl mx-auto mt-3 flex items-center gap-2 overflow-x-auto text-xs text-stone-500">
            <span className="font-semibold text-stone-700 shrink-0">{navigationData.actions.searchPrompt}</span>
            {['Bữa tối nhanh 15p', 'Cá hồi áp chảo', 'Thực đơn Eat Clean', 'Bò sốt vang', 'Món chay thanh đạm'].map((tag, i) => (
              <button
                key={i}
                type="button"
                onClick={() => {
                  setSearchQuery(tag);
                  if (onNavigate) onNavigate('chat');
                  setIsSearchOpen(false);
                }}
                className="px-2.5 py-1 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-700 whitespace-nowrap transition-colors duration-200"
              >
                {tag}
              </button>
            ))}
          </div>
        </div>

        {/* =========================================
            6. GIỎ NGUYÊN LIỆU MINI POPDOWN
            Hiệu ứng dropdown & pull up trong 0.5s
           ========================================= */}
        <div
          className={`absolute top-full right-4 sm:right-8 mt-1 w-80 bg-white rounded-2xl shadow-2xl border border-stone-100 p-5 z-40 transform transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
            isCartOpen
              ? 'opacity-100 translate-y-0 pointer-events-auto visible'
              : 'opacity-0 -translate-y-3 pointer-events-none invisible'
          }`}
        >
          <div className="flex items-center justify-between pb-3 border-b border-stone-100">
            <div className="flex items-center gap-2">
              <ShoppingCart className="w-5 h-5 text-[#014043]" />
              <h4 className="font-bold text-stone-900 text-sm">
                {navigationData.actions.cartTitle} ({cartCount})
              </h4>
            </div>
            <button
              type="button"
              onClick={() => setIsCartOpen(false)}
              className="text-stone-400 hover:text-stone-700 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
          
          <div className="py-3 space-y-2 text-xs">
            <div className="flex items-center justify-between p-2 rounded-lg bg-stone-50">
              <div>
                <p className="font-semibold text-stone-800">Cá hồi phi lê tươi (300g)</p>
                <p className="text-stone-400">Cho món Cá hồi sốt bơ chanh</p>
              </div>
              <span className="font-bold text-[#014043]">145.000đ</span>
            </div>
            <div className="flex items-center justify-between p-2 rounded-lg bg-stone-50">
              <div>
                <p className="font-semibold text-stone-800">Măng tây xanh loại 1 (250g)</p>
                <p className="text-stone-400">Rau củ tươi Đà Lạt</p>
              </div>
              <span className="font-bold text-[#014043]">45.000đ</span>
            </div>
          </div>

          <div className="pt-3 border-t border-stone-100 flex items-center justify-between text-xs font-semibold">
            <span className="text-stone-600">Tổng cộng ước tính:</span>
            <span className="text-sm font-bold text-stone-900">190.000đ</span>
          </div>

          <button
            type="button"
            onClick={() => {
              setIsCartOpen(false);
              if (onNavigate) onNavigate('chat');
            }}
            className="mt-3 w-full py-2.5 rounded-xl bg-[#014043] hover:bg-[#002f32] text-white text-xs font-bold text-center transition-colors duration-200"
          >
            Lên danh sách đi chợ cùng AI
          </button>
        </div>
      </header>

      {/* =========================================
          7. MENU TRƯỢT DI ĐỘNG (< 1024px)
         ========================================= */}
      {isMobileMenuOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex">
          {/* Lớp nền mờ */}
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity duration-300"
            onClick={() => setIsMobileMenuOpen(false)}
            aria-hidden="true"
          />

          {/* Ngăn kéo trượt */}
          <div className="relative ml-0 mr-auto w-full max-w-sm bg-white h-full shadow-2xl flex flex-col z-50 overflow-y-auto animate-in slide-in-from-left duration-300">
            {/* Đầu trang Drawer */}
            <div className="p-4 sm:p-5 flex items-center justify-between border-b border-gray-100">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-600 to-orange-500 text-white flex items-center justify-center">
                  <span className="text-base">🍲</span>
                </div>
                <span className="font-extrabold text-[#1f2937] tracking-tight text-lg uppercase">
                  {navigationData.brand.title}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setIsMobileMenuOpen(false)}
                className="p-2 text-stone-500 hover:text-stone-900 rounded-lg hover:bg-stone-100 transition-colors"
                aria-label="Đóng menu điều hướng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Ô tìm kiếm di động */}
            <div className="p-4 border-b border-gray-100">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
                <input
                  type="text"
                  placeholder="Tìm công thức, món ăn..."
                  className="w-full pl-9 pr-3 py-2 rounded-lg bg-stone-100 text-sm focus:outline-none focus:ring-2 focus:ring-[#014043]"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      setIsMobileMenuOpen(false);
                      if (onNavigate) onNavigate('chat');
                    }
                  }}
                />
              </div>
            </div>

            {/* Danh sách accordion điều hướng di động */}
            <div className="flex-1 px-4 py-3 space-y-1 overflow-y-auto">
              
              {/* Accordion 1: Công Thức */}
              <div className="border-b border-gray-100 py-1">
                <button
                  type="button"
                  onClick={() => toggleMobileSection('recipes')}
                  className="w-full flex items-center justify-between py-2.5 text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-colors"
                >
                  <span>{navigationData.recipesMegaMenu.label}</span>
                  <ChevronDown
                    className={`w-4 h-4 text-stone-400 transition-transform duration-300 ${
                      mobileExpandedSection === 'recipes' ? 'transform rotate-180 text-[#014043]' : ''
                    }`}
                  />
                </button>
                {mobileExpandedSection === 'recipes' && (
                  <div className="pl-3 pb-3 space-y-4 pt-1">
                    {navigationData.recipesMegaMenu.columns.map((col, idx) => (
                      <div key={idx} className="space-y-1.5">
                        <p className="text-xs font-bold uppercase tracking-wider text-stone-800">
                          {col.title}
                        </p>
                        <div className="grid grid-cols-2 gap-1.5 pl-1">
                          {col.items.slice(0, 4).map((item, itemIdx) => (
                            <button
                              key={itemIdx}
                              type="button"
                              onClick={() => {
                                setIsMobileMenuOpen(false);
                                if (onNavigate) onNavigate('chat');
                              }}
                              className="text-xs text-stone-500 hover:text-stone-900 text-left py-0.5 truncate transition-colors"
                            >
                              {item.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    ))}
                    <button
                      type="button"
                      onClick={() => {
                        setIsMobileMenuOpen(false);
                        if (onNavigate) onNavigate('chat');
                      }}
                      className="text-xs font-bold text-[#014043] flex items-center gap-1 pt-1"
                    >
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>{navigationData.recipesMegaMenu.bottomCta.label}</span>
                    </button>
                  </div>
                )}
              </div>

              {/* Accordion 2: Thực Đơn */}
              <div className="border-b border-gray-100 py-1">
                <button
                  type="button"
                  onClick={() => toggleMobileSection('mealPlans')}
                  className="w-full flex items-center justify-between py-2.5 text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-colors"
                >
                  <span>{navigationData.mealPlansDropdown.label}</span>
                  <ChevronDown
                    className={`w-4 h-4 text-stone-400 transition-transform duration-300 ${
                      mobileExpandedSection === 'mealPlans' ? 'transform rotate-180 text-[#014043]' : ''
                    }`}
                  />
                </button>
                {mobileExpandedSection === 'mealPlans' && (
                  <div className="pl-3 pb-3 space-y-2 pt-1">
                    {navigationData.mealPlansDropdown.items.map((item, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => {
                          setIsMobileMenuOpen(false);
                          if (!onNavigate) return;
                          if (item.href === '#personalize' || item.href === '/meal-planner') {
                            onNavigate('meal-planner');
                          } else if (item.href === '#more-plans' || item.href === '/meal-planner/app') {
                            onNavigate('meal-planner-app');
                          } else {
                            onNavigate('meal-planner');
                          }
                        }}
                        className="block w-full text-left text-sm text-stone-600 hover:text-stone-900 py-1 transition-colors"
                      >
                        {item.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Liên kết trực tiếp: Tủ Bếp */}
              <div className="border-b border-gray-100 py-1">
                <button
                  type="button"
                  onClick={() => {
                    setIsMobileMenuOpen(false);
                    if (onNavigate) onNavigate('chat');
                  }}
                  className="w-full text-left py-2.5 text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-colors"
                >
                  {navigationData.myPantry.label}
                </button>
              </div>

              {/* Accordion 3: Khám Phá Thêm */}
              <div className="border-b border-gray-100 py-1">
                <button
                  type="button"
                  onClick={() => toggleMobileSection('exploreMore')}
                  className="w-full flex items-center justify-between py-2.5 text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-colors"
                >
                  <span>{navigationData.exploreMoreDropdown.label}</span>
                  <ChevronDown
                    className={`w-4 h-4 text-stone-400 transition-transform duration-300 ${
                      mobileExpandedSection === 'exploreMore' ? 'transform rotate-180 text-[#014043]' : ''
                    }`}
                  />
                </button>
                {mobileExpandedSection === 'exploreMore' && (
                  <div className="pl-3 pb-3 space-y-2 pt-1">
                    {navigationData.exploreMoreDropdown.items.map((item, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setIsMobileMenuOpen(false)}
                        className="block w-full text-left text-sm text-stone-600 hover:text-stone-900 py-1 transition-colors"
                      >
                        {item.label}
                      </button>
                    ))}
                    {onOpenIntro && (
                      <button
                        type="button"
                        onClick={() => {
                          setIsMobileMenuOpen(false);
                          onOpenIntro();
                        }}
                        className="block w-full text-left text-sm text-amber-700 font-medium py-1 transition-colors"
                      >
                        Giới thiệu trợ lý AI
                      </button>
                    )}
                    {onOpenAbout && (
                      <button
                        type="button"
                        onClick={() => {
                          setIsMobileMenuOpen(false);
                          onOpenAbout();
                        }}
                        className="block w-full text-left text-sm text-orange-700 font-medium py-1 transition-colors"
                      >
                        Về chúng tôi
                      </button>
                    )}
                  </div>
                )}
              </div>

              {/* Liên kết trực tiếp: Dành Cho Doanh Nghiệp */}
              <div className="border-b border-gray-100 py-1">
                <a
                  href="#for-business"
                  onClick={(e) => {
                    e.preventDefault();
                    setIsMobileMenuOpen(false);
                  }}
                  className="flex items-center justify-between py-2.5 text-[15px] font-semibold text-[#1f2937] hover:text-[#014043] transition-colors"
                >
                  <span className="flex items-center gap-1.5">
                    {navigationData.forBusiness.label}
                    {navigationData.forBusiness.badge && (
                      <span className="text-[10px] uppercase font-bold px-1.5 py-0.5 rounded bg-orange-100 text-orange-700">
                        {navigationData.forBusiness.badge}
                      </span>
                    )}
                  </span>
                  <ExternalLink className="w-4 h-4 text-stone-400" />
                </a>
              </div>

            </div>

            {/* Các nút hành động dưới cùng trên di động */}
            <div className="p-4 border-t border-gray-100 space-y-2.5 bg-stone-50">
              <button
                type="button"
                onClick={handleSignUpClick}
                className="w-full py-2.5 rounded-full bg-[#014043] text-white font-bold text-xs uppercase tracking-wider text-center shadow-sm active:scale-98 transition-transform"
              >
                {navigationData.actions.signUp}
              </button>
              <button
                type="button"
                onClick={handleSignInClick}
                className="w-full py-2.5 rounded-full border border-stone-300 text-[#1f2937] font-bold text-xs uppercase tracking-wider text-center bg-white active:scale-98 transition-transform"
              >
                {navigationData.actions.signIn}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
