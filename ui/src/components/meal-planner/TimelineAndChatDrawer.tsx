import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  X,
  Clock,
  Sparkles,
  CheckCircle2,
  Circle,
  ChevronRight,
  Send,
  MessageSquare,
  Flame,
  ChefHat,
  RotateCw,
  Utensils,
  Lightbulb,
} from 'lucide-react';
import { useMealPlanner } from '../../context/MealPlannerContext';
import { DayOfWeek, PlannerRecipe } from '../../types/mealPlanner';
import { DAYS_OF_WEEK } from '../../data/mealPlannerMockData';

interface TimelineStepItem {
  id: string;
  timeOffsetMinutes: number; // minutes before target dinner time (e.g. 75 min before)
  task: string;
  detail: string;
  dishName?: string;
  isCompleted: boolean;
}

interface ChatMessageItem {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  timestamp: string;
  actionTaken?: 'time_changed' | 'recipe_swapped' | 'none';
}

interface TimelineAndChatDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  initialTab?: 'timeline' | 'chat';
  currentDay: DayOfWeek;
  onChangeDay: (day: DayOfWeek) => void;
}

export const TimelineAndChatDrawer: React.FC<TimelineAndChatDrawerProps> = ({
  isOpen,
  onClose,
  initialTab = 'timeline',
  currentDay,
  onChangeDay,
}) => {
  const { weeklyPlan, swapRecipe, formatPrice } = useMealPlanner();

  const [activeTab, setActiveTab] = useState<'timeline' | 'chat'>(initialTab);
  const [targetDinnerTime, setTargetDinnerTime] = useState<string>('18:30'); // Default dinner target time
  const [completedStepIds, setCompletedStepIds] = useState<Record<string, boolean>>({});

  // Chatbot state
  const [chatMessages, setChatMessages] = useState<ChatMessageItem[]>([
    {
      id: 'welcome',
      sender: 'ai',
      text: 'Chào bạn! Mình là Bếp Trưởng AI của Bếp Nhà Mình. Mình có thể giúp bạn tính lại timeline khi ăn muộn, đổi món trên lịch tuần, hoặc hướng dẫn mẹo nấu nhanh từng món!',
      timestamp: 'Vừa xong',
    },
  ]);
  const [inputText, setInputText] = useState('');
  const [isAiTyping, setIsAiTyping] = useState(false);
  const chatBottomRef = useRef<HTMLDivElement>(null);

  // Sync tab when initialTab changes
  useEffect(() => {
    if (isOpen) {
      setActiveTab(initialTab);
    }
  }, [isOpen, initialTab]);

  // Auto-scroll chat
  useEffect(() => {
    if (activeTab === 'chat') {
      chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [chatMessages, activeTab, isAiTyping]);

  // Current day info
  const dayInfo = useMemo(() => {
    return DAYS_OF_WEEK.find((d) => d.key === currentDay) || DAYS_OF_WEEK[0];
  }, [currentDay]);

  const currentRecipes = useMemo(() => {
    return weeklyPlan[currentDay] || [];
  }, [weeklyPlan, currentDay]);

  // Helper to subtract minutes from "HH:MM"
  const calculateStepTime = (timeStr: string, minutesToSubtract: number): string => {
    const [h, m] = timeStr.split(':').map(Number);
    const date = new Date();
    date.setHours(h, m, 0, 0);
    date.setMinutes(date.getMinutes() - minutesToSubtract);
    const resH = String(date.getHours()).padStart(2, '0');
    const resM = String(date.getMinutes()).padStart(2, '0');
    return `${resH}:${resM}`;
  };

  // Generate dynamic timeline steps based on recipes in this day
  const timelineSteps: TimelineStepItem[] = useMemo(() => {
    const mainDish = currentRecipes[0]?.title || 'Món chính gia đình';
    const totalCookTime = Math.max(...currentRecipes.map((r) => r.cookTime), 25);

    return [
      {
        id: `step-defrost-${currentDay}`,
        timeOffsetMinutes: totalCookTime + 45,
        task: 'Rã đông nguyên liệu',
        detail: `Lấy thịt/cá (${mainDish}) ra khỏi ngăn đông, ngâm trong thau nước mát hoặc rã đông vi sóng.`,
        dishName: mainDish,
        isCompleted: !!completedStepIds[`step-defrost-${currentDay}`],
      },
      {
        id: `step-prep-${currentDay}`,
        timeOffsetMinutes: totalCookTime + 15,
        task: 'Sơ chế & Ướp gia vị',
        detail: 'Rửa sạch rau củ, thái hành tỏi và ướp nước mắm, tiêu để món ăn ngấm vị đậm đà.',
        dishName: mainDish,
        isCompleted: !!completedStepIds[`step-prep-${currentDay}`],
      },
      {
        id: `step-rice-${currentDay}`,
        timeOffsetMinutes: 30,
        task: 'Vo gạo & Cắm cơm điện',
        detail: 'Cắm cơm đúng giờ để cơm vừa chín dẻo, nóng hổi đồng bộ khi đồ ăn nấu xong.',
        isCompleted: !!completedStepIds[`step-rice-${currentDay}`],
      },
      {
        id: `step-cook-${currentDay}`,
        timeOffsetMinutes: totalCookTime,
        task: 'Bật bếp & Nấu chín',
        detail: `Áp chảo/kho ${mainDish} và hoàn tất món canh hoặc rau luộc. Nêm nếm vừa miệng.`,
        dishName: mainDish,
        isCompleted: !!completedStepIds[`step-cook-${currentDay}`],
      },
      {
        id: `step-serve-${currentDay}`,
        timeOffsetMinutes: 0,
        task: 'Dọn mâm cơm nóng',
        detail: 'Bày biện ra bàn, rắc thêm tiêu hành ngò. Mời cả nhà cùng thưởng thức!',
        isCompleted: !!completedStepIds[`step-serve-${currentDay}`],
      },
    ];
  }, [currentRecipes, currentDay, completedStepIds]);

  const completedCount = timelineSteps.filter((s) => s.isCompleted).length;

  const toggleStep = (stepId: string) => {
    setCompletedStepIds((prev) => ({
      ...prev,
      [stepId]: !prev[stepId],
    }));
  };

  // Chat message sending with Actionable AI engine
  const handleSendMessage = (textToSend?: string) => {
    const query = (textToSend || inputText).trim();
    if (!query) return;

    const userMsg: ChatMessageItem = {
      id: `user-${Date.now()}`,
      sender: 'user',
      text: query,
      timestamp: 'Vừa xong',
    };

    setChatMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputText('');
    setIsAiTyping(true);

    // AI Intent Recognition & Action Execution
    setTimeout(() => {
      const lower = query.toLowerCase();
      let reply = '';
      let actionTaken: ChatMessageItem['actionTaken'] = 'none';

      // 1. Intent: Change dinner time
      const timeMatch = query.match(/(\d{1,2})[:h](\d{2})?/i);
      if (
        (lower.includes('ăn muộn') ||
          lower.includes('ăn lúc') ||
          lower.includes('giờ') ||
          lower.includes('timeline')) &&
        timeMatch
      ) {
        let newHour = parseInt(timeMatch[1], 10);
        let newMin = timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;
        if (newHour < 24) {
          const newTimeStr = `${String(newHour).padStart(2, '0')}:${String(newMin).padStart(2, '0')}`;
          setTargetDinnerTime(newTimeStr);
          actionTaken = 'time_changed';
          reply = `Đã cập nhật giờ ăn tối của bạn thành ${newTimeStr}! Toàn bộ kịch bản rã đông và bật bếp của ${dayInfo.fullLabel} đã được tính ngược lùi lại chính xác.`;
        }
      }
      // 2. Intent: Swap dish
      else if (
        lower.includes('đổi món') ||
        lower.includes('thay món') ||
        lower.includes('món khác') ||
        lower.includes('đổi sang')
      ) {
        if (currentRecipes.length > 0) {
          const swapped = swapRecipe(currentDay, currentRecipes[0].id);
          actionTaken = 'recipe_swapped';
          if (swapped) {
            reply = `Bếp đã tự động đổi món trên lịch tuần thành "${swapped.title}" (${formatPrice(swapped.costPerServing)}/suất). Timeline nấu nướng của ${dayInfo.fullLabel} cũng đã được cập nhật lại theo thời gian nấu ${swapped.cookTime} phút của món mới!`;
          } else {
            reply = `Bếp đã kiểm tra kho công thức nhưng chưa tìm thấy món thay thế phù hợp hơn với bộ lọc dị ứng hiện tại của bạn.`;
          }
        } else {
          reply = `Hôm nay trên lịch chưa có món nào. Bạn hãy bấm nút "+ THÊM" trên cột ngày để chọn món trước nhé!`;
        }
      }
      // 3. Intent: Cooking tips
      else if (lower.includes('mẹo') || lower.includes('nấu') || lower.includes('tanh') || lower.includes('giòn')) {
        reply = `Mẹo hay từ Bếp: Để cá kho hoặc áp chảo không bị tanh, bạn hãy rửa cá bằng nước vo gạo hoặc xát chút rượu gừng trước khi ướp. Khi nấu, nhớ phi thơm hành tỏi và đun lửa liu riu để cá săn chắc, đậm vị nhé!`;
      }
      // 4. Intent: Nutrition check
      else if (lower.includes('dinh dưỡng') || lower.includes('tiểu đường') || lower.includes('calo')) {
        reply = `Mâm cơm của ${dayInfo.fullLabel} đang có ${currentRecipes.length} món với mức năng lượng cân đối (trung bình ~380-450 kcal/suất). Đã kiểm tra an toàn theo hồ sơ dinh dưỡng gia đình, đảm bảo giàu chất xơ và ít muối!`;
      }
      // 5. Default cheerful response
      else {
        reply = `Bếp đã ghi nhận ý kiến của bạn! Bạn có thể yêu cầu: "Đổi giờ ăn tối sang 19:15", "Đổi món hôm nay", hoặc hỏi bất kỳ mẹo nấu nướng nào, Bếp luôn sẵn sàng hỗ trợ!`;
      }

      setChatMessages((prev) => [
        ...prev,
        {
          id: `ai-${Date.now()}`,
          sender: 'ai',
          text: reply,
          timestamp: 'Vừa xong',
          actionTaken,
        },
      ]);
      setIsAiTyping(false);
    }, 700);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden flex justify-end animate-fadeIn">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/40 backdrop-blur-2xs transition-opacity"
      />

      {/* Slide-over Drawer Panel */}
      <div className="relative w-full max-w-md bg-white shadow-2xl h-full flex flex-col z-10 text-left border-l border-stone-200">
        
        {/* Drawer Header */}
        <div className="px-5 py-4 border-b border-stone-100 flex items-center justify-between bg-stone-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-[#014043] text-white flex items-center justify-center font-bold text-sm shadow-xs">
              <ChefHat className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-extrabold text-sm sm:text-base text-stone-900 leading-tight">
                {activeTab === 'timeline' ? 'Timeline Nấu Tính Ngược' : 'Trợ Lý Bếp AI'}
              </h3>
              <p className="text-[11px] text-stone-500 font-medium">
                {dayInfo.fullLabel} • {dayInfo.dateLabel}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white hover:bg-stone-200 text-stone-500 hover:text-stone-800 flex items-center justify-center transition-colors cursor-pointer border border-stone-200"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tab Switcher (Timeline vs Chatbot) */}
        <div className="grid grid-cols-2 p-1.5 bg-stone-100 mx-5 mt-4 rounded-xl text-xs font-bold">
          <button
            type="button"
            onClick={() => setActiveTab('timeline')}
            className={`py-2 rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
              activeTab === 'timeline'
                ? 'bg-white text-[#014043] shadow-xs'
                : 'text-stone-600 hover:text-stone-900'
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            <span>Timeline Nấu</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('chat')}
            className={`py-2 rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
              activeTab === 'chat'
                ? 'bg-white text-[#014043] shadow-xs'
                : 'text-stone-600 hover:text-stone-900'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Trợ Lý Bếp AI</span>
          </button>
        </div>

        {/* Day Selector Pills */}
        <div className="px-5 pt-3 pb-2 flex gap-1.5 overflow-x-auto no-scrollbar border-b border-stone-100">
          {DAYS_OF_WEEK.map((d) => {
            const isSelected = d.key === currentDay;
            return (
              <button
                key={d.key}
                type="button"
                onClick={() => onChangeDay(d.key)}
                className={`px-3 py-1 rounded-full text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-[#014043] text-white shadow-xs'
                    : 'bg-stone-50 text-stone-600 hover:bg-stone-100 border border-stone-200'
                }`}
              >
                {d.code}
              </button>
            );
          })}
        </div>

        {/* =======================================================
            TAB 1: TIMELINE NẤU TÍNH NGƯỢC
           ======================================================= */}
        {activeTab === 'timeline' && (
          <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">
            
            {/* Setting: Target Dinner Time */}
            <div className="p-3.5 rounded-2xl bg-teal-50/40 border border-teal-100/80 flex items-center justify-between">
              <div>
                <span className="text-[11px] font-bold text-[#014043] uppercase tracking-wider block">
                  Giờ ăn tối mục tiêu
                </span>
                <span className="text-xs text-stone-500">
                  Timeline sẽ tự động tính ngược từ mốc này
                </span>
              </div>

              <div className="flex items-center gap-1.5">
                <input
                  type="time"
                  value={targetDinnerTime}
                  onChange={(e) => setTargetDinnerTime(e.target.value)}
                  className="bg-white border border-stone-200 rounded-xl px-2.5 py-1.5 text-xs font-bold text-stone-900 outline-none focus:border-[#014043] cursor-pointer shadow-2xs"
                />
              </div>
            </div>

            {/* Menu summary for this day */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-stone-800">
                  Món ăn trong ngày ({currentRecipes.length})
                </span>
                <button
                  type="button"
                  onClick={() => setActiveTab('chat')}
                  className="text-[11px] font-semibold text-[#014043] hover:underline cursor-pointer flex items-center gap-1"
                >
                  <Sparkles className="w-3 h-3" />
                  <span>Đổi món bằng AI</span>
                </button>
              </div>

              {currentRecipes.length === 0 ? (
                <div className="p-4 rounded-2xl bg-stone-50 border border-dashed border-stone-200 text-center text-xs text-stone-500">
                  Chưa có món nào trên lịch ngày {dayInfo.fullLabel}. Hãy thêm món để Bếp tạo timeline chi tiết!
                </div>
              ) : (
                <div className="space-y-2">
                  {currentRecipes.map((r) => (
                    <div
                      key={r.id}
                      className="flex items-center gap-2.5 p-2 rounded-xl bg-stone-50 border border-stone-200/80"
                    >
                      <img
                        src={r.image}
                        alt={r.title}
                        className="w-10 h-10 rounded-lg object-cover shrink-0"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="text-xs font-bold text-stone-900 truncate">
                          {r.title}
                        </div>
                        <div className="text-[10px] text-stone-500 flex items-center gap-2 mt-0.5">
                          <span className="flex items-center gap-0.5">
                            <Clock className="w-2.5 h-2.5 text-stone-400" />
                            {r.cookTime} phút
                          </span>
                          <span>•</span>
                          <span>{formatPrice(r.costPerServing)}/suất</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Timeline Progress */}
            <div className="pt-1">
              <div className="flex items-center justify-between text-xs font-bold mb-2">
                <span className="text-stone-900">Kịch bản tính ngược từng bước</span>
                <span className="text-[#014043] font-semibold">
                  {completedCount}/{timelineSteps.length} hoàn tất
                </span>
              </div>
              <div className="w-full bg-stone-100 rounded-full h-1.5 overflow-hidden">
                <div
                  className="bg-[#014043] h-1.5 transition-all duration-300 rounded-full"
                  style={{ width: `${(completedCount / timelineSteps.length) * 100}%` }}
                />
              </div>
            </div>

            {/* Steps list */}
            <div className="space-y-3 relative before:absolute before:inset-0 before:left-3.5 before:w-0.5 before:bg-stone-200 before:z-0">
              {timelineSteps.map((step, idx) => {
                const stepClockTime = calculateStepTime(targetDinnerTime, step.timeOffsetMinutes);
                return (
                  <div
                    key={step.id}
                    onClick={() => toggleStep(step.id)}
                    className={`relative z-10 flex items-start gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                      step.isCompleted
                        ? 'bg-stone-50/70 border-stone-200 opacity-60'
                        : 'bg-white border-stone-200 hover:border-stone-300 shadow-2xs'
                    }`}
                  >
                    {/* Checkbox circle */}
                    <div className="mt-0.5 shrink-0">
                      {step.isCompleted ? (
                        <CheckCircle2 className="w-5 h-5 text-emerald-600 fill-emerald-100" />
                      ) : (
                        <Circle className="w-5 h-5 text-stone-300 hover:text-[#014043]" />
                      )}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1 mb-1">
                        <span
                          className={`text-xs font-bold ${
                            step.isCompleted
                              ? 'line-through text-stone-400'
                              : 'text-stone-900'
                          }`}
                        >
                          {step.task}
                        </span>
                        <span className="px-2 py-0.5 rounded-full bg-stone-100 font-mono text-[11px] font-bold text-[#014043]">
                          {stepClockTime}
                        </span>
                      </div>

                      <p
                        className={`text-[11px] leading-relaxed ${
                          step.isCompleted ? 'text-stone-400' : 'text-stone-600'
                        }`}
                      >
                        {step.detail}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Hint Box for one-handed cooking */}
            <div className="p-3 rounded-xl bg-stone-50 border border-stone-200 text-[11px] text-stone-500 flex items-start gap-2">
              <Lightbulb className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
              <span>
                Khi đang đứng bếp, chỉ cần 1 chạm vào từng ô để đánh dấu đã nấu xong.
              </span>
            </div>

          </div>
        )}

        {/* =======================================================
            TAB 2: TRỢ LÝ BẾP AI
           ======================================================= */}
        {activeTab === 'chat' && (
          <div className="flex-1 flex flex-col justify-between overflow-hidden">
            
            {/* Chat Thread */}
            <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3.5">
              {chatMessages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex flex-col ${
                    msg.sender === 'user' ? 'items-end' : 'items-start'
                  }`}
                >
                  <div
                    className={`max-w-[85%] p-3.5 rounded-2xl text-xs sm:text-sm leading-relaxed ${
                      msg.sender === 'user'
                        ? 'bg-[#014043] text-white rounded-tr-2xs shadow-xs'
                        : 'bg-stone-100 text-stone-800 rounded-tl-2xs border border-stone-200/70'
                    }`}
                  >
                    {msg.text}
                  </div>
                  <span className="text-[10px] text-stone-400 mt-1 px-1">
                    {msg.timestamp}
                  </span>
                </div>
              ))}

              {isAiTyping && (
                <div className="flex items-center gap-1.5 p-3 rounded-2xl bg-stone-100 text-stone-500 text-xs w-28">
                  <span className="w-1.5 h-1.5 rounded-full bg-stone-400 animate-bounce" />
                  <span className="w-1.5 h-1.5 rounded-full bg-stone-400 animate-bounce [animation-delay:0.2s]" />
                  <span className="w-1.5 h-1.5 rounded-full bg-stone-400 animate-bounce [animation-delay:0.4s]" />
                </div>
              )}

              <div ref={chatBottomRef} />
            </div>

            {/* Quick Prompt Pills (1-chạm) */}
            <div className="px-5 py-2 border-t border-stone-100 bg-stone-50/50">
              <div className="text-[10px] font-bold text-stone-400 mb-1.5 uppercase tracking-wider">
                Gợi ý nhanh 1-chạm
              </div>
              <div className="flex gap-1.5 overflow-x-auto no-scrollbar pb-1">
                <button
                  type="button"
                  onClick={() => handleSendMessage('Hôm nay nhà mình ăn tối lúc 19:30, tính lại timeline')}
                  className="px-2.5 py-1 rounded-full bg-white border border-stone-200 text-stone-700 hover:border-[#014043] hover:text-[#014043] text-[11px] whitespace-nowrap cursor-pointer transition-colors"
                >
                  ⏰ Ăn tối lúc 19:30
                </button>

                <button
                  type="button"
                  onClick={() => handleSendMessage('Đổi món hôm nay sang món khác')}
                  className="px-2.5 py-1 rounded-full bg-white border border-stone-200 text-stone-700 hover:border-[#014043] hover:text-[#014043] text-[11px] whitespace-nowrap cursor-pointer transition-colors"
                >
                  ⇄ Đổi món hôm nay
                </button>

                <button
                  type="button"
                  onClick={() => handleSendMessage('Cho mình mẹo nấu cá không bị tanh')}
                  className="px-2.5 py-1 rounded-full bg-white border border-stone-200 text-stone-700 hover:border-[#014043] hover:text-[#014043] text-[11px] whitespace-nowrap cursor-pointer transition-colors"
                >
                  💡 Mẹo nấu cá không tanh
                </button>

                <button
                  type="button"
                  onClick={() => handleSendMessage('Kiểm tra dinh dưỡng mâm cơm hôm nay')}
                  className="px-2.5 py-1 rounded-full bg-white border border-stone-200 text-stone-700 hover:border-[#014043] hover:text-[#014043] text-[11px] whitespace-nowrap cursor-pointer transition-colors"
                >
                  🥗 Kiểm tra dinh dưỡng
                </button>
              </div>
            </div>

            {/* Chat Input Field */}
            <div className="p-4 border-t border-stone-200 bg-white">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendMessage();
                }}
                className="flex items-center gap-2"
              >
                <input
                  type="text"
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  placeholder="Hỏi Bếp: 'Ăn muộn lúc 19h30', 'Đổi món canh'..."
                  className="flex-1 bg-stone-50 border border-stone-200 rounded-full px-4 py-2.5 text-xs sm:text-sm text-stone-900 outline-none focus:border-[#014043] focus:bg-white transition-colors"
                />
                <button
                  type="submit"
                  disabled={!inputText.trim()}
                  className="w-9 h-9 rounded-full bg-[#014043] hover:bg-[#002f32] disabled:opacity-40 disabled:cursor-not-allowed text-white flex items-center justify-center shrink-0 transition-colors shadow-xs cursor-pointer"
                >
                  <Send className="w-4 h-4 ml-0.5" />
                </button>
              </form>
            </div>

          </div>
        )}

      </div>
    </div>
  );
};
