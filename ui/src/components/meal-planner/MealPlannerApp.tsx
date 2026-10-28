import React, { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  useDraggable,
  useDroppable,
  PointerSensor,
  useSensor,
  useSensors,
  DragStartEvent,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  ChevronLeft,
  ChevronRight,
  RotateCw,
  X,
  Plus,
  Star,
  Clock,
  ShoppingBag,
  HelpCircle,
  ChevronUp,
  ChevronDown,
  Info,
  SlidersHorizontal,
  Sparkles,
  Printer,
} from 'lucide-react';
import { useMealPlanner } from '../../context/MealPlannerContext';
import { DAYS_OF_WEEK } from '../../data/mealPlannerMockData';
import { DayOfWeek, PlannerRecipe } from '../../types/mealPlanner';
import { GroceriesModal } from './GroceriesModal';
import { AddRecipeModal } from './AddRecipeModal';
import { RecipeDetailModal } from './RecipeDetailModal';
import { FeedbackModal } from './FeedbackModal';
import { BepNhaMinhFooter } from './BepNhaMinhFooter';

interface MealPlannerAppProps {
  onNavigateToPreferences: () => void;
}

// -------------------------------------------------------------
// THẺ MÓN ĂN KÉO THẢ (DRAGGABLE RECIPE CARD)
// -------------------------------------------------------------
interface DraggableRecipeCardProps {
  day: DayOfWeek;
  recipe: PlannerRecipe;
  onSwap: (day: DayOfWeek, recipeId: string) => void;
  onRemove: (day: DayOfWeek, recipeId: string) => void;
  onClickDetail: (recipe: PlannerRecipe) => void;
  formatPrice: (cost: number) => string;
  isSwapping: boolean;
}

const DraggableRecipeCard: React.FC<DraggableRecipeCardProps> = ({
  day,
  recipe,
  onSwap,
  onRemove,
  onClickDetail,
  formatPrice,
  isSwapping,
}) => {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `${day}::${recipe.id}`,
    data: { day, recipe },
  });

  const style: React.CSSProperties = {
    transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
    opacity: isDragging ? 0.35 : 1,
    zIndex: isDragging ? 999 : undefined,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="group bg-white rounded-2xl overflow-hidden border border-stone-200/90 shadow-xs hover:shadow-md transition-all select-none text-left"
    >
      {/* VÙNG ẢNH & ĐIỂM CHẠM KÉO THẢ */}
      <div
        className="relative aspect-16/11 overflow-hidden bg-stone-100 cursor-grab active:cursor-grabbing"
        {...listeners}
        {...attributes}
      >
        <img
          src={recipe.image}
          alt={recipe.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300 pointer-events-none"
        />

        {/* NÚT THAO TÁC TRÊN ẢNH: SWAP & XÓA */}
        <div className="absolute top-2.5 inset-x-2.5 flex items-center justify-between z-10 pointer-events-auto">
          {/* Nút SWAP (Đổi món ngẫu nhiên) */}
          <button
            type="button"
            onPointerDown={(e) => e.stopPropagation()}
            onClick={(e) => {
              e.stopPropagation();
              onSwap(day, recipe.id);
            }}
            title="Đổi món ngẫu nhiên phù hợp với sở thích gia đình"
            className="px-2.5 py-1 rounded-full bg-black/60 hover:bg-black/85 text-white text-[11px] font-bold flex items-center gap-1.5 backdrop-blur-xs transition-transform active:scale-95 cursor-pointer"
          >
            <RotateCw className={`w-3 h-3 text-amber-300 ${isSwapping ? 'animate-spin' : ''}`} />
            <span>SWAP</span>
          </button>

          {/* Nút xóa X */}
          <button
            type="button"
            onPointerDown={(e) => e.stopPropagation()}
            onClick={(e) => {
              e.stopPropagation();
              onRemove(day, recipe.id);
            }}
            title="Xóa món khỏi ngày này"
            className="w-6 h-6 rounded-full bg-black/60 hover:bg-red-600 text-white flex items-center justify-center backdrop-blur-xs transition-transform active:scale-95 cursor-pointer"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* BADGES DƯỚI ẢNH: THỜI GIAN & GIÁ THÀNH */}
        <div className="absolute bottom-2.5 inset-x-2.5 flex items-center justify-between pointer-events-none">
          <div className="px-2 py-0.5 rounded-md bg-black/60 backdrop-blur-xs text-white text-[10px] font-semibold flex items-center gap-1">
            <Clock className="w-3 h-3 text-amber-300" />
            <span>{recipe.cookTime}p • {formatPrice(recipe.costPerServing)}/suất</span>
          </div>

          <div className="w-6 h-6 rounded-full bg-orange-500 text-white flex items-center justify-center text-[10px] font-bold shadow-xs">
            🍲
          </div>
        </div>
      </div>

      {/* THÔNG TIN MÓN ĂN */}
      <div
        onClick={() => onClickDetail(recipe)}
        className="p-3 cursor-pointer hover:bg-stone-50/60 transition-colors"
      >
        <div className="flex items-center gap-1 text-amber-400 mb-1">
          {[...Array(5)].map((_, i) => (
            <Star key={i} className="w-3 h-3 fill-amber-400 text-amber-400" />
          ))}
          <span className="text-[10px] text-stone-400 ml-1">({recipe.calories} kcal)</span>
        </div>

        <h4 className="text-xs sm:text-sm font-bold text-stone-900 line-clamp-1 group-hover:text-orange-600 transition-colors">
          {recipe.title}
        </h4>
      </div>
    </div>
  );
};

