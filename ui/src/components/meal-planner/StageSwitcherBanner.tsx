import React from 'react';
import { Sparkles, ArrowRight, CheckCircle2 } from 'lucide-react';

export type MealPlannerStage =
  | 'landing' // /meal-planner
  | 'preference' // /meal-planner/preference
  | 'auth' // /account/signup?next=/meal-planner/app
  | 'app'; // /meal-planner/app

interface StageSwitcherBannerProps {
  currentStage: MealPlannerStage;
  onSelectStage: (stage: MealPlannerStage) => void;
  onExitToHome: () => void;
}

export const StageSwitcherBanner: React.FC<StageSwitcherBannerProps> = ({
  currentStage,
  onSelectStage,
  onExitToHome,
}) => {
  const stages: {
    key: MealPlannerStage;
    stepNumber: number;
    label: string;
    url: string;
    tag: string;
  }[] = [
    {
      key: 'landing',
      stepNumber: 1,
      label: '1. Thiết Lập Nhanh',
      url: '/meal-planner',
      tag: 'Giới thiệu & Cấu hình nhanh',
    },
    {
      key: 'preference',
      stepNumber: 2,
      label: '2. Khảo Sát Khẩu Vị',
      url: '/meal-planner/preference',
      tag: 'Khẩu vị, chế độ ăn & dị ứng',
    },
    {
      key: 'auth',
      stepNumber: 3,
      label: '3. Xác Thực Người Dùng',
      url: '/account/signup?next=/meal-planner/app',
      tag: 'Đăng nhập & Đăng ký tài khoản',
    },
    {
      key: 'app',
      stepNumber: 4,
      label: '4. Lịch Thực Đơn Tuần',
      url: '/meal-planner/app',
      tag: 'Bảng kế hoạch tuần & Kéo-Thả',
    },
  ];

  const currentStageObj = stages.find((s) => s.key === currentStage);

  return (
    <div className="bg-[#1f2937] text-white text-xs border-b border-stone-800 z-50 sticky top-0 shadow-md">
      <div className="max-w-7xl mx-auto px-3 sm:px-6 py-2 flex flex-col md:flex-row items-center justify-between gap-2">
        
        {/* Left: Active Stage Info & URL display */}
        <div className="flex items-center gap-2 flex-wrap text-stone-300">
          <span className="font-bold text-amber-400 flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5" />
            <span>🍲 Bếp Nhà Mình • Luồng Lập Kế Hoạch:</span>
          </span>
          <span className="bg-stone-800 px-2.5 py-0.5 rounded-full font-mono text-[11px] text-teal-300 border border-stone-700">
            {currentStageObj?.url}
          </span>
          <span className="hidden sm:inline text-stone-500">|</span>
          <span className="hidden sm:inline text-stone-300 font-medium">
            {currentStageObj?.tag}
          </span>
        </div>

        {/* Right: Stage buttons for quick switching */}
        <div className="flex items-center gap-1.5 overflow-x-auto max-w-full pb-1 md:pb-0">
          {stages.map((stage) => {
            const isActive = currentStage === stage.key;
            return (
              <button
                key={stage.key}
                type="button"
                onClick={() => onSelectStage(stage.key)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold whitespace-nowrap transition-all cursor-pointer flex items-center gap-1 ${
                  isActive
                    ? 'bg-gradient-to-r from-orange-600 to-amber-600 text-white shadow-xs scale-102 ring-1 ring-orange-400'
                    : 'bg-stone-800 hover:bg-stone-700 text-stone-300'
                }`}
                title={`Chuyển đến: ${stage.url}`}
              >
                <span>{stage.label}</span>
              </button>
            );
          })}

          <button
            type="button"
            onClick={onExitToHome}
            className="px-2.5 py-1 rounded-lg text-[11px] font-semibold text-stone-400 hover:text-white bg-transparent hover:bg-stone-800 cursor-pointer ml-1"
            title="Quay lại trang chủ ban đầu"
          >
            Về Trang Chủ
          </button>
        </div>

      </div>
    </div>
  );
};
