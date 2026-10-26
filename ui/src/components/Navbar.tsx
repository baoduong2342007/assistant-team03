import React from 'react';
import { Sparkles, ArrowUpRight, ChefHat, HeartPulse, Info, HelpCircle } from 'lucide-react';

interface NavbarProps {
  currentView: 'landing' | 'onboarding' | 'chat';
  onNavigate: (view: 'landing' | 'onboarding' | 'chat') => void;
  onOpenIntro: () => void;
  onOpenAbout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentView,
  onNavigate,
  onOpenIntro,
  onOpenAbout,
}) => {
  return (
    <header className="fixed top-3 left-0 right-0 z-50 px-4 sm:px-8 max-w-6xl mx-auto">
      <nav className="bg-white/80 backdrop-blur-md border border-[#E9DFD5]/90 rounded-full px-4 sm:px-6 py-2.5 shadow-[0_8px_30px_rgb(0,0,0,0.06)] flex items-center justify-between transition-all duration-300">
        
        {/* Logo */}
        <button
          onClick={() => onNavigate('landing')}
          className="flex items-center gap-2.5 text-left group cursor-pointer focus:outline-none"
        >
          <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-amber-600 to-orange-500 text-white flex items-center justify-center shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform">
            <span className="text-lg">🍲</span>
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-stone-900 tracking-tight text-base sm:text-lg">
                Bếp Nhà Mình
              </span>
              <span className="hidden md:inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-100 text-amber-800 border border-amber-200">
                AI Dinh Dưỡng
              </span>
            </div>
          </div>
        </button>

        {/* Navigation Links */}
        <div className="hidden md:flex items-center gap-1 sm:gap-2 text-sm font-medium text-stone-600">
          <button
            onClick={onOpenIntro}
            className="px-3.5 py-1.5 rounded-full hover:text-stone-900 hover:bg-stone-100/80 transition-colors flex items-center gap-1.5 cursor-pointer"
          >
            <Info className="w-4 h-4 text-amber-600" />
            Introduction
          </button>
          
          <button
            onClick={onOpenAbout}
            className="px-3.5 py-1.5 rounded-full hover:text-stone-900 hover:bg-stone-100/80 transition-colors flex items-center gap-1.5 cursor-pointer"
          >
            <ChefHat className="w-4 h-4 text-orange-600" />
            About us
          </button>

          {currentView !== 'landing' && (
            <button
              onClick={() => onNavigate('landing')}
              className="px-3.5 py-1.5 rounded-full hover:text-stone-900 hover:bg-stone-100/80 transition-colors"
            >
              Trang chủ
            </button>
          )}

          {currentView === 'chat' && (
            <button
              onClick={() => onNavigate('onboarding')}
              className="px-3.5 py-1.5 rounded-full hover:text-stone-900 hover:bg-stone-100/80 transition-colors flex items-center gap-1"
            >
              <HeartPulse className="w-3.5 h-3.5 text-rose-500" />
              Sửa hồ sơ
            </button>
          )}
        </div>

        {/* CTA Button */}
        <div className="flex items-center gap-2">
          {currentView === 'landing' ? (
            <button
              onClick={() => onNavigate('onboarding')}
              className="group relative inline-flex items-center gap-1.5 px-4 sm:px-5 py-2 rounded-full text-xs sm:text-sm font-semibold text-white bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 shadow-md shadow-orange-500/25 transition-all hover:shadow-lg hover:shadow-orange-500/35 active:scale-95 cursor-pointer"
            >
              <span>Bắt đầu</span>
              <ArrowUpRight className="w-4 h-4 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>
          ) : currentView === 'onboarding' ? (
            <button
              onClick={() => onNavigate('chat')}
              className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs sm:text-sm font-semibold text-stone-700 bg-stone-100 hover:bg-stone-200 transition-colors"
            >
              <span>Vào Bếp AI ↗</span>
            </button>
          ) : (
            <button
              onClick={() => onNavigate('landing')}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold text-stone-600 bg-stone-100 hover:bg-stone-200 transition-colors"
            >
              <span>Trang chủ ↗</span>
            </button>
          )}
        </div>

      </nav>
    </header>
  );
};
