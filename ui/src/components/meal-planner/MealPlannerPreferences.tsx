import React, { useState } from 'react';
import {
  ChevronDown,
  ChevronUp,
  Users,
  Timer,
  Wallet,
  Leaf,
  Check,
} from 'lucide-react';
import { useMealPlanner } from '../../context/MealPlannerContext';
import { DayOfWeek, GoalType, ServingSize, DietType, AllergenType } from '../../types/mealPlanner';

interface MealPlannerPreferencesProps {
  onBackToLanding: () => void;
  onSaveAndContinue: () => void;
}

export const MealPlannerPreferences: React.FC<MealPlannerPreferencesProps> = ({
  onBackToLanding,
  onSaveAndContinue,
}) => {
  const {
    preferences,
    toggleDay,
    setServingSize,
    setPriorityGoal,
    setDiet,
    toggleAllergen,
    toggleDislikedIngredient,
  } = useMealPlanner();

  const [isPlanSettingsOpen, setIsPlanSettingsOpen] = useState(false);
  const [isDietOpen, setIsDietOpen] = useState(true);

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

  // 7 chế độ ăn tinh gọn (Minimalist SVG illustrations)
  const dietsConfig: {
    id: DietType;
    title: string;
    sub: string;
    bgColor: string;
    illustration: React.ReactNode;
  }[] = [
    {
      id: 'none',
      title: 'Ăn đa dạng',
      sub: 'Thưởng thức mọi món',
      bgColor: 'bg-[#FCE8E6]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <ellipse cx="20" cy="20" rx="14" ry="10" fill="#F87171" fillOpacity="0.3" />
          <path d="M12 18C12 14 16 12 20 12C24 12 28 14 28 18C28 24 20 28 12 18Z" fill="#DC2626" />
          <path d="M16 16C18 16 19 18 21 18" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      ),
    },
    {
      id: 'vegan',
      title: 'Ăn chay thuần',
      sub: 'Chỉ từ thực vật',
      bgColor: 'bg-[#FEEFD9]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <circle cx="20" cy="20" r="12" fill="#F59E0B" fillOpacity="0.25" />
          <path d="M20 12C20 12 27 15 27 22C27 26 24 28 20 28C16 28 13 26 13 22C13 15 20 12 20 12Z" fill="#D97706" />
          <path d="M20 12V28" stroke="white" strokeWidth="1.5" />
        </svg>
      ),
    },
    {
      id: 'vegetarian',
      title: 'Ăn chay',
      sub: 'Không thịt cá',
      bgColor: 'bg-[#EBF7EE]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <path d="M20 10C24 10 28 14 28 19C28 25 20 30 20 30C20 30 12 25 12 19C12 14 16 10 20 10Z" fill="#10B981" />
          <path d="M20 10V26" stroke="white" strokeWidth="1.5" />
          <path d="M20 16L24 14" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
          <path d="M20 20L16 18" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      ),
    },
    {
      id: 'pescatarian',
      title: 'Ăn chay kèm cá',
      sub: 'Rau củ và hải sản',
      bgColor: 'bg-[#FDE8E8]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <path d="M12 20C16 14 26 14 30 20C26 26 16 26 12 20Z" fill="#F43F5E" />
          <path d="M12 20L8 16V24L12 20Z" fill="#BE123C" />
          <circle cx="25" cy="19" r="1.5" fill="white" />
        </svg>
      ),
    },
    {
      id: 'paleo',
      title: 'Chế độ Paleo',
      sub: 'Ăn kiểu nguyên thủy',
      bgColor: 'bg-[#FEF9E7]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <circle cx="18" cy="20" r="10" fill="#FBBF24" fillOpacity="0.4" />
          <circle cx="18" cy="20" r="5" fill="#F59E0B" />
          <ellipse cx="26" cy="24" rx="5" ry="4" fill="#84CC16" />
        </svg>
      ),
    },
    {
      id: 'low_carb',
      title: 'Ít tinh bột',
      sub: 'Hạn chế cơm, bún, bánh mì',
      bgColor: 'bg-[#F5EFE6]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <ellipse cx="20" cy="20" rx="9" ry="12" fill="#65A30D" />
          <ellipse cx="20" cy="20" rx="7" ry="9" fill="#A3E635" />
          <circle cx="20" cy="22" r="3.5" fill="#78350F" />
        </svg>
      ),
    },
    {
      id: 'keto',
      title: 'Keto',
      sub: 'Giàu béo tốt, rất ít carb',
      bgColor: 'bg-[#EBF5FB]',
      illustration: (
        <svg className="w-8 h-8" viewBox="0 0 40 40" fill="none">
          <path d="M10 26L16 14L30 18L26 28L10 26Z" fill="#FBBF24" />
          <circle cx="18" cy="22" r="2" fill="#D97706" />
          <circle cx="24" cy="24" r="1.5" fill="#D97706" />
        </svg>
      ),
    },
  ];

  // Danh sách dị ứng thực phẩm (Pills tối giản - Không emoji)
  const allergensList: { id: AllergenType; label: string }[] = [
    { id: 'gluten', label: 'Gluten' },
    { id: 'dairy', label: 'Sữa bò (Dairy)' },
    { id: 'egg', label: 'Trứng (Egg)' },
    { id: 'soy', label: 'Đậu nành (Soy)' },
    { id: 'peanut', label: 'Đậu phộng (Peanut)' },
    { id: 'treenut', label: 'Hạt cây (Tree Nut)' },
    { id: 'fish', label: 'Cá (Fish)' },
    { id: 'shellfish', label: 'Hải sản vỏ cứng (Shellfish)' },
  ];

  // Danh sách nguyên liệu không thích (Icon badge tròn tối giản)
  const dislikedList: { id: string; label: string; color: string; dot: string }[] = [
    { id: 'Mushroom', label: 'Nấm', color: 'bg-stone-200 text-stone-700', dot: '🍄' },
    { id: 'Celery', label: 'Cần tây', color: 'bg-emerald-100 text-emerald-800', dot: '🥬' },
    { id: 'Brussels Sprouts', label: 'Bắp cải tí hon', color: 'bg-green-100 text-green-800', dot: '🥦' },
    { id: 'Broccoli', label: 'Súp lơ xanh', color: 'bg-emerald-100 text-emerald-800', dot: '🥦' },
    { id: 'Tofu', label: 'Đậu phụ', color: 'bg-amber-100 text-amber-800', dot: '🧈' },
    { id: 'Avocado', label: 'Quả bơ', color: 'bg-lime-100 text-lime-800', dot: '🥑' },
    { id: 'Beet', label: 'Củ dền', color: 'bg-rose-100 text-rose-800', dot: '🥕' },
    { id: 'Olives', label: 'Quả ô-liu', color: 'bg-lime-100 text-lime-800', dot: '🫒' },
    { id: 'Cilantro', label: 'Ngò rí (Rau mùi)', color: 'bg-emerald-100 text-emerald-800', dot: '🌿' },
    { id: 'Eggplant', label: 'Cà tím', color: 'bg-purple-100 text-purple-800', dot: '🍆' },
    { id: 'Tomato', label: 'Cà chua', color: 'bg-red-100 text-red-800', dot: '🍅' },
    { id: 'Cheese', label: 'Phô mai', color: 'bg-amber-100 text-amber-800', dot: '🧀' },
    { id: 'Cauliflower', label: 'Súp lơ trắng', color: 'bg-yellow-100 text-yellow-800', dot: '🥦' },
    { id: 'Onion', label: 'Hành tây', color: 'bg-stone-200 text-stone-700', dot: '🧅' },
    { id: 'Beef', label: 'Thịt bò', color: 'bg-rose-100 text-rose-800', dot: '🥩' },
    { id: 'Lamb', label: 'Thịt cừu', color: 'bg-rose-100 text-rose-800', dot: '🍖' },
    { id: 'Pork', label: 'Thịt heo', color: 'bg-rose-100 text-rose-800', dot: '🥓' },
    { id: 'Chicken', label: 'Thịt gà', color: 'bg-amber-100 text-amber-800', dot: '🍗' },
    { id: 'Shrimp', label: 'Tôm', color: 'bg-orange-100 text-orange-800', dot: '🍤' },
    { id: 'BitterMelon', label: 'Mướp đắng / Khổ qua', color: 'bg-green-100 text-green-800', dot: '🥒' },
    { id: 'Chili', label: 'Ớt cay', color: 'bg-red-100 text-red-800', dot: '🌶️' },
  ];

  return (
    <div className="min-h-screen bg-white text-[#27211D] pt-10 pb-28 text-left selection:bg-teal-100 selection:text-teal-900">
      <div className="max-w-3xl mx-auto px-4 sm:px-6">
        
        {/* Tiêu đề căn giữa (Minimalist) */}
        <div className="text-center mb-10">
          <h1 className="text-3xl sm:text-4xl font-extrabold text-stone-900 tracking-tight">
            Cá Nhân Hóa Thực Đơn Của Bạn
          </h1>
          <p className="text-sm text-stone-500 mt-2 font-normal max-w-lg mx-auto">
            Chia sẻ một chút về khẩu vị của gia đình để Bếp chọn những món ăn phù hợp nhất.
          </p>
        </div>

        {/* Cấu trúc Accordion 2 phần */}
        <div className="space-y-4">
          
          {/* ====================================================
              PHẦN 1: CÀI ĐẶT KẾ HOẠCH (PLAN SETTINGS)
             ==================================================== */}
          <div className="bg-white border-b border-stone-200 pb-5">
            <button
              type="button"
              onClick={() => setIsPlanSettingsOpen(!isPlanSettingsOpen)}
              className="w-full py-3 flex items-center justify-between text-left cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-full border border-stone-400 text-stone-800 flex items-center justify-center font-bold text-xs">
                  1
                </div>
                <h2 className="text-base sm:text-lg font-bold text-stone-900 group-hover:text-[#014043] transition-colors">
                  Cài Đặt Kế Hoạch (Plan Settings)
                </h2>
              </div>
              <div className="text-stone-400 group-hover:text-stone-700 transition-colors">
                {isPlanSettingsOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
              </div>
            </button>

            {isPlanSettingsOpen && (
              <div className="pt-4 pb-4 px-2 space-y-6">
                {/* 1. Ngày nấu */}
                <div>
                  <label className="block text-xs font-bold text-stone-900 mb-2">
                    Bạn cần nấu những ngày nào trong tuần?
                  </label>
                  <div className="grid grid-cols-7 gap-1.5 sm:gap-2 max-w-sm">
                    {daysList.map((d) => {
                      const isSelected = preferences.selectedDays.includes(d.key);
                      return (
                        <button
                          key={d.key}
                          type="button"
                          onClick={() => toggleDay(d.key)}
                          className={`w-9 h-9 sm:w-10 sm:h-10 rounded-full font-bold text-xs sm:text-sm flex items-center justify-center transition-all cursor-pointer ${
                            isSelected
                              ? 'bg-[#014043] text-white shadow-xs'
                              : 'border border-stone-200 text-stone-600 hover:border-stone-400 bg-white'
                          }`}
                        >
                          {d.label}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* 2. Khẩu phần */}
                <div>
                  <label className="block text-xs font-bold text-stone-900 mb-1.5">
                    Khẩu phần ăn:
                  </label>
                  <div className="relative inline-block w-36">
                    <Users className="w-4 h-4 text-stone-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <select
                      value={preferences.servingSize}
                      onChange={(e) => setServingSize(Number(e.target.value) as ServingSize)}
                      className="w-full appearance-none bg-white hover:bg-stone-50 border border-stone-200 rounded-xl pl-9 pr-7 py-2 text-xs font-semibold text-stone-800 outline-none focus:border-[#014043] cursor-pointer shadow-2xs"
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

                {/* 3. Mục tiêu */}
                <div>
                  <label className="block text-xs font-bold text-stone-900 mb-2">
                    Mục tiêu quan trọng nhất với bạn:
                  </label>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                    <button
                      type="button"
                      onClick={() => setPriorityGoal('quick_easy')}
                      className={`p-3 rounded-xl border text-left text-xs transition-all cursor-pointer flex items-center gap-2.5 ${
                        preferences.priorityGoal === 'quick_easy'
                          ? 'border-[#014043] bg-teal-50/40 font-bold text-[#014043] ring-1 ring-[#014043]'
                          : 'border-stone-200 text-stone-700 hover:bg-stone-50 bg-white'
                      }`}
                    >
                      <Timer className="w-4 h-4 shrink-0" />
                      <span>Nhanh & Tiện lợi</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setPriorityGoal('budget_friendly')}
                      className={`p-3 rounded-xl border text-left text-xs transition-all cursor-pointer flex items-center gap-2.5 ${
                        preferences.priorityGoal === 'budget_friendly'
                          ? 'border-[#014043] bg-teal-50/40 font-bold text-[#014043] ring-1 ring-[#014043]'
                          : 'border-stone-200 text-stone-700 hover:bg-stone-50 bg-white'
                      }`}
                    >
                      <Wallet className="w-4 h-4 shrink-0" />
                      <span>Tiết kiệm chi phí</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setPriorityGoal('eat_healthy')}
                      className={`p-3 rounded-xl border text-left text-xs transition-all cursor-pointer flex items-center gap-2.5 ${
                        preferences.priorityGoal === 'eat_healthy'
                          ? 'border-[#014043] bg-teal-50/40 font-bold text-[#014043] ring-1 ring-[#014043]'
                          : 'border-stone-200 text-stone-700 hover:bg-stone-50 bg-white'
                      }`}
                    >
                      <Leaf className="w-4 h-4 shrink-0" />
                      <span>Ăn lành mạnh</span>
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* ====================================================
              PHẦN 2: CHẾ ĐỘ ĂN & LỐI SỐNG (DIET & LIFESTYLE)
             ==================================================== */}
          <div className="bg-white pt-2 pb-6">
            <button
              type="button"
              onClick={() => setIsDietOpen(!isDietOpen)}
              className="w-full py-3 flex items-center justify-between text-left cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-full border border-stone-400 text-stone-800 flex items-center justify-center font-bold text-xs">
                  2
                </div>
                <h2 className="text-base sm:text-lg font-bold text-stone-900 group-hover:text-[#014043] transition-colors">
                  Chế Độ Ăn & Lối Sống (Diet & Lifestyle)
                </h2>
              </div>
              <div className="text-stone-400 group-hover:text-stone-700 transition-colors">
                {isDietOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
              </div>
            </button>

            {isDietOpen && (
              <div className="space-y-8 pt-5">
                
                {/* 1. Chế độ ăn (Diets) */}
                <div>
                  <h3 className="text-sm font-bold text-stone-900 mb-1">
                    Bạn có theo chế độ ăn nào dưới đây không?
                  </h3>
                  <p className="text-xs text-stone-500 mb-4">
                    Bếp sẽ chỉ hiển thị những món ăn phù hợp với chế độ ăn này.
                  </p>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                    {dietsConfig.map((diet) => {
                      const isSelected = preferences.diet === diet.id;
                      return (
                        <div
                          key={diet.id}
                          onClick={() => setDiet(diet.id)}
                          className={`p-3.5 rounded-2xl border transition-all flex items-center gap-3.5 cursor-pointer ${
                            isSelected
                              ? 'border-2 border-[#014043] bg-teal-50/20 shadow-2xs'
                              : 'border-stone-200 hover:border-stone-300 bg-white'
                          }`}
                        >
                          <div
                            className={`w-12 h-12 rounded-xl ${diet.bgColor} flex items-center justify-center shrink-0 shadow-2xs`}
                          >
                            {diet.illustration}
                          </div>

                          <div className="flex-1 min-w-0">
                            <div className="text-sm font-bold text-stone-900 leading-snug">
                              {diet.title}
                            </div>
                            <div className="text-xs text-stone-500 mt-0.5 truncate">
                              {diet.sub}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* 2. Dị ứng thực phẩm (Allergens) */}
                <div>
                  <h3 className="text-sm font-bold text-stone-900 mb-1">
                    Bạn có dị ứng hay kiêng kỵ nguyên liệu nào không?
                  </h3>
                  <p className="text-xs text-stone-500 mb-3">
                    Chọn các mục cần tránh (chọn nhiều).
                  </p>

                  <div className="flex flex-wrap gap-2">
                    {allergensList.map((allergen) => {
                      const isSelected = preferences.allergies.includes(allergen.id);
                      return (
                        <button
                          key={allergen.id}
                          type="button"
                          onClick={() => toggleAllergen(allergen.id)}
                          className={`px-4 py-2 rounded-full text-xs font-semibold border transition-all cursor-pointer ${
                            isSelected
                              ? 'border-2 border-[#014043] bg-[#014043] text-white shadow-2xs'
                              : 'border-stone-200 bg-white text-stone-700 hover:border-stone-400'
                          }`}
                        >
                          {allergen.label}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* 3. Nguyên liệu không thích (Disliked Ingredients) */}
                <div>
                  <h3 className="text-sm font-bold text-stone-900 mb-1">
                    Bạn không thích những nguyên liệu nào?
                  </h3>
                  <p className="text-xs text-stone-500 mb-3">
                    Chọn các nguyên liệu để loại trừ khỏi thực đơn gợi ý.
                  </p>

                  <div className="flex flex-wrap gap-2">
                    {dislikedList.map((item) => {
                      const isSelected = preferences.dislikedIngredients.includes(item.label);
                      return (
                        <button
                          key={item.id}
                          type="button"
                          onClick={() => toggleDislikedIngredient(item.label)}
                          className={`py-1 pl-1.5 pr-3.5 rounded-full text-xs font-medium border transition-all cursor-pointer flex items-center gap-2 ${
                            isSelected
                              ? 'border-2 border-[#014043] bg-teal-50/50 text-[#014043] font-bold shadow-2xs'
                              : 'border-stone-200 bg-white text-stone-700 hover:border-stone-300'
                          }`}
                        >
                          <span className={`w-5 h-5 rounded-full ${item.color} flex items-center justify-center text-[10px] shrink-0`}>
                            {item.dot}
                          </span>
                          <span>{item.label}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Nút SAVE & CONTINUE (Tối giản ở giữa trang, khớp với hình mẫu) */}
                <div className="pt-8 text-center">
                  <button
                    type="button"
                    disabled={preferences.selectedDays.length === 0}
                    onClick={onSaveAndContinue}
                    className="py-3 px-14 rounded-full bg-[#014043] hover:bg-[#002f32] active:scale-[0.99] text-white font-bold text-xs sm:text-sm tracking-wider uppercase transition-all shadow-sm cursor-pointer"
                  >
                    LƯU & TIẾP TỤC
                  </button>
                </div>

              </div>
            )}
          </div>

        </div>

      </div>
    </div>
  );
};
