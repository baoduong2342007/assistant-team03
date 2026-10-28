import React, { useState } from 'react';
import {
  Clock,
  Bookmark,
  Star,
  ChevronRight,
  ChevronLeft,
  ChevronDown,
  ShoppingBag,
  ChefHat,
  Sparkles,
  ArrowRight,
  Play,
  Pause,
  CheckCircle2,
  DollarSign,
  Heart,
  ShieldCheck,
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
  const { preferences, toggleDay, setServingSize, setPriorityGoal, formatPrice, allRecipes } =
    useMealPlanner();

  // 7 days in week (T2, T3, T4, T5, T6, T7, CN)
  const daysList: { key: DayOfWeek; label: string; full: string }[] = [
    { key: 'mon', label: 'T2', full: 'Thứ Hai' },
    { key: 'tue', label: 'T3', full: 'Thứ Ba' },
    { key: 'wed', label: 'T4', full: 'Thứ Tư' },
    { key: 'thu', label: 'T5', full: 'Thứ Năm' },
    { key: 'fri', label: 'T6', full: 'Thứ Sáu' },
    { key: 'sat', label: 'T7', full: 'Thứ Bảy' },
    { key: 'sun', label: 'CN', full: 'Chủ Nhật' },
  ];

  const [isVideoPlaying, setIsVideoPlaying] = useState(false);
  const [selectedTagFilter, setSelectedTagFilter] = useState('Tất cả');

  // Days quick presets
  const handleSelectWorkdays = () => {
    const workdays: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri'];
    workdays.forEach((day) => {
      if (!preferences.selectedDays.includes(day)) toggleDay(day);
    });
    ['sat', 'sun'].forEach((day) => {
      if (preferences.selectedDays.includes(day as DayOfWeek)) toggleDay(day as DayOfWeek);
    });
  };

  const handleSelectAllDays = () => {
    const allDays: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
    allDays.forEach((day) => {
      if (!preferences.selectedDays.includes(day)) toggleDay(day);
    });
  };

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

  const filteredRecipes = allRecipes.filter((r) => {
    if (selectedTagFilter === 'Tất cả') return true;
    if (selectedTagFilter === 'Nhanh gọn') return r.cookTime <= 20;
    if (selectedTagFilter === 'Tiết kiệm') return r.costPerServing <= 40000;
    if (selectedTagFilter === 'Lành mạnh') return r.calories <= 400;
    if (selectedTagFilter === 'Món Chay') return r.diet.includes('vegetarian') || r.diet.includes('vegan');
    return true;
  });

  return (
    <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col justify-between">
      <div>
        
        {/* ====================================================
            HERO SECTION: BẾP NHÀ MÌNH
            Trái: Headline ấm áp, giàu cảm xúc gia đình Việt
            Phải: Card cấu hình nhanh (Quick Config Widget)
           ==================================================== */}
        <section className="relative overflow-hidden bg-gradient-to-b from-orange-50/70 via-[#FDFBF7] to-[#FDFBF7] pt-8 sm:pt-12 pb-16 lg:pb-24 border-b border-stone-200/70">
          
          {/* Mâm cơm tròn trang trí phía góc phải */}
          <div className="absolute right-0 top-0 bottom-0 w-1/3 max-w-sm pointer-events-none hidden xl:flex flex-col justify-between opacity-80 overflow-hidden">
            <img
              src="https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop&q=80"
              alt="Mâm cơm cá kho"
              className="w-56 h-56 rounded-full object-cover translate-x-16 -translate-y-6 shadow-xl"
            />
            <img
              src="https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&auto=format&fit=crop&q=80"
              alt="Mâm cơm rau luộc"
              className="w-64 h-64 rounded-full object-cover translate-x-12 shadow-xl"
            />
            <img
              src="https://images.unsplash.com/photo-1512058564366-18510be2db19?w=600&auto=format&fit=crop&q=80"
              alt="Mâm cơm canh nóng"
              className="w-60 h-60 rounded-full object-cover translate-x-16 translate-y-6 shadow-xl"
            />
          </div>

          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
              
              {/* CỘT BÊN TRÁI: TIÊU ĐỀ & LỜI GIỚI THIỆU */}
              <div className="lg:col-span-7 space-y-6 max-w-2xl text-left">
                
                <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-orange-100 text-orange-900 text-xs font-bold border border-orange-200/80 shadow-xs">
                  <span className="text-base">🍲</span>
                  <span>Bếp Nhà Mình • Trợ Lý Bữa Ăn & Dinh Dưỡng Gia Đình</span>
                </div>

                <h1 className="text-3xl sm:text-5xl lg:text-6xl font-extrabold text-stone-900 font-serif-display tracking-tight leading-[1.15]">
                  Lập Kế Hoạch Bữa Ăn
                  <span className="block text-transparent bg-clip-text bg-gradient-to-r from-orange-700 via-amber-700 to-amber-600">
                    Thảnh Thơi Cho Người Bận Rộn
                  </span>
                </h1>

                <p className="text-base sm:text-lg text-stone-600 leading-relaxed font-normal">
                  Hôm nay nhà mình ăn gì? Để Bếp lo trọn vẹn từng mâm cơm tròn vị. Tiết kiệm chi phí đi chợ, chuẩn dinh dưỡng từng thành viên và không còn phải đau đầu tự hỏi mỗi chiều tan sở.
                </p>

                {/* 3 giá trị cốt lõi */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
                  <div className="flex items-center gap-2.5 p-3 rounded-2xl bg-white border border-stone-200 shadow-xs">
                    <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold text-sm shrink-0">
                      ✓
                    </div>
                    <div>
                      <div className="text-xs font-bold text-stone-900">100% Miễn Phí</div>
                      <div className="text-[11px] text-stone-500">Trọn đời cho gia đình</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2.5 p-3 rounded-2xl bg-white border border-stone-200 shadow-xs">
                    <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center font-bold text-sm shrink-0">
                      ⏱️
                    </div>
                    <div>
                      <div className="text-xs font-bold text-stone-900">Tiết Kiệm 3+ Giờ</div>
                      <div className="text-[11px] text-stone-500">Mỗi tuần nấu nướng</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2.5 p-3 rounded-2xl bg-white border border-stone-200 shadow-xs">
                    <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-800 flex items-center justify-center font-bold text-sm shrink-0">
                      🥗
                    </div>
                    <div>
                      <div className="text-xs font-bold text-stone-900">Chuẩn Dinh Dưỡng</div>
                      <div className="text-[11px] text-stone-500">Hợp bệnh lý từng người</div>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 text-xs text-stone-500 pt-1">
                  <span>Hơn <strong>45.000+</strong> mâm cơm gia đình Việt đã được lên kế hoạch thành công mỗi tuần</span>
                </div>
              </div>

              {/* CỘT BÊN PHẢI: CARD CẤU HÌNH NHANH (QUICK CONFIG WIDGET) */}
              <div className="lg:col-span-5">
                <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-[0_15px_45px_rgba(0,0,0,0.06)] border border-stone-200/90 relative">
                  
                  {/* Card Header */}
                  <div className="flex items-center justify-between pb-4 mb-4 border-b border-stone-100">
                    <div>
                      <span className="text-[11px] font-bold text-orange-600 uppercase tracking-wider">
                        THIẾT LẬP NHANH 1 PHÚT
                      </span>
                      <h2 className="text-xl font-bold font-serif-display text-stone-900">
                        Tùy Chọn Thực Đơn Tuần
                      </h2>
                    </div>
                    <div className="w-9 h-9 rounded-2xl bg-orange-50 text-orange-600 flex items-center justify-center font-bold text-lg">
                      ⚙️
                    </div>
                  </div>

                  <form onSubmit={handleGetStarted} className="space-y-5 text-left">
                    
                    {/* 1. Số ngày nấu trong tuần */}
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <label className="text-xs font-bold text-stone-800 flex items-center gap-1.5">
                          <span>1. Bạn cần nấu những ngày nào trong tuần?</span>
                        </label>
                        <div className="flex items-center gap-1.5 text-[11px]">
                          <button
                            type="button"
                            onClick={handleSelectWorkdays}
                            className="text-stone-500 hover:text-orange-600 underline cursor-pointer"
                          >
                            T2-T6
                          </button>
                          <span className="text-stone-300">|</span>
                          <button
                            type="button"
                            onClick={handleSelectAllDays}
                            className="text-stone-500 hover:text-orange-600 underline cursor-pointer"
                          >
                            Cả tuần
                          </button>
                        </div>
                      </div>

                      {/* 7 nút tròn đại diện T2..CN */}
                      <div className="grid grid-cols-7 gap-1.5 sm:gap-2">
                        {daysList.map((d) => {
                          const isSelected = preferences.selectedDays.includes(d.key);
                          return (
                            <button
                              key={d.key}
                              type="button"
                              onClick={() => toggleDay(d.key)}
                              className={`py-2 rounded-2xl font-bold text-xs sm:text-sm flex flex-col items-center justify-center transition-all cursor-pointer border ${
                                isSelected
                                  ? 'bg-[#014043] text-white border-[#014043] shadow-xs scale-102 ring-2 ring-[#014043]/20'
                                  : 'bg-stone-50 text-stone-600 border-stone-200 hover:bg-stone-100'
                              }`}
                              title={d.full}
                            >
                              <span>{d.label}</span>
                            </button>
                          );
                        })}
                      </div>
                      <div className="mt-1 text-[11px] text-stone-500">
                        Đang chọn: <strong className="text-stone-800">{preferences.selectedDays.length} ngày nấu</strong>
                      </div>
                    </div>

                    {/* 2. Khẩu phần ăn */}
                    <div>
                      <label className="block text-xs font-bold text-stone-800 mb-1.5">
                        2. Khẩu phần ăn cho bao nhiêu người?
                      </label>
                      <div className="relative">
                        <select
                          value={preferences.servingSize}
                          onChange={(e) => setServingSize(Number(e.target.value) as ServingSize)}
                          className="w-full appearance-none bg-stone-50 hover:bg-stone-100 border border-stone-200 rounded-2xl px-4 py-2.5 text-xs sm:text-sm font-bold text-stone-800 outline-none focus:border-[#014043] cursor-pointer pr-10 shadow-xs"
                        >
                          <option value={1}>1 người (Độc thân / Bữa ăn cá nhân)</option>
                          <option value={2}>2 người (Cặp đôi / Vợ chồng)</option>
                          <option value={3}>3 người (Gia đình nhỏ)</option>
                          <option value={4}>4 người (Gia đình tiêu chuẩn 2 thế hệ)</option>
                          <option value={5}>5+ người (Gia đình đông thành viên)</option>
                        </select>
                        <ChevronDown className="w-4 h-4 text-stone-500 absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none" />
                      </div>
                    </div>

                    {/* 3. Mục tiêu ưu tiên */}
                    <div>
                      <label className="block text-xs font-bold text-stone-800 mb-2">
                        3. Mục tiêu nào quan trọng nhất với gia đình bạn?
                      </label>

                      <div className="space-y-2">
                        {/* Quick & Easy */}
                        <label
                          onClick={() => setPriorityGoal('quick_easy')}
                          className={`flex items-center gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                            preferences.priorityGoal === 'quick_easy'
                              ? 'border-orange-500 bg-orange-50/70 ring-1 ring-orange-400'
                              : 'border-stone-200 hover:border-stone-300 bg-stone-50/50'
                          }`}
                        >
                          <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-700 flex items-center justify-center text-sm shrink-0">
                            ⚡
                          </div>
                          <div className="flex-1">
                            <div className="text-xs sm:text-sm font-bold text-stone-900">
                              Nhanh & Tiện Lợi (Quick & Easy)
                            </div>
                            <div className="text-[11px] text-stone-500">
                              Dưới 30 phút, ít bước sơ chế, gọn gàng dọn dẹp
                            </div>
                          </div>
                        </label>

                        {/* Budget-Friendly */}
                        <label
                          onClick={() => setPriorityGoal('budget_friendly')}
                          className={`flex items-center gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                            preferences.priorityGoal === 'budget_friendly'
                              ? 'border-orange-500 bg-orange-50/70 ring-1 ring-orange-400'
                              : 'border-stone-200 hover:border-stone-300 bg-stone-50/50'
                          }`}
                        >
                          <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center text-sm shrink-0">
                            💰
                          </div>
                          <div className="flex-1">
                            <div className="text-xs sm:text-sm font-bold text-stone-900">
                              Tiết Kiệm Chi Phí (Budget-Friendly)
                            </div>
                            <div className="text-[11px] text-stone-500">
                              Dưới 40.000 ₫/khẩu phần, tận dụng nguyên liệu theo mùa
                            </div>
                          </div>
                        </label>

                        {/* Eat Healthy */}
                        <label
                          onClick={() => setPriorityGoal('eat_healthy')}
                          className={`flex items-center gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                            preferences.priorityGoal === 'eat_healthy'
                              ? 'border-orange-500 bg-orange-50/70 ring-1 ring-orange-400'
                              : 'border-stone-200 hover:border-stone-300 bg-stone-50/50'
                          }`}
                        >
                          <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center text-sm shrink-0">
                            🥗
                          </div>
                          <div className="flex-1">
                            <div className="text-xs sm:text-sm font-bold text-stone-900">
                              Ăn Khỏe & Cân Bằng (Eat Healthy)
                            </div>
                            <div className="text-[11px] text-stone-500">
                              Đủ 4 nhóm chất, giảm muối mặn, giàu chất xơ & đạm nạc
                            </div>
                          </div>
                        </label>
                      </div>
                    </div>

                    {/* 4. Action Button: BẮT ĐẦU NGAY */}
                    <div className="pt-2">
                      <button
                        type="submit"
                        className="w-full py-3.5 px-6 rounded-2xl bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 text-white font-extrabold text-xs sm:text-sm tracking-wider uppercase transition-all shadow-md shadow-orange-600/20 active:scale-[0.99] cursor-pointer flex items-center justify-center gap-2"
                      >
                        <span>BẮT ĐẦU NGAY (GET STARTED)</span>
                        <ArrowRight className="w-4 h-4" />
                      </button>
                      <p className="text-center text-[10px] text-stone-400 mt-2">
                        Tự động điều hướng sang bước khảo sát khẩu vị & loại trừ dị ứng chi tiết
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
           ==================================================== */}
        <section className="py-16 sm:py-20 bg-white border-b border-stone-200/70">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            
            <span className="text-xs font-bold text-orange-600 tracking-widest uppercase">
              QUY TRÌNH BẾP NHÀ MÌNH
            </span>
            <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 mt-1 mb-12 sm:mb-16">
              Cách Thức Hoạt Động (How it works)
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 text-center">
              
              {/* Bước 1 */}
              <div className="flex flex-col items-center max-w-xs mx-auto">
                <div className="w-20 h-20 rounded-3xl bg-orange-100 text-orange-800 flex items-center justify-center mb-4 text-3xl shadow-xs">
                  📝
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  1. Khảo Sát Khẩu Vị & Bệnh Lý
                </h3>
                <p className="text-xs text-stone-600 leading-relaxed">
                  Thiết lập số ngày nấu, dị ứng thực phẩm và bệnh lý (huyết áp, tiểu đường, gout...) của từng người.
                </p>
              </div>

              {/* Bước 2 */}
              <div className="flex flex-col items-center max-w-xs mx-auto">
                <div className="w-20 h-20 rounded-3xl bg-amber-100 text-amber-800 flex items-center justify-center mb-4 text-3xl shadow-xs">
                  🍲
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  2. Gợi Ý Thực Đơn Thông Minh
                </h3>
                <p className="text-xs text-stone-600 leading-relaxed">
                  Hệ thống tự động sắp xếp mâm cơm đủ 4 nhóm chất, an toàn tuyệt đối và tính toán chi phí từng suất.
                </p>
              </div>

              {/* Bước 3 */}
              <div className="flex flex-col items-center max-w-xs mx-auto">
                <div className="w-20 h-20 rounded-3xl bg-teal-100 text-teal-800 flex items-center justify-center mb-4 text-3xl shadow-xs">
                  ⇄
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  3. Kéo - Thả & Đổi Món (Swap)
                </h3>
                <p className="text-xs text-stone-600 leading-relaxed">
                  Kéo thẻ món ăn sang ngày khác hoặc bấm nút Swap để đổi ngẫu nhiên món phù hợp trong kho công thức.
                </p>
              </div>

              {/* Bước 4 */}
              <div className="flex flex-col items-center max-w-xs mx-auto">
                <div className="w-20 h-20 rounded-3xl bg-emerald-100 text-emerald-800 flex items-center justify-center mb-4 text-3xl shadow-xs">
                  🛒
                </div>
                <h3 className="font-bold text-sm sm:text-base text-stone-900 mb-1.5">
                  4. Tạo Giỏ Đi Chợ 1-Chạm
                </h3>
                <p className="text-xs text-stone-600 leading-relaxed">
                  Tự động gom nguyên liệu cần mua theo quầy rau củ, thịt cá, gia vị. Đánh dấu đồ đã có sẵn để trừ tiền.
                </p>
              </div>

            </div>

          </div>
        </section>

        {/* ====================================================
            SECTION 3: VIDEO HƯỚNG DẪN MÔ PHỎNG NẤU ĂN
           ==================================================== */}
        <section className="py-16 sm:py-20 bg-[#FDFBF7] border-b border-stone-200/70">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 text-center">
            
            <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 mb-2">
              Xem Video Hướng Dẫn Lên Lịch Thực Đơn
            </h2>
            <p className="text-xs sm:text-sm text-stone-600 mb-8 max-w-xl mx-auto">
              Chỉ mất 2 phút để hiểu toàn bộ thao tác Kéo & Thả, Thay đổi món (Swap) và cách quản lý chi phí giỏ hàng.
            </p>

            <div className="relative rounded-3xl overflow-hidden shadow-xl border-4 border-white aspect-video bg-stone-900 flex items-center justify-center group">
              <img
                src="https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=1400&auto=format&fit=crop&q=80"
                alt="Video mockup"
                className="absolute inset-0 w-full h-full object-cover opacity-75 group-hover:scale-105 transition-transform duration-700"
              />
              <div className="absolute inset-0 bg-black/40" />

              {isVideoPlaying ? (
                <div className="relative z-10 text-white p-6 space-y-3">
                  <span className="px-3 py-1 rounded-full bg-emerald-500/30 text-emerald-300 text-xs font-bold border border-emerald-400/40">
                    Đang phát video mô phỏng
                  </span>
                  <h3 className="text-lg sm:text-xl font-bold">Lập thực đơn tuần thông minh cùng Bếp Nhà Mình</h3>
                  <button
                    type="button"
                    onClick={() => setIsVideoPlaying(false)}
                    className="px-5 py-2 rounded-full bg-white text-stone-900 text-xs font-bold cursor-pointer hover:bg-stone-100"
                  >
                    Tạm dừng
                  </button>
                </div>
              ) : (
                <div className="relative z-10 text-center">
                  <button
                    type="button"
                    onClick={() => setIsVideoPlaying(true)}
                    className="w-18 h-18 sm:w-20 sm:h-20 rounded-full bg-orange-600 hover:bg-orange-500 text-white flex items-center justify-center shadow-2xl hover:scale-110 active:scale-95 transition-all cursor-pointer mx-auto mb-3"
                  >
                    <Play className="w-8 h-8 fill-white translate-x-1" />
                  </button>
                  <div className="text-white font-bold text-sm drop-shadow-sm">
                    Bấm để xem video hướng dẫn (2:15)
                  </div>
                </div>
              )}
            </div>

          </div>
        </section>

        {/* ====================================================
            SECTION 4: THỰC ĐƠN TUẦN NÀY CÓ GÌ? (WHAT'S ON THE MENU?)
           ==================================================== */}
        <section className="py-16 sm:py-20 bg-white">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-left">
            
            <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
              <div>
                <span className="text-xs font-bold text-orange-600 tracking-widest uppercase">
                  MÂM CƠM TUẦN NÀY
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 mt-1">
                  Thực Đơn Của Bếp Có Gì? (What's On The Menu?)
                </h2>
                <p className="text-xs sm:text-sm text-stone-600 mt-1">
                  Các món ăn đậm đà chuẩn vị gia đình Việt, định lượng dinh dưỡng rõ ràng và nguyên liệu tươi ngon.
                </p>
              </div>

              {/* Tag filters */}
              <div className="flex flex-wrap gap-2">
                {['Tất cả', 'Nhanh gọn', 'Tiết kiệm', 'Lành mạnh', 'Món Chay'].map((t) => (
                  <button
                    key={t}
                    type="button"
                    onClick={() => setSelectedTagFilter(t)}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-bold cursor-pointer transition-colors ${
                      selectedTagFilter === t
                        ? 'bg-[#014043] text-white shadow-xs'
                        : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* Grid recipes */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {filteredRecipes.slice(0, 8).map((recipe) => (
                <div
                  key={recipe.id}
                  className="bg-white rounded-3xl overflow-hidden border border-stone-200 shadow-xs hover:shadow-xl transition-all flex flex-col justify-between"
                >
                  <div>
                    <div className="relative aspect-4/3 overflow-hidden bg-stone-100">
                      <img
                        src={recipe.image}
                        alt={recipe.title}
                        className="w-full h-full object-cover hover:scale-105 transition-transform duration-500"
                      />

                      <div className="absolute top-2.5 left-2.5 flex items-center gap-1.5">
                        <span className="px-2 py-0.5 rounded-md bg-black/60 backdrop-blur-xs text-white text-[10px] font-bold flex items-center gap-1">
                          <Clock className="w-3 h-3 text-amber-300" />
                          <span>{recipe.cookTime}p</span>
                        </span>
                        <span className="px-2 py-0.5 rounded-md bg-emerald-700/90 backdrop-blur-xs text-white text-[10px] font-bold">
                          {formatPrice(recipe.costPerServing)}/suất
                        </span>
                      </div>
                    </div>

                    <div className="p-4">
                      <div className="flex items-center gap-1 text-amber-500 text-xs mb-1 font-bold">
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <span>{recipe.rating}</span>
                        <span className="text-stone-400 font-normal">({recipe.calories} kcal)</span>
                      </div>

                      <h3 className="font-bold text-sm text-stone-900 line-clamp-1">
                        {recipe.title}
                      </h3>

                      <p className="text-xs text-stone-500 mt-1 line-clamp-2 leading-relaxed">
                        {recipe.description}
                      </p>
                    </div>
                  </div>

                  <div className="p-4 pt-0">
                    <button
                      type="button"
                      onClick={() => {
                        if (onSelectRecipePreview) onSelectRecipePreview(recipe);
                        else onNavigateToWizard();
                      }}
                      className="w-full py-2 px-3 rounded-xl bg-orange-50 hover:bg-orange-100 text-orange-800 text-xs font-bold transition-colors cursor-pointer flex items-center justify-center gap-1.5"
                    >
                      <ShoppingBag className="w-3.5 h-3.5" />
                      <span>Xem Nguyên Liệu & Cách Nấu</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-12 text-center">
              <button
                type="button"
                onClick={onNavigateToWizard}
                className="inline-flex items-center gap-2 px-8 py-3.5 rounded-full bg-[#014043] hover:bg-[#002f32] text-white text-sm font-bold shadow-md cursor-pointer transition-all"
              >
                <span>TIẾP TỤC ĐẾN BƯỚC KHẢO SÁT KHẨU VỊ</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>

          </div>
        </section>

      </div>

      {/* FOOTER BẾP NHÀ MÌNH */}
      <BepNhaMinhFooter />

    </div>
  );
};