// -------------------------------------------------------------
// CỘT NGÀY TRÊN LỊCH TUẦN (DROPPABLE DAY COLUMN)
// -------------------------------------------------------------
interface DayColumnProps {
  dayInfo: (typeof DAYS_OF_WEEK)[0];
  recipes: PlannerRecipe[];
  onOpenAddModal: (day: DayOfWeek) => void;
  onSwap: (day: DayOfWeek, recipeId: string) => void;
  onRemove: (day: DayOfWeek, recipeId: string) => void;
  onClickDetail: (recipe: PlannerRecipe) => void;
  formatPrice: (cost: number) => string;
  swappingCardId: string | null;
}

const DayColumn: React.FC<DayColumnProps> = ({
  dayInfo,
  recipes,
  onOpenAddModal,
  onSwap,
  onRemove,
  onClickDetail,
  formatPrice,
  swappingCardId,
}) => {
  const { setNodeRef, isOver } = useDroppable({
    id: `day::${dayInfo.key}`,
    data: { day: dayInfo.key },
  });

  return (
    <div
      ref={setNodeRef}
      className={`bg-white rounded-3xl border p-3 flex flex-col justify-between transition-all min-h-[260px] text-left ${
        isOver
          ? 'border-2 border-orange-500 bg-orange-50/30'
          : 'border-stone-200/90 shadow-xs'
      }`}
    >
      <div>
        {/* Tiêu đề ngày và nút + THÊM */}
        <div className="flex items-center justify-between mb-3 pb-2 border-b border-gray-100">
          <div>
            <span className="font-extrabold text-sm sm:text-base text-stone-900 block leading-tight">
              {dayInfo.fullLabel}
            </span>
            <span className="text-[10px] text-stone-400">{dayInfo.dateLabel}</span>
          </div>

          <button
            type="button"
            onClick={() => onOpenAddModal(dayInfo.key)}
            className="px-3 py-1 rounded-full bg-orange-100 hover:bg-orange-200 text-orange-800 text-xs font-bold transition-colors cursor-pointer"
          >
            + THÊM
          </button>
        </div>

        {/* Danh sách thẻ món ăn trong ngày */}
        <div className="space-y-3">
          {recipes.map((recipe) => (
            <DraggableRecipeCard
              key={recipe.id}
              day={dayInfo.key}
              recipe={recipe}
              onSwap={onSwap}
              onRemove={onRemove}
              onClickDetail={onClickDetail}
              formatPrice={formatPrice}
              isSwapping={swappingCardId === `${dayInfo.key}::${recipe.id}`}
            />
          ))}

          {/* Ô trống khi ngày chưa có món */}
          {recipes.length === 0 && (
            <div
              onClick={() => onOpenAddModal(dayInfo.key)}
              className="rounded-2xl border-2 border-dashed border-stone-200 hover:border-orange-500 p-6 text-center cursor-pointer transition-colors flex flex-col items-center justify-center min-h-[160px] bg-stone-50/50 hover:bg-orange-50/30"
            >
              <Plus className="w-5 h-5 text-stone-400 mb-1" />
              <span className="text-xs font-bold text-stone-500 hover:text-orange-600">
                Kéo món vào đây
              </span>
              <span className="text-[10px] text-stone-400 mt-0.5">hoặc bấm + Thêm</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// -------------------------------------------------------------
// BẢNG ĐIỀU KHIỂN KẾ HOẠCH TUẦN (MEAL PLANNER APP)
// -------------------------------------------------------------
export const MealPlannerApp: React.FC<MealPlannerAppProps> = ({ onNavigateToPreferences }) => {
  const {
    weeklyPlan,
    swapRecipe,
    removeRecipe,
    addRecipe,
    moveRecipe,
    generateWeeklyPlan,
    preferences,
    stats,
    formatPrice,
    user,
  } = useMealPlanner();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    })
  );

  const [activeDragItem, setActiveDragItem] = useState<{
    day: DayOfWeek;
    recipe: PlannerRecipe;
  } | null>(null);

  const [swappingCardId, setSwappingCardId] = useState<string | null>(null);
  const [isHowItWorksOpen, setIsHowItWorksOpen] = useState(true);
  const [isFeedbackDismissed, setIsFeedbackDismissed] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Modals
  const [isGroceriesModalOpen, setIsGroceriesModalOpen] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [targetDay, setTargetDay] = useState<DayOfWeek>('mon');
  const [detailRecipe, setDetailRecipe] = useState<PlannerRecipe | null>(null);
  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Drag handlers
  const handleDragStart = (event: DragStartEvent) => {
    const data = event.active.data.current as { day: DayOfWeek; recipe: PlannerRecipe };
    if (data) setActiveDragItem(data);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveDragItem(null);
    if (!over) return;

    const activeData = active.data.current as { day: DayOfWeek; recipe: PlannerRecipe };
    const overId = String(over.id);

    if (overId.startsWith('day::')) {
      const destDay = overId.replace('day::', '') as DayOfWeek;
      if (activeData && activeData.day !== destDay) {
        moveRecipe(activeData.day, destDay, activeData.recipe.id);
        const dayLabel = DAYS_OF_WEEK.find((d) => d.key === destDay)?.fullLabel;
        showToast(`Đã chuyển "${activeData.recipe.title}" sang ${dayLabel}!`);
      }
    }
  };

  const handleSwap = (day: DayOfWeek, currentRecipeId: string) => {
    setSwappingCardId(`${day}::${currentRecipeId}`);
    setTimeout(() => {
      const newRec = swapRecipe(day, currentRecipeId);
      setSwappingCardId(null);
      if (newRec) {
        showToast(`Đã đổi sang: "${newRec.title}"`);
      }
    }, 250);
  };

  const handleRemove = (day: DayOfWeek, recipeId: string) => {
    removeRecipe(day, recipeId);
    showToast('Đã xóa món khỏi ngày này.');
  };

  const handleOpenAddModal = (day: DayOfWeek) => {
    setTargetDay(day);
    setIsAddModalOpen(true);
  };

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className="min-h-screen bg-[#FAF8F5] text-[#27211D] flex flex-col justify-between">
        
        <div>
          {/* ====================================================
              THANH TIÊU ĐỀ TRÊN CÙNG
              Trái: "Kế Hoạch Bữa Ăn Của Tôi"
              Giữa: "< Tuần Này (28 Th10 - 03 Th11) >"
              Phải: Nút "CHỈNH SỬA SỞ THÍCH", "TẠO LẠI THỰC ĐƠN"
             ==================================================== */}
          <div className="bg-white border-b border-gray-200">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex flex-col md:flex-row md:items-center justify-between gap-3">
              
              {/* Tiêu đề */}
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-amber-600 to-orange-500 text-white flex items-center justify-center font-bold text-lg shadow-xs">
                  🍲
                </div>
                <div className="text-left">
                  <h1 className="text-xl sm:text-2xl font-extrabold text-stone-900 tracking-tight font-serif-display">
                    Kế Hoạch Bữa Ăn Của Tôi
                  </h1>
                  <p className="text-[11px] text-stone-500">
                    Định lượng cho <strong>{preferences.servingSize} người</strong> • {stats.plannedMealsCount} bữa ăn trên lịch
                  </p>
                </div>
              </div>

              {/* Điều hướng tuần */}
              <div className="flex items-center gap-3 text-xs sm:text-sm font-bold text-stone-900 bg-stone-50 border border-stone-200 rounded-full px-3 py-1.5 shadow-xs self-start md:self-center">
                <button
                  type="button"
                  className="p-1 hover:text-orange-600 cursor-pointer"
                  title="Tuần trước"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span>Tuần Này (28 Th10 - 03 Th11)</span>
                <button
                  type="button"
                  className="p-1 hover:text-orange-600 cursor-pointer"
                  title="Tuần sau"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              {/* Các nút thao tác */}
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={onNavigateToPreferences}
                  className="px-4 py-2 rounded-full border border-stone-300 hover:border-orange-500 hover:text-orange-600 text-stone-700 text-xs font-bold tracking-wider uppercase transition-all cursor-pointer flex items-center gap-1.5 shadow-xs"
                >
                  <SlidersHorizontal className="w-3.5 h-3.5" />
                  <span>CHỈNH SỬA SỞ THÍCH</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    generateWeeklyPlan();
                    showToast('Đã tạo mới thực đơn tuần theo sở thích gia đình!');
                  }}
                  className="px-4 py-2 rounded-full bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-700 hover:to-amber-700 text-white text-xs font-bold tracking-wider uppercase transition-all cursor-pointer flex items-center gap-1.5 shadow-xs"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>TẠO LẠI THỰC ĐƠN</span>
                </button>
              </div>

            </div>
          </div>

          {/* Toast thông báo */}
          {toastMessage && (
            <div className="fixed top-24 right-6 z-50 bg-stone-900 text-white px-4 py-3 rounded-2xl shadow-xl flex items-center gap-2.5 text-xs sm:text-sm font-semibold animate-fadeIn">
              <Sparkles className="w-4 h-4 text-amber-400 shrink-0" />
              <span>{toastMessage}</span>
            </div>
          )}

          {/* ====================================================
              NỘI DUNG CHÍNH: SIDEBAR + LƯỚI LỊCH TUẦN 7 NGÀY
             ==================================================== */}
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              
              {/* =========================================
                  CỘT BÊN TRÁI: SIDEBAR
                  1. Card "Đi Chợ Của Tôi" (Estimated Cart Total)
                  2. Card "Cách Thao Tác" (How it works)
                 ========================================= */}
              <div className="lg:col-span-3 space-y-6 text-left">
                
                {/* 1. Card Đi Chợ Của Tôi */}
                <div className="bg-white rounded-3xl p-6 border border-stone-200/90 shadow-xs">
                  
                  <div className="flex items-center gap-2 mb-3">
                    <div className="w-7 h-7 rounded-xl bg-orange-100 text-orange-700 flex items-center justify-center font-bold text-xs">
                      🛒
                    </div>
                    <span className="font-extrabold text-sm text-stone-900 uppercase tracking-wider">
                      Đi Chợ Của Tôi
                    </span>
                  </div>

                  {/* Tổng chi phí ước tính */}
                  <div className="text-3xl font-extrabold text-orange-700 tracking-tight leading-none mb-1 font-serif-display">
                    {formatPrice(stats.estimatedCartTotal)}
                  </div>

                  <div className="flex items-center gap-1.5 text-xs text-stone-500 mb-4 font-normal">
                    <span>Tổng chi phí giỏ hàng dự tính</span>
                    <span className="cursor-help" title="Chi phí dự tính cho tất cả nguyên liệu của tuần">🛈</span>
                  </div>

                  {/* Giá trung bình mỗi suất */}
                  <div className="pt-2 border-t border-gray-100 mb-5">
                    <div className="text-sm font-bold text-stone-900">
                      {formatPrice(stats.averageCostPerServing)} / khẩu phần
                    </div>
                    <div className="text-[11px] text-stone-500 leading-snug mt-0.5">
                      Đơn giá trung bình dựa trên {stats.plannedMealsCount} bữa ăn tuần này
                    </div>
                  </div>

                  {/* Nút CTA: THÊM TẤT CẢ VÀO GIỎ HÀNG */}
                  <button
                    type="button"
                    onClick={() => setIsGroceriesModalOpen(true)}
                    className="w-full py-3.5 px-4 rounded-full bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 text-white font-extrabold text-xs tracking-wider uppercase transition-all shadow-md shadow-orange-600/20 active:scale-[0.99] cursor-pointer text-center flex items-center justify-center gap-2"
                  >
                    <ShoppingBag className="w-4 h-4" />
                    <span>THÊM TẤT CẢ VÀO GIỎ HÀNG</span>
                  </button>
                  <p className="text-[10px] text-stone-400 text-center mt-2">
                    Nhấn để xem đầy đủ danh mục rau củ, thịt cá và đồ khô
                  </p>

                </div>

                {/* 2. Card Hướng Dẫn Thao Tác (How it works) */}
                <div className="bg-white rounded-3xl p-6 border border-stone-200/90 shadow-xs">
                  
                  <button
                    type="button"
                    onClick={() => setIsHowItWorksOpen(!isHowItWorksOpen)}
                    className="w-full flex items-center justify-between text-left font-extrabold text-sm text-stone-900 cursor-pointer"
                  >
                    <span className="flex items-center gap-1.5">
                      <HelpCircle className="w-4 h-4 text-orange-600" />
                      <span>Cách thao tác (How it works)</span>
                    </span>
                    {isHowItWorksOpen ? <ChevronUp className="w-4 h-4 text-stone-500" /> : <ChevronDown className="w-4 h-4 text-stone-500" />}
                  </button>

                  {isHowItWorksOpen && (
                    <div className="mt-4 space-y-4 text-xs text-stone-600 animate-fadeIn">
                      {/* Drag & Drop */}
                      <div className="flex items-start gap-3">
                        <div className="w-6 h-6 rounded-md bg-orange-100 text-orange-800 flex items-center justify-center shrink-0 text-xs font-bold">
                          ⇄
                        </div>
                        <div>
                          <strong className="text-stone-900 font-bold block">Kéo & Thả (Drag & Drop):</strong>
                          Nhấn giữ thẻ món ăn và kéo thả sang bất kỳ ngày nào trong tuần.
                        </div>
                      </div>

                      {/* Add */}
                      <div className="flex items-start gap-3">
                        <div className="w-6 h-6 rounded-md bg-amber-100 text-amber-800 flex items-center justify-center shrink-0 text-xs font-bold">
                          +
                        </div>
                        <div>
                          <strong className="text-stone-900 font-bold block">Thêm món (+ Add):</strong>
                          Bấm nút + THÊM ở góc mỗi ngày để chọn món từ thư viện hoặc tự tạo món.
                        </div>
                      </div>

                      {/* Swap */}
                      <div className="flex items-start gap-3">
                        <div className="w-6 h-6 rounded-md bg-teal-100 text-teal-800 flex items-center justify-center shrink-0 text-xs font-bold">
                          ↺
                        </div>
                        <div>
                          <strong className="text-stone-900 font-bold block">Đổi món (Swap):</strong>
                          Bấm nút SWAP trên ảnh để đổi ngẫu nhiên món phù hợp với khẩu vị gia đình.
                        </div>
                      </div>

                      <div className="pt-2">
                        <button
                          type="button"
                          onClick={() => setIsFeedbackModalOpen(true)}
                          className="font-bold text-xs text-orange-700 underline uppercase tracking-wider hover:text-orange-900 cursor-pointer"
                        >
                          ĐÓNG GÓP Ý KIẾN VỚI BẾP →
                        </button>
                      </div>
                    </div>
                  )}

                </div>

                {/* Nút in lịch tuần */}
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="w-full py-2.5 px-4 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-700 text-xs font-bold transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  <Printer className="w-4 h-4" />
                  <span>In / Xuất Lịch Tuần Ra Giấy</span>
                </button>

              </div>

              {/* =========================================
                  CỘT BÊN PHẢI: LƯỚI LỊCH TUẦN 7 NGÀY
                 ========================================= */}
              <div className="lg:col-span-9">
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                  {DAYS_OF_WEEK.map((day) => {
                    const recipes = weeklyPlan[day.key] || [];
                    return (
                      <DayColumn
                        key={day.key}
                        dayInfo={day}
                        recipes={recipes}
                        onOpenAddModal={handleOpenAddModal}
                        onSwap={handleSwap}
                        onRemove={handleRemove}
                        onClickDetail={(rec) => setDetailRecipe(rec)}
                        formatPrice={formatPrice}
                        swappingCardId={swappingCardId}
                      />
                    );
                  })}
                </div>
              </div>

            </div>
          </div>
        </div>

        {/* ====================================================
            DẢI PHẢN HỒI DƯỚI CHÂN TRANG (MÀU VÀNG ĐÀO ẤM CÚNG)
           ==================================================== */}
        {!isFeedbackDismissed && (
          <div className="bg-[#FEF3E2] text-stone-800 py-3.5 px-4 sm:px-6 border-t border-amber-200">
            <div className="max-w-7xl mx-auto flex items-center justify-between gap-4 text-xs sm:text-sm">
              <div>
                <span>Bạn cảm thấy tính năng Kế hoạch Bữa ăn của Bếp Nhà Mình thế nào? Chúng tôi rất muốn lắng nghe ý kiến của bạn. </span>
                <button
                  type="button"
                  onClick={() => setIsFeedbackModalOpen(true)}
                  className="font-bold underline text-orange-800 hover:text-orange-950 cursor-pointer"
                >
                  Gửi phản hồi ngay
                </button>
              </div>

              <button
                type="button"
                onClick={() => setIsFeedbackDismissed(true)}
                className="p-1 hover:bg-black/5 rounded-full text-stone-600 cursor-pointer shrink-0"
                aria-label="Đóng dải thông báo"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* FOOTER BẾP NHÀ MÌNH */}
        <BepNhaMinhFooter />

        {/* ====================================================
            MODALS
           ==================================================== */}
        <DragOverlay>
          {activeDragItem ? (
            <div className="w-48 bg-white rounded-2xl overflow-hidden shadow-2xl border-2 border-orange-500 scale-105 rotate-2 opacity-95">
              <img
                src={activeDragItem.recipe.image}
                alt={activeDragItem.recipe.title}
                className="w-full h-24 object-cover"
              />
              <div className="p-2">
                <div className="text-xs font-bold text-stone-900 line-clamp-1">
                  {activeDragItem.recipe.title}
                </div>
                <div className="text-[10px] text-orange-600 font-semibold">
                  {formatPrice(activeDragItem.recipe.costPerServing)}/suất
                </div>
              </div>
            </div>
          ) : null}
        </DragOverlay>

        <GroceriesModal
          isOpen={isGroceriesModalOpen}
          onClose={() => setIsGroceriesModalOpen(false)}
        />

        <AddRecipeModal
          isOpen={isAddModalOpen}
          targetDay={targetDay}
          onClose={() => setIsAddModalOpen(false)}
          onSelectRecipe={(rec) => addRecipe(targetDay, rec)}
        />

        <RecipeDetailModal
          recipe={detailRecipe}
          onClose={() => setDetailRecipe(null)}
        />

        <FeedbackModal
          isOpen={isFeedbackModalOpen}
          onClose={() => setIsFeedbackModalOpen(false)}
        />

      </div>
    </DndContext>
  );
};
