import React, { useState } from 'react';
import { VideoBackground } from './VideoBackground';
import { ArrowRight, Play, ShieldCheck, HeartPulse, Clock, Search } from 'lucide-react';

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
  const [cravingQuery, setCravingQuery] = useState('');

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onExploreDirectChat();
  };

  return (
    <div className="relative min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col justify-between overflow-hidden">
      
      {/* TẦNG 2: Video/Ảnh nền chuyển động mượt mà (Fade in/out + Slow Zoom) */}
      <VideoBackground />

      {/* TẦNG 3: Giao diện chính */}
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

        {/* Extra Large Center Headline: Đã khắc phục che cạnh trên & dưới của chữ */}
        <h1 className="text-4xl sm:text-6xl md:text-7xl font-bold font-serif-display tracking-tight text-stone-900 leading-tight mb-6 sm:mb-8 max-w-4xl">
          <span className="block">Hôm nay nhà mình ăn gì?</span>
          <span className="inline-block mt-0 sm:mt-0.5 pt-1 pb-3 px-1.5 text-3xl sm:text-5xl md:text-6xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-orange-700 via-amber-700 to-amber-600 leading-snug">
            Để Bếp lo trọn vẹn từng bữa.
          </span>
        </h1>

        {/* SideChef-Style Hero Search Bar (Thay thế dòng chữ trong ngoặc) */}
        <div className="w-full max-w-2xl mx-auto mb-8 sm:mb-10">
          <form
            onSubmit={handleSearchSubmit}
            className="relative bg-white rounded-full shadow-[0_8px_30px_rgb(0,0,0,0.08)] hover:shadow-[0_12px_35px_rgba(0,0,0,0.12)] border border-stone-200/80 p-1.5 sm:p-2 pl-5 sm:pl-6 flex items-center gap-3 transition-all focus-within:shadow-[0_12px_35px_rgba(1,64,67,0.16)] focus-within:border-[#014043]"
          >
            <Search className="w-5 h-5 text-stone-400 shrink-0" />
            <input
              type="text"
              value={cravingQuery}
              onChange={(e) => setCravingQuery(e.target.value)}
              placeholder="Hôm nay bạn thèm món gì?"
              className="flex-1 bg-transparent border-none outline-none text-stone-800 placeholder-stone-400 text-sm sm:text-base font-normal"
            />
            <button
              type="submit"
              className="px-6 sm:px-8 py-2.5 sm:py-3 rounded-full bg-[#014043] hover:bg-[#002f32] active:scale-95 text-white font-bold text-xs sm:text-sm tracking-widest uppercase transition-all shadow-sm cursor-pointer shrink-0"
            >
              TÌM KIẾM
            </button>
          </form>
        </div>

        {/* PROMINENT PILL CTA BUTTON: Đưa vào chính giữa & Đổi chữ thành "CÁ NHÂN HÓA THỰC ĐƠN NGAY" */}
        <div className="flex justify-center w-full mb-14">
          <button
            onClick={onBeginNow}
            className="group relative inline-flex items-center justify-center gap-3 px-8 sm:px-10 py-4 sm:py-4.5 rounded-full text-base sm:text-lg font-bold text-white bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 shadow-[0_12px_35px_rgba(234,88,12,0.35)] hover:shadow-[0_16px_45px_rgba(234,88,12,0.5)] active:scale-95 transition-all duration-300 cursor-pointer overflow-hidden border border-orange-400/30"
          >
            {/* Subtle light sweep animation */}
            <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full bg-gradient-to-r from-transparent via-white/25 to-transparent transition-transform duration-1000 ease-in-out" />
            
            <div className="w-7 h-7 rounded-full bg-white text-orange-600 flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform shrink-0">
              <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
            </div>
            
            <span className="tracking-wide uppercase font-extrabold text-sm sm:text-base whitespace-nowrap">
              CÁ NHÂN HÓA THỰC ĐƠN NGAY
            </span>
            
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform shrink-0" />
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
