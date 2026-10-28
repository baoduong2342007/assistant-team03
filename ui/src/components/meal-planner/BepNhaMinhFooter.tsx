import React, { useState } from 'react';

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
    <footer className="bg-[#014043] text-stone-300 pt-16 pb-8 border-t border-teal-900/40 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Top 4-Column Grid */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8 lg:gap-12 pb-12 border-b border-white/10">
          
          {/* Newsletter (Col 1-4) */}
          <div className="md:col-span-4 space-y-3">
            <h4 className="font-bold text-white text-sm">Nhận Thực Đơn Mẫu Hàng Tuần</h4>
            <p className="text-stone-300/80 leading-relaxed max-w-sm">
              Đăng ký để nhận ngay gợi ý mâm cơm tròn vị, tiết kiệm chi phí và chuẩn dinh dưỡng mỗi sáng thứ Hai.
            </p>

            <form onSubmit={handleSubscribe} className="pt-1">
              <div className="relative flex items-center max-w-md">
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Nhập địa chỉ email..."
                  className="w-full bg-white text-stone-900 placeholder-stone-400 text-xs px-4 py-2.5 rounded-full outline-none pr-28 border border-white"
                />
                <button
                  type="submit"
                  className="absolute right-1 top-1 bottom-1 px-4 rounded-full bg-[#014043] hover:bg-[#002f32] text-white font-bold text-xs uppercase tracking-wider transition-colors cursor-pointer"
                >
                  {subscribed ? '✓ Đã nhận' : 'ĐĂNG KÝ'}
                </button>
              </div>
            </form>
            <p className="text-[11px] text-stone-400">
              Email của bạn được bảo mật tuyệt đối theo chính sách của Bếp Nhà Mình.
            </p>
          </div>

          {/* Cột 1: Nội Dung & Công Thức (Col 5-6) */}
          <div className="md:col-span-3 space-y-2.5">
            <h4 className="font-bold text-white text-sm mb-3">Nội Dung & Công Thức</h4>
            <div><a href="#recipes" className="hover:text-white transition-colors">Công thức món ngon</a></div>
            <div><a href="#articles" className="hover:text-white transition-colors">Bài viết & Mẹo bếp</a></div>
            <div><a href="#collections" className="hover:text-white transition-colors">Bộ sưu tập theo mùa</a></div>
            <div><a href="#meal-plans" className="hover:text-white transition-colors">Kế hoạch bữa ăn tuần</a></div>
            <div><a href="#kids" className="hover:text-white transition-colors">Món ngon cho bé</a></div>
            <div><a href="#health" className="hover:text-white transition-colors">Dinh dưỡng & Sức khỏe</a></div>
          </div>

          {/* Cột 2: Giải Pháp Bếp Nhà Mình (Col 7-9) */}
          <div className="md:col-span-3 space-y-2.5">
            <h4 className="font-bold text-white text-sm mb-3">Giải Pháp Bếp Nhà Mình</h4>
            <div><a href="/meal-planner" className="hover:text-white transition-colors">Trợ lý thực đơn gia đình</a></div>
            <div><a href="/meal-planner/preference" className="hover:text-white transition-colors">Khảo sát khẩu vị & bệnh lý</a></div>
            <div><a href="/meal-planner/app" className="hover:text-white transition-colors">Bảng kế hoạch tuần (Kéo - Thả)</a></div>
            <div><a href="#groceries" className="hover:text-white transition-colors">Tối ưu giỏ hàng đi chợ</a></div>
            <div><a href="#business" className="hover:text-white transition-colors">Dành cho doanh nghiệp</a></div>
          </div>

          {/* Cột 3: Về Chúng Tôi (Col 10-12) */}
          <div className="md:col-span-2 space-y-2.5">
            <h4 className="font-bold text-white text-sm mb-3">Về Chúng Tôi</h4>
            <div><a href="#about" className="hover:text-white transition-colors">Giới thiệu</a></div>
            <div><a href="#careers" className="hover:text-white transition-colors">Đội ngũ & Sự nghiệp</a></div>
            <div><a href="#press" className="hover:text-white transition-colors">Báo chí</a></div>
            <div><a href="#terms" className="hover:text-white transition-colors">Điều khoản dịch vụ</a></div>
            <div><a href="#privacy" className="hover:text-white transition-colors">Chính sách bảo mật</a></div>
            <div><a href="#faq" className="hover:text-white transition-colors">Hỏi đáp (FAQ)</a></div>
          </div>

        </div>

        {/* Bottom Bar */}
        <div className="pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-stone-400">
          <div>
            © 2026 Bếp Nhà Mình. Giữ toàn bộ bản quyền.
          </div>
          <div className="flex items-center gap-4 flex-wrap">
            <a href="#terms" className="hover:text-white transition-colors">Điều khoản</a>
            <a href="#privacy" className="hover:text-white transition-colors">Chính sách quyền riêng tư</a>
            <a href="#cookies" className="hover:text-white transition-colors">Cài đặt Cookie</a>
            <a href="#accessibility" className="hover:text-white transition-colors">Khả năng tiếp cận</a>
          </div>
        </div>

      </div>
    </footer>
  );
};
