import React, { useState } from 'react';
import { ChefHat, Heart, ShieldCheck, Mail, Sparkles } from 'lucide-react';

export const BepNhaMinhFooter: React.FC = () => {
  const [email, setEmail] = useState('');
  const [subscribed, setSubscribed] = useState(false);

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    if (email) {
      setSubscribed(true);
      setTimeout(() => {
        setSubscribed(false);
        setEmail('');
      }, 3000);
    }
  };

  return (
    <footer className="bg-[#1f2937] text-white pt-12 pb-8 border-t border-stone-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8 pb-10 border-b border-stone-800">
          
          {/* Brand info (Col 1-5) */}
          <div className="md:col-span-5 space-y-4">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-amber-600 via-orange-500 to-amber-500 text-white flex items-center justify-center font-bold text-lg shadow-sm">
                🍲
              </div>
              <div>
                <span className="font-extrabold text-lg tracking-tight uppercase">
                  BẾP NHÀ MÌNH
                </span>
                <span className="block text-[10px] font-semibold text-amber-400 tracking-wider uppercase">
                  AI Dinh Dưỡng Gia Đình Việt
                </span>
              </div>
            </div>

            <p className="text-xs text-stone-400 leading-relaxed max-w-sm">
              Trợ lý bữa ăn và dinh dưỡng gia đình thông minh: mâm cơm hợp bệnh lý từng thành viên, cân đối 4 nhóm chất và timeline nấu nướng tính ngược thảnh thơi.
            </p>

            <div className="flex items-center gap-3 text-xs text-stone-400 pt-1">
              <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Chuẩn Viện Dinh Dưỡng QG</span>
              </span>
              <span>•</span>
              <span>Thông tư 43/2014/TT-BYT</span>
            </div>
          </div>

          {/* Navigation Links (Col 6-8) */}
          <div className="md:col-span-3 space-y-2.5 text-xs">
            <h4 className="font-bold text-white text-sm mb-3">Tính Năng Chính</h4>
            <div><a href="/meal-planner" className="text-stone-400 hover:text-orange-400 transition-colors">Lập Kế Hoạch Bữa Ăn</a></div>
            <div><a href="/meal-planner/preference" className="text-stone-400 hover:text-orange-400 transition-colors">Khảo Sát Khẩu Vị & Bệnh Lý</a></div>
            <div><a href="/meal-planner/app" className="text-stone-400 hover:text-orange-400 transition-colors">Lịch Thực Đơn Tuần (Kéo - Thả)</a></div>
            <div><a href="/chat" className="text-stone-400 hover:text-orange-400 transition-colors">Trò Chuyện Cùng Bếp Trưởng AI</a></div>
          </div>

          {/* Newsletter (Col 9-12) */}
          <div className="md:col-span-4 space-y-3">
            <h4 className="font-bold text-white text-sm mb-2">Đăng Ký Nhận Thực Đơn Mẫu</h4>
            <p className="text-xs text-stone-400">
              Nhận gợi ý mâm cơm ngon mỗi tuần và mẹo bảo vệ sức khỏe cho cả gia đình.
            </p>

            <form onSubmit={handleSubscribe} className="flex gap-2 pt-1">
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Nhập địa chỉ email của bạn..."
                className="bg-stone-800 border border-stone-700 text-white placeholder-stone-500 text-xs px-3.5 py-2.5 rounded-xl outline-none flex-1 focus:border-orange-500"
              />
              <button
                type="submit"
                className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-orange-600 to-amber-600 text-white font-bold text-xs shrink-0 cursor-pointer hover:from-orange-700 hover:to-amber-700 transition-all shadow-xs"
              >
                {subscribed ? '✓ Đã nhận' : 'ĐĂNG KÝ'}
              </button>
            </form>
          </div>

        </div>

        {/* Bottom copyright */}
        <div className="pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-stone-500">
          <div>
            © 2026 Bếp Nhà Mình. Đồng hành cùng bữa cơm gia đình Việt thảnh thơi và trọn vị.
          </div>
          <div className="flex items-center gap-4">
            <a href="#dieukhoan" className="hover:text-stone-300">Điều khoản sử dụng</a>
            <a href="#baomat" className="hover:text-stone-300">Chính sách bảo mật</a>
            <a href="#lienhe" className="hover:text-stone-300">Liên hệ góp ý</a>
          </div>
        </div>

      </div>
    </footer>
  );
};
