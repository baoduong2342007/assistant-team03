import React from 'react';
import { VideoBackground } from './VideoBackground';
import { ArrowRight, Play, ShieldCheck, HeartPulse, Clock, Sparkles, ChefHat, CheckCircle2 } from 'lucide-react';
import { SAMPLE_MEAL_PLAN } from '../data/mockData';

interface HeroLandingProps {
  onBeginNow: () => void;
  onExploreDirectChat: () => void;
  onOpenIntro: () => void;
}

export const HeroLanding: React.FC<HeroLandingProps> = ({
  onBeginNow,
  onExploreDirectChat,
  onOpenIntro,
}) => {
  return (
    <div className="relative min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col justify-between overflow-hidden">
      
      {/* TẦNG 2: Video nấu ăn chạy nền (~50% màn hình, chuyển mờ dần xuống nền sáng) */}
      <VideoBackground />

      {/* TẦNG 3: Giao diện bắt đầu (Phong cách Antigravity: nền sáng, hạt trang trí nhẹ, chữ cực lớn ở chính giữa) */}
      <section className="relative z-20 -mt-24 sm:-mt-28 px-4 sm:px-6 pb-20 max-w-5xl mx-auto w-full text-center flex flex-col items-center">
        
        {/* Antigravity floating particles / stars */}
        <div className="pointer-events-none absolute inset-0 -top-20 overflow-hidden flex justify-center">
          <div className="w-full max-w-4xl h-full relative">
            <span className="absolute top-4 left-10 text-amber-500/60 text-lg animate-sparkle">✦</span>
            <span className="absolute top-12 left-1/4 text-orange-400/50 text-xs animate-sparkle" style={{ animationDelay: '1s' }}>•</span>
            <span className="absolute top-8 right-16 text-amber-600/70 text-xl animate-sparkle" style={{ animationDelay: '1.8s' }}>*</span>
            <span className="absolute top-24 right-1/4 text-orange-500/40 text-sm animate-sparkle" style={{ animationDelay: '0.6s' }}>✦</span>
            <span className="absolute top-36 left-16 text-amber-400/50 text-xs animate-sparkle" style={{ animationDelay: '2.4s' }}>.</span>
            <span className="absolute top-44 right-12 text-orange-400/50 text-base animate-sparkle" style={{ animationDelay: '1.2s' }}>*</span>
          </div>
        </div>

        {/* Tagline Badge */}
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/90 backdrop-blur-md border border-amber-200/80 shadow-[0_4px_20px_rgba(245,158,11,0.12)] text-amber-900 text-xs sm:text-sm font-semibold tracking-wide uppercase mb-6 sm:mb-8 transition-transform hover:scale-105 cursor-default">
          <span className="text-amber-500 animate-pulse text-sm">✨</span>
          <span>Trợ lý dinh dưỡng gia đình AI</span>
          <span className="text-amber-500 animate-pulse text-sm">✨</span>
        </div>

        {/* Extra Large Center Headline */}
        <h1 className="text-4xl sm:text-6xl md:text-7xl font-bold font-serif-display tracking-tight text-stone-900 leading-[1.12] mb-4 sm:mb-6 max-w-4xl">
          Hôm nay nhà mình ăn gì?
          <span className="block mt-2 sm:mt-3 text-3xl sm:text-5xl md:text-6xl text-transparent bg-clip-text bg-gradient-to-r from-orange-700 via-amber-700 to-amber-600">
            Để Bếp lo trọn vẹn từng bữa.
          </span>
        </h1>

        {/* Subtitle / Core highlights */}
        <p className="text-stone-600 font-medium text-base sm:text-xl md:text-2xl max-w-2xl mb-8 sm:mb-10 leading-relaxed">
          (Mâm cơm hợp bệnh lý từng người • Timeline nấu tính ngược)
        </p>

        {/* PROMINENT PILL CTA BUTTON: ▶ BEGIN NOW */}
        <div className="flex flex-col sm:flex-row items-center gap-4 sm:gap-5 mb-14">
          <button
            onClick={onBeginNow}
            className="group relative inline-flex items-center justify-center gap-3 px-8 sm:px-10 py-4 sm:py-4.5 rounded-full text-base sm:text-lg font-bold text-white bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 shadow-[0_12px_35px_rgba(234,88,12,0.35)] hover:shadow-[0_16px_45px_rgba(234,88,12,0.5)] active:scale-95 transition-all duration-300 cursor-pointer overflow-hidden border border-orange-400/30"
          >
            {/* Subtle light sweep animation */}
            <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full bg-gradient-to-r from-transparent via-white/25 to-transparent transition-transform duration-1000 ease-in-out" />
            
            <div className="w-7 h-7 rounded-full bg-white text-orange-600 flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform">
              <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
            </div>
            
            <span className="tracking-wide uppercase font-extrabold text-sm sm:text-base">BEGIN NOW</span>
            
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </button>

          <button
            onClick={onExploreDirectChat}
            className="inline-flex items-center gap-2 px-6 py-3.5 rounded-full text-stone-700 font-semibold text-sm sm:text-base bg-white/80 hover:bg-white border border-stone-200/80 shadow-sm hover:shadow transition-all cursor-pointer"
          >
            <ChefHat className="w-4 h-4 text-orange-600" />
            <span>Xem thử Mâm cơm & Chatbot AI ↗</span>
          </button>
        </div>

        {/* 3 Pillars of Safety & Antigravity value card */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-5 w-full text-left max-w-4xl pt-4">
          
          <div className="bg-white/85 backdrop-blur-sm p-5 sm:p-6 rounded-2xl border border-stone-200/70 shadow-sm hover:shadow-md transition-shadow group">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-stone-900 text-base mb-1">Dị ứng là bộ lọc tuyệt đối</h3>
            <p className="text-stone-500 text-xs sm:text-sm leading-relaxed">
              Code tự động loại trừ nguyên liệu dị ứng (tôm, cua, đậu phộng...) trước khi đề xuất. An toàn 100%.
            </p>
          </div>

          <div className="bg-white/85 backdrop-blur-sm p-5 sm:p-6 rounded-2xl border border-stone-200/70 shadow-sm hover:shadow-md transition-shadow group">
            <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <HeartPulse className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-stone-900 text-base mb-1">Quy tắc bệnh lý chính thống</h3>
            <p className="text-stone-500 text-xs sm:text-sm leading-relaxed">
              Dựa trên Thông tư 43/2014/TT-BYT & Viện Dinh Dưỡng: Ba huyết áp cao giảm muối, mẹ tiểu đường chọn carb chậm.
            </p>
          </div>

          <div className="bg-white/85 backdrop-blur-sm p-5 sm:p-6 rounded-2xl border border-stone-200/70 shadow-sm hover:shadow-md transition-shadow group">
            <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <Clock className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-stone-900 text-base mb-1">Timeline nấu tính ngược</h3>
            <p className="text-stone-500 text-xs sm:text-sm leading-relaxed">
              Tính ngược từ giờ ăn 18:30: rã đông 16:30, sơ chế 17:15, kho cá 17:35 để đúng giờ dọn mâm tròn vị.
            </p>
          </div>

        </div>

        {/* Quick Example Teaser */}
        <div className="mt-12 w-full max-w-4xl bg-gradient-to-r from-amber-50/80 via-orange-50/60 to-stone-50/80 border border-amber-200/60 rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-sm">
          <div className="text-left space-y-1">
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full bg-orange-100 text-orange-800 font-bold text-[11px]">
                VÍ DỤ THỰC TẾ
              </span>
              <span className="text-xs text-stone-500">Gia đình mẫu 4 người</span>
            </div>
            <h4 className="font-bold text-stone-900 text-lg sm:text-xl">
              "Tối nay nhà muốn ăn cá kho"
            </h4>
            <p className="text-stone-600 text-xs sm:text-sm max-w-md">
              AI bổ sung Canh bí đỏ + Cải ngọt luộc để đủ chất, đồng thời tách phần nhạt cho Ba trước khi rim mắm đậm!
            </p>
          </div>
          
          <button
            onClick={onExploreDirectChat}
            className="shrink-0 px-5 py-2.5 rounded-full text-xs sm:text-sm font-semibold bg-stone-900 text-white hover:bg-stone-800 transition-colors cursor-pointer"
          >
            Trải nghiệm kịch bản này →
          </button>
        </div>

      </section>

      {/* Footer minimal info */}
      <footer className="border-t border-stone-200/60 py-6 px-4 text-center text-xs text-stone-500">
        <p className="max-w-2xl mx-auto leading-relaxed">
          Bếp Nhà Mình — Family Meal Planner AI • Hướng dẫn đối chiếu Viện Dinh Dưỡng Quốc Gia Việt Nam & Thông tư 43/2014/TT-BYT.
        </p>
      </footer>

    </div>
  );
};
