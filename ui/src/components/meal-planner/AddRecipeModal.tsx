import React, { useState } from 'react';
import {
  X,
  Search,
  Plus,
  Clock,
  DollarSign,
  Star,
  ChefHat,
  Filter,
  Check,
} from 'lucide-react';
import { useMealPlanner } from '../../context/MealPlannerContext';
import { DayOfWeek, PlannerRecipe } from '../../types/mealPlanner';
import { DAYS_OF_WEEK } from '../../data/mealPlannerMockData';

interface AddRecipeModalProps {
  isOpen: boolean;
  targetDay: DayOfWeek;
  onClose: () => void;
  onSelectRecipe: (recipe: PlannerRecipe) => void;
}

export const AddRecipeModal: React.FC<AddRecipeModalProps> = ({
  isOpen,
  targetDay,
  onClose,
  onSelectRecipe,
}) => {
  const { allRecipes, formatPrice, weeklyPlan } = useMealPlanner();

  const [activeTab, setActiveTab] = useState<'search' | 'saved' | 'create'>('search');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterDiet, setFilterDiet] = useState<string>('all');

  // Custom recipe form state
  const [customTitle, setCustomTitle] = useState('');
  const [customTime, setCustomTime] = useState(20);
  const [customPrice, setCustomPrice] = useState(40000);
  const [customImage, setCustomImage] = useState(
    'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&auto=format&fit=crop&q=80'
  );
  const [customDesc, setCustomDesc] = useState('');

  if (!isOpen) return null;

  const dayInfo = DAYS_OF_WEEK.find((d) => d.key === targetDay);

  // Filter recipes
  const filteredRecipes = allRecipes.filter((r) => {
    const matchesSearch =
      r.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.tags.some((t) => t.toLowerCase().includes(searchQuery.toLowerCase())) ||
      r.ingredients.some((i) => i.name.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesDiet = filterDiet === 'all' || r.diet.includes(filterDiet as any);

    return matchesSearch && matchesDiet;
  });

  const handleCreateCustom = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customTitle.trim()) return;

    const newRecipe: PlannerRecipe = {
      id: `custom_${Date.now()}`,
      title: customTitle.trim(),
      description: customDesc.trim() || 'Công thức do bạn tự sáng tạo.',
      image: customImage,
      cookTime: Number(customTime) || 20,
      costPerServing: Number(customPrice) || 35000,
      rating: 5.0,
      reviewCount: 1,
      calories: 380,
      protein: 25,
      carbs: 30,
      fat: 12,
      tags: ['Tự tạo', 'Món gia đình'],
      diet: ['none'],
      allergens: [],
      ingredients: [
        { name: customTitle, amount: '1 phần', category: 'meat' },
        { name: 'Gia vị tùy thích', amount: 'Vừa đủ', category: 'spice' },
      ],
      instructions: ['Sơ chế nguyên liệu sạch sẽ.', 'Nấu theo sở thích và khẩu vị riêng của gia đình.'],
    };

    onSelectRecipe(newRecipe);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-3xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden border border-stone-200">
        
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-stone-100 flex items-center justify-between bg-stone-50/50">
          <div>
            <span className="text-[11px] font-bold text-orange-600 uppercase tracking-wider">
              THÊM MÓN VÀO LỊCH TUẦN
            </span>
            <h2 className="text-xl font-bold font-serif-display text-stone-900">
              Chọn Món Cho {dayInfo?.fullLabel} ({dayInfo?.dateLabel})
            </h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Switcher */}
        <div className="flex border-b border-stone-100 px-6 pt-3 gap-6 bg-white">
          <button
            type="button"
            onClick={() => setActiveTab('search')}
            className={`pb-3 text-xs sm:text-sm font-bold border-b-2 cursor-pointer transition-all ${
              activeTab === 'search'
                ? 'border-orange-600 text-orange-600'
                : 'border-transparent text-stone-500 hover:text-stone-800'
            }`}
          >
            Tìm Kiếm Món Mới (Search New Recipe)
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('saved')}
            className={`pb-3 text-xs sm:text-sm font-bold border-b-2 cursor-pointer transition-all ${
              activeTab === 'saved'
                ? 'border-orange-600 text-orange-600'
                : 'border-transparent text-stone-500 hover:text-stone-800'
            }`}
          >
            Công Thức Đã Lưu (Add Saved Recipe)
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('create')}
            className={`pb-3 text-xs sm:text-sm font-bold border-b-2 cursor-pointer transition-all ${
              activeTab === 'create'
                ? 'border-orange-600 text-orange-600'
                : 'border-transparent text-stone-500 hover:text-stone-800'
            }`}
          >
            + Tự Tạo Món Mới (Create New Recipe)
          </button>
        </div>

        {/* Body content based on tab */}
        <div className="flex-1 overflow-y-auto p-6">
          
          {/* TAB 1 & 2: Search & Saved Recipes */}
          {(activeTab === 'search' || activeTab === 'saved') && (
            <div>
              {/* Search & Filter Bar */}
              <div className="flex flex-col sm:flex-row gap-3 mb-6">
                <div className="relative flex-1">
                  <Search className="w-4 h-4 text-stone-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Tìm tên món (Cá hồi, Gà, Canh chua, Bò...)"
                    className="w-full bg-stone-50 pl-10 pr-4 py-2.5 text-xs sm:text-sm rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none"
                  />
                </div>

                <div className="flex gap-2">
                  <select
                    value={filterDiet}
                    onChange={(e) => setFilterDiet(e.target.value)}
                    className="bg-stone-50 border border-stone-200 rounded-xl px-3 py-2 text-xs font-semibold text-stone-700 outline-none cursor-pointer"
                  >
                    <option value="all">Tất cả chế độ ăn</option>
                    <option value="none">Ăn mọi thứ</option>
                    <option value="pescatarian">Ăn cá / Hải sản</option>
                    <option value="vegetarian">Ăn chay</option>
                    <option value="low_carb">Ít tinh bột</option>
                    <option value="keto">Keto</option>
                  </select>
                </div>
              </div>

              {/* Recipe Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {filteredRecipes.map((recipe) => {
                  const isAlreadyInDay = (weeklyPlan[targetDay] || []).some((r) => r.id === recipe.id);
                  return (
                    <div
                      key={recipe.id}
                      className="flex gap-3 p-3 rounded-2xl border border-stone-200 hover:border-orange-400 bg-white hover:shadow-md transition-all group"
                    >
                      <img
                        src={recipe.image}
                        alt={recipe.title}
                        className="w-22 h-22 rounded-xl object-cover shrink-0"
                      />

                      <div className="flex-1 min-w-0 flex flex-col justify-between">
                        <div>
                          <div className="flex items-center gap-1.5 text-[11px] text-amber-500 font-bold">
                            <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                            <span>{recipe.rating}</span>
                            <span className="text-stone-400 font-normal">({recipe.cookTime}p)</span>
                          </div>
                          <h4 className="text-xs sm:text-sm font-bold text-stone-900 group-hover:text-orange-600 transition-colors line-clamp-1">
                            {recipe.title}
                          </h4>
                          <div className="text-[11px] text-stone-500 font-semibold mt-0.5">
                            {formatPrice(recipe.costPerServing)}/suất • {recipe.calories} kcal
                          </div>
                        </div>

                        <button
                          type="button"
                          onClick={() => {
                            onSelectRecipe(recipe);
                            onClose();
                          }}
                          className={`mt-2 py-1.5 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1 cursor-pointer ${
                            isAlreadyInDay
                              ? 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                              : 'bg-[#014043] hover:bg-[#002f32] text-white shadow-xs'
                          }`}
                        >
                          <Plus className="w-3.5 h-3.5" />
                          <span>{isAlreadyInDay ? 'Thêm Thêm Suất' : 'Thêm Vào Ngày Này'}</span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* TAB 3: Create Custom Recipe */}
          {activeTab === 'create' && (
            <form onSubmit={handleCreateCustom} className="space-y-4 max-w-lg mx-auto py-2">
              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">
                  Tên món ăn của bạn *
                </label>
                <input
                  type="text"
                  required
                  value={customTitle}
                  onChange={(e) => setCustomTitle(e.target.value)}
                  placeholder="Ví dụ: Thịt kho trứng cút gia truyền"
                  className="w-full bg-stone-50 px-3.5 py-2.5 text-xs sm:text-sm rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">
                    Thời gian nấu (phút)
                  </label>
                  <input
                    type="number"
                    min="5"
                    max="180"
                    value={customTime}
                    onChange={(e) => setCustomTime(Number(e.target.value))}
                    className="w-full bg-stone-50 px-3 py-2 text-xs sm:text-sm rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">
                    Giá ước tính/suất (VNĐ)
                  </label>
                  <input
                    type="number"
                    step="1000"
                    value={customPrice}
                    onChange={(e) => setCustomPrice(Number(e.target.value))}
                    className="w-full bg-stone-50 px-3 py-2 text-xs sm:text-sm rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">
                  Mô tả ngắn
                </label>
                <textarea
                  rows={2}
                  value={customDesc}
                  onChange={(e) => setCustomDesc(e.target.value)}
                  placeholder="Món ăn yêu thích của cả gia đình..."
                  className="w-full bg-stone-50 px-3.5 py-2 text-xs sm:text-sm rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">
                  Link ảnh minh họa
                </label>
                <input
                  type="url"
                  value={customImage}
                  onChange={(e) => setCustomImage(e.target.value)}
                  className="w-full bg-stone-50 px-3 py-2 text-xs rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none text-stone-500"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  className="w-full py-3 rounded-full bg-[#014043] hover:bg-[#002f32] text-white font-bold text-xs sm:text-sm shadow-sm cursor-pointer flex items-center justify-center gap-2 transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  <span>LƯU & THÊM VÀO {dayInfo?.fullLabel.toUpperCase()}</span>
                </button>
              </div>
            </form>
          )}

        </div>

      </div>
    </div>
  );
};
