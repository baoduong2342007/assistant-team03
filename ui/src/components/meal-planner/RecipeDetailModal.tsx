import React from 'react';
import {
  X,
  Clock,
  DollarSign,
  Star,
  Flame,
  ChefHat,
  CheckCircle2,
  Utensils,
  Share2,
} from 'lucide-react';
import { PlannerRecipe } from '../../types/mealPlanner';
import { useMealPlanner } from '../../context/MealPlannerContext';

interface RecipeDetailModalProps {
  recipe: PlannerRecipe | null;
  onClose: () => void;
}

export const RecipeDetailModal: React.FC<RecipeDetailModalProps> = ({ recipe, onClose }) => {
  const { formatPrice, preferences } = useMealPlanner();

  if (!recipe) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden border border-stone-200">
        
        {/* Header Image */}
        <div className="relative aspect-16/9 overflow-hidden bg-stone-900 shrink-0">
          <img
            src={recipe.image}
            alt={recipe.title}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

          {/* Close button */}
          <button
            type="button"
            onClick={onClose}
            className="absolute top-4 right-4 w-9 h-9 rounded-full bg-black/50 hover:bg-black/80 text-white flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>

          {/* Badges on image */}
          <div className="absolute bottom-4 left-4 right-4 text-white">
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              <span className="px-2.5 py-1 rounded-full bg-orange-600 text-white text-xs font-bold flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                <span>{recipe.cookTime} phút nấu</span>
              </span>
              <span className="px-2.5 py-1 rounded-full bg-emerald-600 text-white text-xs font-bold">
                {formatPrice(recipe.costPerServing)}/suất
              </span>
              <span className="px-2.5 py-1 rounded-full bg-white/20 backdrop-blur-xs text-white text-xs font-semibold flex items-center gap-1">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                <span>{recipe.rating} ({recipe.reviewCount} đánh giá)</span>
              </span>
            </div>

            <h2 className="text-xl sm:text-2xl font-bold font-serif-display drop-shadow-md">
              {recipe.title}
            </h2>
          </div>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          
          {/* Description */}
          <p className="text-stone-600 text-xs sm:text-sm leading-relaxed">
            {recipe.description}
          </p>

          {/* Macro Nutrition Grid */}
          <div className="grid grid-cols-4 gap-2 bg-stone-50 p-4 rounded-2xl border border-stone-200/80 text-center">
            <div>
              <div className="text-[11px] text-stone-500 font-medium">Năng lượng</div>
              <div className="text-base sm:text-lg font-bold text-orange-600">{recipe.calories} kcal</div>
            </div>
            <div>
              <div className="text-[11px] text-stone-500 font-medium">Chất đạm (P)</div>
              <div className="text-base sm:text-lg font-bold text-stone-800">{recipe.protein}g</div>
            </div>
            <div>
              <div className="text-[11px] text-stone-500 font-medium">Tinh bột (C)</div>
              <div className="text-base sm:text-lg font-bold text-stone-800">{recipe.carbs}g</div>
            </div>
            <div>
              <div className="text-[11px] text-stone-500 font-medium">Chất béo (F)</div>
              <div className="text-base sm:text-lg font-bold text-stone-800">{recipe.fat}g</div>
            </div>
          </div>

          {/* Ingredients Section */}
          <div>
            <h3 className="text-sm font-bold text-stone-900 mb-3 flex items-center gap-2">
              <Utensils className="w-4 h-4 text-orange-600" />
              <span>Nguyên liệu chuẩn bị (Định lượng cho {preferences.servingSize} người):</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {recipe.ingredients.map((ing, idx) => (
                <div
                  key={idx}
                  className="flex items-center justify-between p-2.5 rounded-xl bg-white border border-stone-200/80 text-xs"
                >
                  <span className="font-semibold text-stone-800">{ing.name}</span>
                  <span className="text-stone-500 font-mono">{ing.amount}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Instructions Section */}
          <div>
            <h3 className="text-sm font-bold text-stone-900 mb-3 flex items-center gap-2">
              <ChefHat className="w-4 h-4 text-orange-600" />
              <span>Các bước thực hiện:</span>
            </h3>

            <div className="space-y-3">
              {recipe.instructions.map((step, idx) => (
                <div key={idx} className="flex gap-3 text-xs sm:text-sm text-stone-700">
                  <div className="w-6 h-6 rounded-full bg-orange-100 text-orange-700 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">
                    {idx + 1}
                  </div>
                  <div className="flex-1 leading-relaxed">
                    {step}
                  </div>
                </div>
              ))}
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="p-4 sm:p-5 border-t border-stone-100 bg-stone-50 flex items-center justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-6 py-2 rounded-full bg-[#014043] hover:bg-[#002f32] text-white text-xs sm:text-sm font-bold cursor-pointer"
          >
            Đóng
          </button>
        </div>

      </div>
    </div>
  );
};
