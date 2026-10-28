import React, { useState } from 'react';
import {
  Calendar,
  CheckCircle,
  Mail,
  Lock,
  User,
  ArrowRight,
  ShieldCheck,
  Sparkles,
  ShoppingBag,
  Star,
  Check,
  ChefHat,
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { useMealPlanner } from '../../context/MealPlannerContext';

interface MealPlannerAuthProps {
  nextUrl?: string;
  onSuccess: () => void;
  onBackToPreferences: () => void;
}

export const MealPlannerAuth: React.FC<MealPlannerAuthProps> = ({
  nextUrl = '/meal-planner/app',
  onSuccess,
  onBackToPreferences,
}) => {
  const { login, generateWeeklyPlan } = useMealPlanner();

  const [isEmailFormOpen, setIsEmailFormOpen] = useState(false);
  const [email, setEmail] = useState('minhanh@bepnhaminh.vn');
  const [name, setName] = useState('Trần Minh Anh');
  const [password, setPassword] = useState('••••••••••');
  const [receiveInspiration, setReceiveInspiration] = useState(true);
  const [isLoading, setIsLoading] = useState(false);

  const triggerLoginSuccess = (userEmail: string, userName: string) => {
    setIsLoading(true);

    try {
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
      });
    } catch {}

    setTimeout(() => {
      login(userEmail, userName);
      generateWeeklyPlan();
      setIsLoading(false);
      onSuccess();
    }, 600);
  };

  return (
    <div className="min-h-screen bg-white text-[#27211D] flex flex-col justify-between">
      
      {/* Thanh trên cùng */}
      <div className="w-full flex justify-between items-center px-6 sm:px-12 py-4 border-b border-gray-100">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-600 via-orange-500 to-amber-500 text-white flex items-center justify-center font-bold text-base shadow-xs">
            🍲
          </div>
          <span className="font-extrabold text-base tracking-tight text-stone-900 uppercase">
            BẾP NHÀ MÌNH
          </span>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-xs sm:text-sm text-stone-600 font-medium">
            Đã có tài khoản?
          </span>
          <button
            type="button"
            onClick={() => triggerLoginSuccess('thanhvien@bepnhaminh.vn', 'Thành Viên Bếp')}
            className="px-5 py-2 rounded-full border border-stone-300 hover:border-orange-500 hover:text-orange-600 text-stone-800 font-extrabold text-xs tracking-wider uppercase transition-all cursor-pointer"
          >
            ĐĂNG NHẬP
          </button>
        </div>
      </div>

      {/* Màn hình chia đôi 50/50 */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 min-h-[calc(100vh-80px)]">
        
        {/* ====================================================
            CỘT TRÁI (50%): MÂM CƠM GIA ĐÌNH VIỆT & NGHỆ THUẬT NẤU NƯỚNG
           ==================================================== */}
        <div className="lg:col-span-6 bg-[#013B3E] relative overflow-hidden flex flex-col justify-between p-8 sm:p-12 text-white text-left">
          
          <div className="relative z-10 flex-1 flex flex-col items-center justify-center">
            
            {/* Lớp các thẻ công thức xếp chồng */}
            <div className="relative w-full max-w-sm h-72 sm:h-80 mb-6">
              
              {/* Thẻ 1: Cá bống kho tiêu */}
              <div className="absolute top-2 left-4 w-44 sm:w-48 bg-white text-stone-900 rounded-2xl shadow-2xl p-2.5 -rotate-6 border border-white/20">
                <img
                  src="https://images.unsplash.com/photo-1544025162-d76694265947?w=400&auto=format&fit=crop&q=80"
                  alt="Cá kho tộ"
                  className="w-full h-24 rounded-xl object-cover mb-2"
                />
                <div className="text-[11px] font-bold truncate">Cá Bống Kho Tiêu</div>
                <div className="flex items-center justify-between px-0.5 mt-1">
                  <div className="flex gap-0.5 text-amber-400 text-xs">
                    {[...Array(5)].map((_, i) => (
                      <Star key={i} className="w-3 h-3 fill-amber-400 text-amber-400" />
                    ))}
                  </div>
                  <span className="text-[10px] text-stone-400">40 phút</span>
                </div>
              </div>

              {/* Thẻ 2: Canh bí đỏ thịt bằm */}
              <div className="absolute top-8 right-4 w-44 sm:w-48 bg-white text-stone-900 rounded-2xl shadow-2xl p-2.5 rotate-6 border border-white/20">
                <img
                  src="https://images.unsplash.com/photo-1476718406336-bb5a9690ee2a?w=400&auto=format&fit=crop&q=80"
                  alt="Canh bí đỏ"
                  className="w-full h-24 rounded-xl object-cover mb-2"
                />
                <div className="text-[11px] font-bold truncate">Canh Bí Đỏ Thịt Bằm</div>
                <div className="flex items-center justify-between px-0.5 mt-1">
                  <div className="flex gap-0.5 text-amber-400 text-xs">
                    {[...Array(5)].map((_, i) => (
                      <Star key={i} className="w-3 h-3 fill-amber-400 text-amber-400" />
                    ))}
                  </div>
                  <span className="text-[10px] text-stone-400">20 phút</span>
                </div>
              </div>

              {/* Thẻ 3: Cải ngọt luộc kho quẹt */}
              <div className="absolute bottom-6 left-12 w-48 sm:w-52 bg-white text-stone-900 rounded-2xl shadow-2xl p-2.5 -rotate-2 border border-white/20 z-10">
                <img
                  src="https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&auto=format&fit=crop&q=80"
                  alt="Rau cải luộc"
                  className="w-full h-24 rounded-xl object-cover mb-2"
                />
                <div className="text-[11px] font-bold truncate">Cải Ngọt Luộc Chấm Trứng</div>
                <div className="flex items-center justify-between px-0.5 mt-1">
                  <div className="flex gap-0.5 text-amber-400 text-xs">
                    {[...Array(5)].map((_, i) => (
                      <Star key={i} className="w-3 h-3 fill-amber-400 text-amber-400" />
                    ))}
                  </div>
                  <span className="text-[10px] text-stone-400">10 phút</span>
                </div>
              </div>

              {/* Huy hiệu lịch tuần */}
              <div className="absolute bottom-2 left-6 z-20 w-11 h-11 rounded-full bg-gradient-to-tr from-amber-600 to-orange-500 border-2 border-white text-white flex items-center justify-center shadow-lg font-bold text-sm">
                📅
              </div>

              {/* Huy hiệu giỏ hàng */}
              <div className="absolute bottom-1 right-20 z-20 w-11 h-11 rounded-full bg-gradient-to-tr from-amber-600 to-orange-500 border-2 border-white text-white flex items-center justify-center shadow-lg font-bold text-sm">
                🛒
              </div>

            </div>

            {/* Bát canh nóng */}
            <div className="relative mt-auto w-full max-w-sm text-center pt-4">
              <div className="relative inline-block">
                <img
                  src="https://images.unsplash.com/photo-1547592180-85f173990554?w=600&auto=format&fit=crop&q=80"
                  alt="Tô canh chua cá"
                  className="w-56 h-56 sm:w-64 sm:h-64 rounded-full object-cover border-4 border-white/30 shadow-2xl mx-auto"
                />
                <span className="absolute -top-3 left-4 text-3xl">🍲</span>
                <span className="absolute top-10 -right-2 text-2xl">🌿</span>
              </div>
            </div>

          </div>

          <div className="relative z-10 text-center text-xs text-teal-100/70 pt-4">
            Bếp Nhà Mình • Chăm sóc sức khỏe và hạnh phúc gia đình qua từng bữa cơm ngon
          </div>

        </div>

        {/* ====================================================
            CỘT PHẢI (50%): FORM ĐĂNG NHẬP / ĐĂNG KÝ
           ==================================================== */}
        <div className="lg:col-span-6 p-8 sm:p-14 lg:p-20 flex flex-col justify-center max-w-xl mx-auto w-full text-left">
          
          <div className="text-center mb-8">
            <div className="inline-flex items-center gap-2 mb-3">
              <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-600 to-orange-500 text-white flex items-center justify-center font-bold text-base shadow-xs">
                🍲
              </div>
              <span className="font-extrabold text-xl tracking-tight text-stone-900 uppercase">
                BẾP NHÀ MÌNH
              </span>
            </div>

            <h1 className="text-3xl sm:text-4xl font-extrabold text-stone-900 tracking-tight font-serif-display">
              Chào Mừng Đến Với Bếp
            </h1>
            <p className="text-sm text-stone-500 mt-2 font-normal">
              Lưu công thức gia đình, lên thực đơn tuần và chuẩn bị giỏ đi chợ dễ dàng
            </p>
          </div>

          {/* Các nút phương thức đăng nhập */}
          <div className="space-y-3.5 mb-6">
            
            {/* Google */}
            <button
              type="button"
              onClick={() => triggerLoginSuccess('google.user@gmail.com', 'Người dùng Google')}
              disabled={isLoading}
              className="w-full py-3.5 px-6 rounded-full bg-[#3B82F6] hover:bg-[#2563EB] text-white font-extrabold text-xs sm:text-sm tracking-wider uppercase transition-all shadow-md cursor-pointer flex items-center justify-center gap-3 active:scale-[0.99]"
            >
              <span className="w-5 h-5 rounded-full bg-white text-[#3B82F6] flex items-center justify-center font-black text-xs">
                G
              </span>
              <span>TIẾP TỤC VỚI GOOGLE</span>
            </button>

            {/* Facebook */}
            <button
              type="button"
              onClick={() => triggerLoginSuccess('facebook.user@gmail.com', 'Người dùng Facebook')}
              disabled={isLoading}
              className="w-full py-3.5 px-6 rounded-full bg-[#3B5998] hover:bg-[#2d4373] text-white font-extrabold text-xs sm:text-sm tracking-wider uppercase transition-all shadow-md cursor-pointer flex items-center justify-center gap-3 active:scale-[0.99]"
            >
              <span className="font-black text-base lowercase">f</span>
              <span>TIẾP TỤC VỚI FACEBOOK</span>
            </button>

            {/* Email */}
            <button
              type="button"
              onClick={() => setIsEmailFormOpen(!isEmailFormOpen)}
              className="w-full py-3.5 px-6 rounded-full bg-[#014043] hover:bg-[#002f32] text-white font-bold text-xs sm:text-sm tracking-wider uppercase transition-all shadow-sm cursor-pointer flex items-center justify-center gap-2.5 active:scale-[0.99]"
            >
              <Mail className="w-4 h-4" />
              <span>ĐĂNG KÝ BẰNG EMAIL</span>
            </button>

            {/* Form Email mở rộng */}
            {isEmailFormOpen && (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  triggerLoginSuccess(email, name);
                }}
                className="p-4 bg-stone-50 rounded-2xl border border-gray-200 space-y-3 animate-fadeIn mt-2 text-left"
              >
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Họ và tên của bạn</label>
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full bg-white px-3.5 py-2 text-xs rounded-xl border border-gray-300 outline-none focus:border-[#014043]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Địa chỉ Email</label>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full bg-white px-3.5 py-2 text-xs rounded-xl border border-gray-300 outline-none focus:border-[#014043]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Mật khẩu</label>
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-white px-3.5 py-2 text-xs rounded-xl border border-gray-300 outline-none focus:border-[#014043]"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full py-2.5 rounded-full bg-[#014043] hover:bg-[#002f32] text-white text-xs font-bold uppercase tracking-wider cursor-pointer transition-colors"
                >
                  XÁC NHẬN VÀ TIẾP TỤC
                </button>
              </form>
            )}

            {/* Nút đăng nhập nhanh 1-Click Demo */}
            <div className="pt-2 text-center">
              <button
                type="button"
                onClick={() => triggerLoginSuccess('demo@bepnhaminh.vn', 'Thu Trang (Tài khoản mẫu)')}
                className="text-xs text-[#014043] font-bold hover:underline cursor-pointer"
              >
                ⚡ Hoặc trải nghiệm nhanh với tài khoản Demo (1-Click)
              </button>
            </div>

          </div>

          {/* Checkbox nhận thông tin */}
          <div
            onClick={() => setReceiveInspiration(!receiveInspiration)}
            className="flex items-start gap-3 cursor-pointer select-none mb-6"
          >
            <div
              className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                receiveInspiration ? 'bg-[#014043] text-white' : 'border border-gray-400 bg-white'
              }`}
            >
              {receiveInspiration && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className="text-xs text-stone-600 leading-snug">
              Tôi muốn nhận gợi ý món ngon mỗi tuần và cẩm nang dinh dưỡng từ Bếp Nhà Mình!
            </span>
          </div>

          {/* Điều khoản */}
          <p className="text-[11px] text-stone-500 text-center leading-relaxed">
            Bằng việc tiếp tục, bạn đồng ý với{' '}
            <a href="#dieukhoan" className="underline hover:text-stone-800">
              Điều khoản sử dụng
            </a>{' '}
            và{' '}
            <a href="#baomat" className="underline hover:text-stone-800">
              Chính sách bảo mật
            </a>{' '}
            của Bếp Nhà Mình.
          </p>

        </div>

      </div>

    </div>
  );
};
