import React from 'react';
import {
  Clock,
  Bookmark,
  Star,
  ChevronDown,
  Users,
  Timer,
  Wallet,
  Leaf,
  Plus,
} from 'lucide-react';
import { useMealPlanner } from '../../context/MealPlannerContext';
import { DayOfWeek, GoalType, ServingSize, PlannerRecipe } from '../../types/mealPlanner';
import { BepNhaMinhFooter } from './BepNhaMinhFooter';

interface MealPlannerLandingProps {
  onNavigateToWizard: () => void;
  onSelectRecipePreview?: (recipe: PlannerRecipe) => void;
}

export const MealPlannerLanding: React.FC<MealPlannerLandingProps> = ({
  onNavigateToWizard,
  onSelectRecipePreview,
}) => {
  const { preferences, toggleDay, setServingSize, setPriorityGoal, allRecipes } =
    useMealPlanner();

  // 7 days in week: T2, T3, T4, T5, T6, T7, CN
  const daysList: { key: DayOfWeek; label: string; full: string }[] = [
    { key: 'mon', label: 'T2', full: 'Thứ Hai' },
    { key: 'tue', label: 'T3', full: 'Thứ Ba' },
    { key: 'wed', label: 'T4', full: 'Thứ Tư' },
    { key: 'thu', label: 'T5', full: 'Thứ Năm' },
    { key: 'fri', label: 'T6', full: 'Thứ Sáu' },
    { key: 'sat', label: 'T7', full: 'Thứ Bảy' },
    { key: 'sun', label: 'CN', full: 'Chủ Nhật' },
  ];

  const handleGetStarted = (e: React.FormEvent) => {
    e.preventDefault();
    if (preferences.selectedDays.length === 0) {
      alert('Vui lòng chọn ít nhất 1 ngày bạn muốn nấu ăn trong tuần!');
      return;
    }
    try {
      sessionStorage.setItem('bep_quick_setup_completed', 'true');
    } catch {}
    onNavigateToWizard();
  };

  // 3 featured recipes for the minimalist showcase (matching image layout)
  const featuredRecipes: {
    id: string;
    title: string;
    time: string;
    image: string;
    rating: number;
    fallbackRecipe?: PlannerRecipe;
  }[] = [
    {
      id: 'featured-1',
      title: 'Cá hồi áp chảo sốt bơ tỏi rau củ',
      time: '25 phút',
      rating: 5.0,
      image:
        'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=800&auto=format&fit=crop&q=80',
      fallbackRecipe: allRecipes[0],
    },
    {
      id: 'featured-2',
      title: 'Cơm gà xé Hội An nhanh gọn',
      time: '30 phút',
      rating: 5.0,
      image:
        'https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=800&auto=format&fit=crop&q=80',
      fallbackRecipe: allRecipes[1],
    },
    {
      id: 'featured-3',
      title: 'Bún bò trộn Nam Bộ thanh mát',
      time: '20 phút',
      rating: 5.0,
      image:
        'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop&q=80',
      fallbackRecipe: allRecipes[2],
    },
  ];

  return (
    <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col justify-between selection:bg-teal-100 selection:text-teal-900">
      <div>
        
        {/* ====================================================
            HERO SECTION: MINIMALIST & CHUẨN BẾP NHÀ MÌNH
            Trái: Tiêu đề lớn, thoáng đãng, không emoji ồn ào
            Phải: Card cấu hình nhanh (Quick Config Widget)
           ==================================================== */}
        <section className="relative overflow-hidden pt-12 sm:pt-16 pb-20 lg:pb-28 border-b border-stone-200/60 bg-gradient-to-b from-stone-50/80 via-[#FDFBF7] to-[#FDFBF7]">
          
          {/* Mảng ảnh món ăn decor góc phải phía sau card (Collage phong cách SideChef) */}
          <div className="absolute right-0 top-0 bottom-0 w-2/5 max-w-lg pointer-events-none hidden xl:block overflow-hidden opacity-90 select-none">
            <div className="relative w-full h-full">
              <img
                src="https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&auto=format&fit=crop&q=80"
                alt="Món ăn nền 1"
                className="absolute -top-10 right-4 w-60 h-60 rounded-3xl object-cover shadow-2xl rotate-3"
              />
              <img
                src="https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600&auto=format&fit=crop&q=80"
                alt="Món ăn nền 2"
                className="absolute top-44 -right-10 w-72 h-72 rounded-3xl object-cover shadow-2xl -rotate-6"
              />
              <img
                src="https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&auto=format&fit=crop&q=80"
                alt="Món ăn nền 3"
                className="absolute bottom-4 right-12 w-64 h-64 rounded-3xl object-cover shadow-2xl rotate-6"
              />
            </div>
          </div>

          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-14 items-center">
              
              {/* CỘT BÊN TRÁI: TIÊU ĐỀ LỚN & MÔ TẢ TINH GỌN (MINIMALIST) */}
              <div className="lg:col-span-7 space-y-6 text-left max-w-xl">
                <h1 className="text-4xl sm:text-5xl lg:text-[56px] font-black text-stone-900 tracking-tight leading-[1.12]">
                  Lên Kế Hoạch Bữa Ăn Thảnh Thơi Cho Người Bận Rộn
                </h1>

                <p className="text-base sm:text-lg text-stone-600 leading-relaxed font-normal">
                  Chủ động bữa cơm gia đình với thực đơn cá nhân hóa miễn phí. Tiết kiệm thời gian, tối ưu chi phí và ăn ngon hơn mỗi ngày.
                </p>
              </div>

              {/* CỘT BÊN PHẢI: CARD CẤU HÌNH NHANH (QUICK CONFIG WIDGET) */}
              <div className="lg:col-span-5 flex justify-end">
                <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-[0_20px_60px_rgba(0,0,0,0.07)] border border-stone-200/90 w-full max-w-md">
                  
                  <form onSubmit={handleGetStarted} className="space-y-5 text-left">
                    
                    {/* 1. Số ngày nấu trong tuần */}
                    <div>
                      <label className="block text-sm font-bold text-stone-900 mb-2.5">
                        Bạn cần nấu những ngày nào?
                      </label>

                      {/* 7 nút tròn đại diện T2..CN */}
                      <div className="grid grid-cols-7 gap-1.5 sm:gap-2">
                        {daysList.map((d) => {
                          const isSelected = preferences.selectedDays.includes(d.key);
                          return (
                            <button
                              key={d.key}
                              type="button"
                              onClick={() => toggleDay(d.key)}
                              className={`w-9 h-9 sm:w-10 sm:h-10 rounded-full font-bold text-xs sm:text-sm flex items-center justify-center transition-all cursor-pointer ${
                                isSelected
                                  ? 'bg-[#014043] text-white shadow-sm'
                                  : 'border border-stone-200 text-stone-600 hover:border-stone-400 bg-white'
                              }`}
                              title={d.full}
                            >
                              <span>{d.label}</span>
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    {/* 2. Khẩu phần ăn */}
                    <div>
                      <label className="block text-sm font-bold text-stone-900 mb-1.5">
                        Khẩu phần ăn
                      </label>
                      <div className="relative inline-block w-40">
                        <Users className="w-4 h-4 text-stone-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                        <select
                          value={preferences.servingSize}
                          onChange={(e) => setServingSize(Number(e.target.value) as ServingSize)}
                          className="w-full appearance-none bg-white hover:bg-stone-50 border border-stone-200 rounded-xl pl-9 pr-8 py-2 text-xs sm:text-sm font-semibold text-stone-800 outline-none focus:border-[#014043] cursor-pointer shadow-2xs"
                        >
                          <option value={1}>1 người</option>
                          <option value={2}>2 người</option>
                          <option value={3}>3 người</option>
                          <option value={4}>4 người</option>
                          <option value={5}>5+ người</option>
                        </select>
                        <ChevronDown className="w-4 h-4 text-stone-400 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                      </div>
                    </div>

                    {/* 3. Mục tiêu ưu tiên */}
                    <div>
                      <label className="block text-sm font-bold text-stone-900 mb-2">
                        Mục tiêu quan trọng nhất với bạn?
                      </label>

                      <div className="space-y-2">
                        {/* Quick & Easy */}
                        <div
                          onClick={() => setPriorityGoal('quick_easy')}
                          className={`flex items-center gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                            preferences.priorityGoal === 'quick_easy'
                              ? 'border-[#014043] bg-teal-50/30 ring-1 ring-[#014043]'
                              : 'border-stone-200 hover:border-stone-300 bg-white'
                          }`}
                        >
                          <div className="w-9 h-9 rounded-xl bg-stone-100 text-[#014043] flex items-center justify-center shrink-0">
                            <Timer className="w-4 h-4" />
                          </div>
                          <div>
                            <div className="text-xs sm:text-sm font-bold text-stone-900">
                              Nhanh & Tiện lợi
                            </div>
                            <div className="text-[11px] text-stone-500">
                              Dưới 30 phút
                            </div>
                          </div>
                        </div>

                        {/* Budget-Friendly */}
                        <div
                          onClick={() => setPriorityGoal('budget_friendly')}
                          className={`flex items-center gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                            preferences.priorityGoal === 'budget_friendly'
                              ? 'border-[#014043] bg-teal-50/30 ring-1 ring-[#014043]'
                              : 'border-stone-200 hover:border-stone-300 bg-white'
                          }`}
                        >
                          <div className="w-9 h-9 rounded-xl bg-stone-100 text-[#014043] flex items-center justify-center shrink-0">
                            <Wallet className="w-4 h-4" />
                          </div>
                          <div>
                            <div className="text-xs sm:text-sm font-bold text-stone-900">
                              Tiết kiệm chi phí
                            </div>
                            <div className="text-[11px] text-stone-500">
                              Dưới 35.000 ₫ / khẩu phần
                            </div>
                          </div>
                        </div>

                        {/* Eat Healthy */}
                        <div
                          onClick={() => setPriorityGoal('eat_healthy')}
                          className={`flex items-center gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                            preferences.priorityGoal === 'eat_healthy'
                              ? 'border-[#014043] bg-teal-50/30 ring-1 ring-[#014043]'
                              : 'border-stone-200 hover:border-stone-300 bg-white'
                          }`}
                        >
                          <div className="w-9 h-9 rounded-xl bg-stone-100 text-[#014043] flex items-center justify-center shrink-0">
                            <Leaf className="w-4 h-4" />
                          </div>
                          <div>
                            <div className="text-xs sm:text-sm font-bold text-stone-900">
                              Ăn lành mạnh
                            </div>
                            <div className="text-[11px] text-stone-500">
                              Cân bằng dinh dưỡng
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* 4. Action Button: BẮT ĐẦU NGAY */}
                    <div className="pt-2">
                      <button
                        type="submit"
                        className="w-full py-3.5 px-6 rounded-full bg-[#014043] hover:bg-[#002f32] active:scale-[0.99] text-white font-bold text-xs sm:text-sm tracking-wider uppercase transition-all shadow-sm cursor-pointer"
                      >
                        BẮT ĐẦU NGAY
                      </button>
                      <p className="text-center text-[11px] text-stone-400 mt-2.5">
                        Tùy chỉnh dị ứng, chế độ ăn và nguyên liệu ở bước tiếp theo
                      </p>
                    </div>

                  </form>
                </div>
              </div>

            </div>
          </div>
        </section>

        {/* ====================================================
            SECTION 2: CÁCH THỨC HOẠT ĐỘNG (HOW IT WORKS)
            4 bước trực quan, minh họa tròn tinh giản (Minimalist)
           ==================================================== */}
        <section className="py-16 sm:py-24 bg-white border-b border-stone-200/60">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            
            <h2 className="text-2xl sm:text-3xl font-bold text-stone-900 mb-12 sm:mb-16 tracking-tight">
              Cách thức hoạt động
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-10">
              
              {/* Bước 1: Chia sẻ khẩu vị */}
              <div className="flex flex-col items-center text-center max-w-xs mx-auto">
                <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-full bg-orange-50 border border-orange-100 flex items-center justify-center mb-5 shadow-2xs">
                  <svg className="w-12 h-12 text-orange-600" viewBox="0 0 48 48" fill="none">
                    <rect x="12" y="8" width="24" height="32" rx="4" stroke="currentColor" strokeWidth="2.5" />
                    <line x1="20" y1="14" x2="28" y2="14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    <circle cx="24" cy="34" r="1.5" fill="currentColor" />
                    <path d="M28 20L34 26" stroke="#ea580c" strokeWidth="2.5" strokeLinecap="round" />
                    <path d="M34 20L28 26" stroke="#ea580c" strokeWidth="2.5" strokeLinecap="round" />
                  </svg>
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  Chia sẻ khẩu vị
                </h3>
                <p className="text-xs text-stone-500 leading-relaxed">
                  Cho Bếp biết bạn thích gì và chúng tôi sẽ cá nhân hóa thực đơn phù hợp cho bạn.
                </p>
              </div>

              {/* Bước 2: Duyệt thực đơn */}
              <div className="flex flex-col items-center text-center max-w-xs mx-auto">
                <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-full bg-amber-50 border border-amber-100 flex items-center justify-center mb-5 shadow-2xs">
                  <svg className="w-12 h-12 text-amber-700" viewBox="0 0 48 48" fill="none">
                    <rect x="10" y="10" width="20" height="26" rx="3" stroke="currentColor" strokeWidth="2" />
                    <rect x="18" y="14" width="20" height="26" rx="3" stroke="#d97706" strokeWidth="2.5" fill="white" />
                    <line x1="22" y1="20" x2="34" y2="20" stroke="#d97706" strokeWidth="2" strokeLinecap="round" />
                    <line x1="22" y1="25" x2="30" y2="25" stroke="#d97706" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  Duyệt thực đơn
                </h3>
                <p className="text-xs text-stone-500 leading-relaxed">
                  Xem thực đơn tuần gợi ý, đổi món hoặc thêm món yêu thích bất kỳ lúc nào.
                </p>
              </div>

              {/* Bước 3: Đi chợ thông minh */}
              <div className="flex flex-col items-center text-center max-w-xs mx-auto">
                <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-full bg-teal-50 border border-teal-100 flex items-center justify-center mb-5 shadow-2xs">
                  <svg className="w-12 h-12 text-[#014043]" viewBox="0 0 48 48" fill="none">
                    <path d="M10 16H38L35 34H13L10 16Z" stroke="currentColor" strokeWidth="2.5" strokeLinejoin="round" />
                    <path d="M18 16V12C18 9.79086 19.7909 8 22 8H26C28.2091 8 30 9.79086 30 12V16" stroke="currentColor" strokeWidth="2.5" />
                    <line x1="20" y1="22" x2="20" y2="28" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    <line x1="28" y1="22" x2="28" y2="28" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  Đi chợ thông minh
                </h3>
                <p className="text-xs text-stone-500 leading-relaxed">
                  Tự động gom nguyên liệu cần mua, phân loại theo từng quầy tiện lợi.
                </p>
              </div>

              {/* Bước 4: Nấu & Thưởng thức */}
              <div className="flex flex-col items-center text-center max-w-xs mx-auto">
                <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-full bg-emerald-50 border border-emerald-100 flex items-center justify-center mb-5 shadow-2xs">
                  <svg className="w-12 h-12 text-emerald-700" viewBox="0 0 48 48" fill="none">
                    <ellipse cx="22" cy="26" rx="14" ry="7" stroke="currentColor" strokeWidth="2.5" />
                    <path d="M33 28L42 37" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
                    <path d="M18 17C18 17 20 14 20 12" stroke="#059669" strokeWidth="2" strokeLinecap="round" />
                    <path d="M24 18C24 18 26 15 26 13" stroke="#059669" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  Nấu & Thưởng thức
                </h3>
                <p className="text-xs text-stone-500 leading-relaxed">
                  Tận hưởng bữa cơm ngon miệng tại nhà với các bước hướng dẫn nấu chi tiết.
                </p>
              </div>

            </div>

          </div>
        </section>

        {/* ====================================================
            SECTION 3: THỰC ĐƠN TUẦN NÀY CÓ GÌ? (WHAT'S ON THE MENU?)
            3 thẻ món ăn thanh lịch, tối giản đúng theo hình mẫu
           ==================================================== */}
        <section className="py-16 sm:py-24 bg-[#FDFBF7]">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            
            <h2 className="text-2xl sm:text-3xl font-bold text-stone-900 mb-10 sm:mb-14 tracking-tight">
              Thực đơn tuần này có gì?
            </h2>

            {/* 3 Recipe Cards Showcase */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8 text-left">
              {featuredRecipes.map((item) => (
                <div
                  key={item.id}
                  className="bg-white rounded-3xl overflow-hidden border border-stone-200/90 shadow-xs hover:shadow-xl transition-all duration-300 flex flex-col justify-between group"
                >
                  <div>
                    {/* Ảnh món ăn */}
                    <div className="relative aspect-4/3 overflow-hidden bg-stone-100">
                      <img
                        src={item.image}
                        alt={item.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                      />

                      {/* Nút Bookmark góc trên bên phải */}
                      <button
                        type="button"
                        className="absolute top-3 right-3 w-8 h-8 rounded-full bg-white/85 hover:bg-white text-stone-700 hover:text-[#014043] flex items-center justify-center shadow-xs transition-all cursor-pointer"
                        title="Lưu công thức"
                        onClick={() => {
                          if (item.fallbackRecipe && onSelectRecipePreview) {
                            onSelectRecipePreview(item.fallbackRecipe);
                          }
                        }}
                      >
                        <Bookmark className="w-4 h-4" />
                      </button>

                      {/* Huy hiệu thời gian góc dưới bên trái */}
                      <div className="absolute bottom-3 left-3 px-2.5 py-1 rounded-full bg-black/60 backdrop-blur-xs text-white text-[11px] font-bold flex items-center gap-1.5">
                        <Clock className="w-3 h-3 text-stone-300" />
                        <span>{item.time}</span>
                      </div>
                    </div>

                    {/* Nội dung thẻ */}
                    <div className="p-5">
                      {/* Đánh giá sao */}
                      <div className="flex items-center gap-1 text-amber-500 text-xs mb-2">
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                      </div>

                      {/* Tiêu đề món */}
                      <h3 className="font-bold text-base text-stone-900 line-clamp-1 mb-1">
                        {item.title}
                      </h3>
                    </div>
                  </div>

                  {/* Nút + THÊM VÀO THỰC ĐƠN */}
                  <div className="p-5 pt-0">
                    <button
                      type="button"
                      onClick={() => {
                        if (item.fallbackRecipe && onSelectRecipePreview) {
                          onSelectRecipePreview(item.fallbackRecipe);
                        } else {
                          onNavigateToWizard();
                        }
                      }}
                      className="w-full py-2.5 rounded-full bg-[#014043] hover:bg-[#002f32] active:scale-[0.99] text-white text-xs font-bold uppercase tracking-wider transition-all shadow-xs cursor-pointer flex items-center justify-center gap-1.5"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>THÊM VÀO THỰC ĐƠN</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>

          </div>
        </section>

      </div>

      {/* FOOTER BẾP NHÀ MÌNH (ĐỒNG NHẤT MÀU XANH #014043) */}
      <BepNhaMinhFooter />

    </div>
  );
};
