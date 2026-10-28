import React, { useState } from 'react';
import {
  ChevronDown,
  ChevronUp,
  Check,
  Calendar,
  Users,
  Sparkles,
  ArrowRight,
  ArrowLeft,
  X,
  ChefHat,
  ShieldAlert,
  Flame,
} from 'lucide-react';
import { useMealPlanner } from '../../context/MealPlannerContext';
import {
  DAYS_OF_WEEK,
  GOAL_OPTIONS,
} from '../../data/mealPlannerMockData';
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

  // 7 chế độ ăn
  const dietsConfig: {
    id: DietType;
    title: string;
    sub: string;
    bgColor: string;
    illustrationEmoji: string;
  }[] = [
    {
      id: 'none',
      title: 'Ăn Mọi Thứ (None)',
      sub: 'Tự do & đa dạng món ăn',
      bgColor: 'bg-[#FCE8E6]',
      illustrationEmoji: '🥩🌿',
    },
    {
      id: 'vegan',
      title: 'Thuần Chay (Vegan)',
      sub: '100% nguồn gốc thực vật',
      bgColor: 'bg-[#FEEFD9]',
      illustrationEmoji: '🧅🧄',
    },
    {
      id: 'vegetarian',
      title: 'Ăn Chay (Vegetarian)',
      sub: 'Không thịt, có trứng & sữa',
      bgColor: 'bg-[#EBF7EE]',
      illustrationEmoji: '🥬🥦',
    },
    {
      id: 'pescatarian',
      title: 'Ăn Cá (Pescatarian)',
      sub: 'Cá, hải sản & rau củ tươi',
      bgColor: 'bg-[#FDE8E8]',
      illustrationEmoji: '🐟🍋',
    },
    {
      id: 'paleo',
      title: 'Chế Độ Paleo',
      sub: 'Nguyên thủy, không chế biến sẵn',
      bgColor: 'bg-[#FEF9E7]',
      illustrationEmoji: '🍳🥑',
    },
    {
      id: 'low_carb',
      title: 'Ít Tinh Bột (Low-Carb)',
      sub: 'Cắt giảm cơm trắng & đường',
      bgColor: 'bg-[#F5EFE6]',
      illustrationEmoji: '🥑🥗',
    },
    {
      id: 'keto',
      title: 'Keto Năng Lượng',
      sub: 'Giàu chất béo tốt & đạm',
      bgColor: 'bg-[#EBF5FB]',
      illustrationEmoji: '🧀🫐',
    },
  ];

  // Dị ứng thực phẩm
  const allergensList: { id: AllergenType; label: string; icon: string }[] = [
    { id: 'gluten', label: 'Gluten (Bột mì/Lúa mì)', icon: '🌾' },
    { id: 'dairy', label: 'Sữa & Chế phẩm sữa', icon: '🥛' },
    { id: 'egg', label: 'Trứng gà, vịt', icon: '🥚' },
    { id: 'soy', label: 'Đậu nành & chế phẩm', icon: '🫘' },
    { id: 'peanut', label: 'Đậu phộng (Lạc)', icon: '🥜' },
    { id: 'treenut', label: 'Hạt cây (Óc chó, điều...)', icon: '🌰' },
    { id: 'fish', label: 'Cá biển & cá đồng', icon: '🐟' },
    { id: 'shellfish', label: 'Hải sản có vỏ (Tôm, Cua, Sò)', icon: '🦐' },
  ];

  // Thực phẩm không thích / Kiêng khem
  const dislikedList: { id: string; label: string; icon: string; bg: string }[] = [
    { id: 'Mushroom', label: 'Nấm các loại', icon: '🍄', bg: 'bg-[#EFEBE9]' },
    { id: 'Celery', label: 'Cần tây', icon: '🥬', bg: 'bg-[#E8F5E9]' },
    { id: 'Brussels Sprouts', label: 'Bắp cải tí hon', icon: '🥦', bg: 'bg-[#E8F5E9]' },
    { id: 'Broccoli', label: 'Bông cải xanh', icon: '🥦', bg: 'bg-[#E8F5E9]' },
    { id: 'Tofu', label: 'Đậu phụ', icon: '🧈', bg: 'bg-[#FFF8E1]' },
    { id: 'Avocado', label: 'Quả bơ', icon: '🥑', bg: 'bg-[#E8F5E9]' },
    { id: 'Beet', label: 'Củ dền đỏ', icon: '🥕', bg: 'bg-[#FCE4EC]' },
    { id: 'Olives', label: 'Quả ô-liu', icon: '🫒', bg: 'bg-[#F1F8E9]' },
    { id: 'Cilantro', label: 'Ngò rí (Rau mùi)', icon: '🌿', bg: 'bg-[#E8F5E9]' },
    { id: 'Eggplant', label: 'Cà tím', icon: '🍆', bg: 'bg-[#EDE7F6]' },
    { id: 'Tomato', label: 'Cà chua', icon: '🍅', bg: 'bg-[#FFEBEE]' },
    { id: 'Cheese', label: 'Phô mai', icon: '🧀', bg: 'bg-[#FFFDE7]' },
    { id: 'Cauliflower', label: 'Súp lơ trắng', icon: '🥦', bg: 'bg-[#FFFDE7]' },
    { id: 'Onion', label: 'Hành tây', icon: '🧅', bg: 'bg-[#EFEBE9]' },
    { id: 'Beef', label: 'Thịt bò', icon: '🥩', bg: 'bg-[#FFEBEE]' },
    { id: 'Lamb', label: 'Thịt cừu', icon: '🍖', bg: 'bg-[#FFEBEE]' },
    { id: 'Pork', label: 'Thịt heo', icon: '🥓', bg: 'bg-[#FFEBEE]' },
    { id: 'Chicken', label: 'Thịt gà', icon: '🍗', bg: 'bg-[#FCE4EC]' },
    { id: 'Shrimp', label: 'Tôm & tép', icon: '🍤', bg: 'bg-[#FFF3E0]' },
  ];

  return (
    <div className="min-h-screen bg-white text-[#27211D] pb-32 pt-8 text-left">
      <div className="max-w-3xl mx-auto px-4 sm:px-6">
        
        {/* Tiêu đề căn giữa */}
        <div className="text-center mb-10">
          <span className="text-xs font-bold text-orange-600 tracking-widest uppercase">
            BƯỚC 2: KHẢO SÁT KHẨU VỊ GIA ĐÌNH
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-stone-900 tracking-tight font-serif-display mt-1">
            Cá Nhân Hóa Thực Đơn Cho Gia Đình Bạn
          </h1>
          <p className="text-sm text-stone-500 mt-2 font-normal max-w-xl mx-auto">
            Hãy chia sẻ với Bếp về sở thích ăn uống và các hạn chế dinh dưỡng để Bếp lựa chọn mâm cơm phù hợp nhất cho cả nhà.
          </p>
        </div>

        {/* Accordion */}
        <div className="space-y-4">
          
          {/* (1) CÀI ĐẶT KẾ HOẠCH (PLAN SETTINGS) */}
          <div className="bg-white border-b border-gray-200 pb-4">
            <button
              type="button"
              onClick={() => setIsPlanSettingsOpen(!isPlanSettingsOpen)}
              className="w-full py-4 flex items-center justify-between text-left cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full border-2 border-stone-400 text-stone-700 flex items-center justify-center font-bold text-sm">
                  1
                </div>
                <div>
                  <h2 className="text-lg sm:text-xl font-bold font-serif-display text-stone-900">
                    Phần 1: Cài Đặt Kế Hoạch (Plan Settings)
                  </h2>
                  <p className="text-xs text-stone-500">
                    Số ngày nấu: {preferences.selectedDays.length} ngày • Khẩu phần: {preferences.servingSize} người
                  </p>
                </div>
              </div>
              <div className="p-1 text-stone-500 group-hover:text-stone-800">
                {isPlanSettingsOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
              </div>
            </button>

            {isPlanSettingsOpen && (
              <div className="pt-4 pb-4 px-2 space-y-5 animate-fadeIn">
                {/* Ngày nấu */}
                <div>
                  <div className="text-xs font-bold text-stone-800 mb-2">Số ngày bạn dự định nấu trong tuần:</div>
                  <div className="flex gap-2 flex-wrap">
                    {DAYS_OF_WEEK.map((d) => {
                      const isSelected = preferences.selectedDays.includes(d.key);
                      return (
                        <button
                          key={d.key}
                          type="button"
                          onClick={() => toggleDay(d.key)}
                          className={`w-9 h-9 rounded-full text-xs font-bold cursor-pointer transition-colors ${
                            isSelected ? 'bg-[#014043] text-white shadow-xs' : 'bg-stone-100 text-stone-600'
                          }`}
                        >
                          {d.code}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Khẩu phần */}
                <div>
                  <div className="text-xs font-bold text-stone-800 mb-1.5">Khẩu phần ăn:</div>
                  <select
                    value={preferences.servingSize}
                    onChange={(e) => setServingSize(Number(e.target.value) as ServingSize)}
                    className="bg-white border border-gray-300 rounded-full px-4 py-2 text-xs font-bold text-stone-800 outline-none"
                  >
                    <option value={1}>1 người (Độc thân)</option>
                    <option value={2}>2 người (Cặp đôi)</option>
                    <option value={3}>3 người (Gia đình nhỏ)</option>
                    <option value={4}>4 người (Gia đình tiêu chuẩn)</option>
                    <option value={5}>5+ người (Gia đình lớn)</option>
                  </select>
                </div>

                {/* Mục tiêu */}
                <div>
                  <div className="text-xs font-bold text-stone-800 mb-2">Mục tiêu ưu tiên:</div>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                    {GOAL_OPTIONS.map((g) => (
                      <button
                        key={g.id}
                        type="button"
                        onClick={() => setPriorityGoal(g.id)}
                        className={`p-3 rounded-xl border text-left text-xs transition-colors cursor-pointer ${
                          preferences.priorityGoal === g.id
                            ? 'border-orange-500 bg-orange-50/70 font-bold text-orange-950'
                            : 'border-gray-200 text-stone-600 hover:bg-stone-50'
                        }`}
                      >
                        {g.title}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* (2) CHẾ ĐỘ ĂN & LỐI SỐNG (DIET & LIFESTYLE) */}
          <div className="bg-white border-b border-gray-200 pb-8">
            <button
              type="button"
              onClick={() => setIsDietOpen(!isDietOpen)}
              className="w-full py-4 flex items-center justify-between text-left cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full border-2 border-stone-400 text-stone-700 flex items-center justify-center font-bold text-sm">
                  2
                </div>
                <div>
                  <h2 className="text-lg sm:text-xl font-bold font-serif-display text-stone-900">
                    Phần 2: Chế Độ Ăn & Lối Sống (Diet & Lifestyle)
                  </h2>
                  <p className="text-xs text-stone-500">
                    Chọn chế độ ăn, nhóm dị ứng và nguyên liệu kiêng khem
                  </p>
                </div>
              </div>
              <div className="p-1 text-stone-500 group-hover:text-stone-800">
                {isDietOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
              </div>
            </button>

            {isDietOpen && (
              <div className="space-y-8 pt-3 animate-fadeIn">
                
                {/* 1. Chế độ ăn */}
                <div>
                  <h3 className="text-sm font-bold text-stone-900 mb-1">
                    Gia đình bạn có theo chế độ ăn nào dưới đây không? (Chọn 1)
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
                              ? 'border-2 border-orange-500 bg-orange-50/30 shadow-xs'
                              : 'border-gray-200 hover:border-gray-300 bg-white'
                          }`}
                        >
                          <div
                            className={`w-14 h-14 rounded-2xl ${diet.bgColor} flex items-center justify-center text-2xl shrink-0 shadow-xs`}
                          >
                            <span>{diet.illustrationEmoji}</span>
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

                {/* 2. Dị ứng thực phẩm */}
                <div>
                  <h3 className="text-sm font-bold text-stone-900 mb-1">
                    Có ai trong gia đình bị dị ứng hoặc không dung nạp thành phần nào không?
                  </h3>
                  <p className="text-xs text-stone-500 mb-3">
                    Chọn các nhóm cần loại trừ tuyệt đối khỏi thực đơn.
                  </p>

                  <div className="flex flex-wrap gap-2.5">
                    {allergensList.map((allergen) => {
                      const isSelected = preferences.allergies.includes(allergen.id);
                      return (
                        <button
                          key={allergen.id}
                          type="button"
                          onClick={() => toggleAllergen(allergen.id)}
                          className={`px-3.5 py-2 rounded-full text-xs font-semibold border transition-all cursor-pointer flex items-center gap-1.5 ${
                            isSelected
                              ? 'bg-red-50 text-red-900 border-red-500 shadow-xs ring-1 ring-red-400'
                              : 'bg-white text-stone-700 border-gray-300 hover:border-gray-400'
                          }`}
                        >
                          <span>{allergen.icon}</span>
                          <span>{allergen.label}</span>
                          {isSelected && <Check className="w-3.5 h-3.5 text-red-700" />}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* 3. Nguyên liệu không thích */}
                <div>
                  <h3 className="text-sm font-bold text-stone-900 mb-1">
                    Những nguyên liệu nào bạn không thích hoặc muốn loại trừ?
                  </h3>
                  <p className="text-xs text-stone-500 mb-3">
                    Bếp sẽ loại bỏ các món chứa những nguyên liệu này.
                  </p>

                  <div className="flex flex-wrap gap-2">
                    {dislikedList.map((item) => {
                      const isSelected = preferences.dislikedIngredients.includes(item.label);
                      return (
                        <button
                          key={item.id}
                          type="button"
                          onClick={() => toggleDislikedIngredient(item.label)}
                          className={`px-3 py-1.5 rounded-full text-xs font-semibold border transition-all cursor-pointer flex items-center gap-1.5 ${
                            isSelected
                              ? 'bg-amber-100 text-amber-950 border-amber-500 shadow-xs font-bold'
                              : 'bg-white text-stone-700 border-gray-200 hover:border-gray-300'
                          }`}
                        >
                          <span className={`w-5 h-5 rounded-full ${item.bg} flex items-center justify-center text-[11px] shrink-0`}>
                            {item.icon}
                          </span>
                          <span>{item.label}</span>
                          {isSelected && <X className="w-3.5 h-3.5 text-amber-800" />}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Nút SAVE & CONTINUE */}
                <div className="pt-6">
                  <button
                    type="button"
                    onClick={onSaveAndContinue}
                    className="w-full py-4 rounded-full bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 text-white font-extrabold text-xs sm:text-sm tracking-widest uppercase transition-all shadow-md shadow-orange-600/20 active:scale-[0.99] cursor-pointer text-center"
                  >
                    LƯU & TIẾP TỤC (SAVE & CONTINUE)
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
