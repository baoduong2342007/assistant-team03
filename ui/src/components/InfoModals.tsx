import React from 'react';
import { X, ShieldCheck, HeartPulse, Sparkles, ChefHat, CheckCircle2, BookOpen, Clock } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const IntroductionModal: React.FC<ModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-[#FDFBF7] text-[#27211D] rounded-3xl max-w-2xl w-full p-6 sm:p-8 border border-stone-200 shadow-2xl relative max-h-[88vh] overflow-y-auto">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-2.5 mb-4">
          <div className="w-10 h-10 rounded-2xl bg-amber-500 text-white flex items-center justify-center text-xl shadow-xs">
            🍲
          </div>
          <div>
            <span className="text-[11px] font-bold uppercase text-orange-600 tracking-wider">
              GIỚI THIỆU DỰ ÁN
            </span>
            <h2 className="text-2xl font-bold font-serif-display text-stone-900">
              Bếp Nhà Mình — Family Meal Planner
            </h2>
          </div>
        </div>

        {/* Core Content from PDF */}
        <div className="space-y-4 text-stone-700 text-sm leading-relaxed">
          <p className="bg-orange-50/80 p-4 rounded-2xl border border-orange-200/70 font-medium text-stone-800">
            Mỗi ngày người nội trợ phải trả lời 3 câu hỏi lớn: <strong>"Hôm nay nấu gì?"</strong>, <strong>"Nấu thế nào cho kịp giờ?"</strong>, và <strong>"Bữa ăn có hợp với người bệnh trong nhà không?"</strong>
          </p>

          <p>
            <strong>Bếp Nhà Mình</strong> dùng AI để biến câu <em>"hôm nay muốn ăn gì"</em> thành một mâm cơm chuẩn vị, hợp sức khỏe từng người (người huyết áp cao, tiểu đường, gout...), kèm <strong>Timeline nấu tính ngược</strong> và lời nhắc việc đúng từng mốc.
          </p>

          <div className="space-y-2 pt-2">
            <h3 className="font-bold text-stone-900 text-sm uppercase tracking-wide flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>3 Lớp An Toàn Sức Khỏe Nghiêm Ngặt</span>
            </h3>
            
            <div className="grid grid-cols-1 gap-2 text-xs">
              <div className="p-3 rounded-xl bg-white border border-stone-200">
                <span className="font-bold text-stone-900 block">1. Dị ứng là bộ lọc tuyệt đối</span>
                <span className="text-stone-500">Code loại bỏ hoàn toàn nguyên liệu gây dị ứng trước khi AI gợi ý món.</span>
              </div>
              <div className="p-3 rounded-xl bg-white border border-stone-200">
                <span className="font-bold text-stone-900 block">2. Quy tắc bệnh lý từ tài liệu chính thống</span>
                <span className="text-stone-500">Đối chiếu Thông tư 43/2014/TT-BYT & Bảng nhu cầu dinh dưỡng Viện Dinh Dưỡng Quốc Gia. AI không tự nghĩ ra quy tắc.</span>
              </div>
              <div className="p-3 rounded-xl bg-white border border-stone-200">
                <span className="font-bold text-stone-900 block">3. Khuyến nghị y khoa tham khảo</span>
                <span className="text-stone-500">Người có bệnh mạn tính nặng, phụ nữ có thai luôn được khuyên tham vấn bác sĩ điều trị.</span>
              </div>
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-amber-50/70 border border-amber-200/70 text-xs text-amber-900 flex items-center gap-2">
            <Clock className="w-4 h-4 text-amber-700 shrink-0" />
            <span>
              <strong>Timeline tính ngược:</strong> Rã đông lúc 16:30, sơ chế 17:15, kho cá 17:35 để đúng 18:30 dọn mâm cơm nóng hổi!
            </span>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-stone-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-full bg-stone-900 text-white font-semibold text-xs hover:bg-stone-800 transition-colors cursor-pointer"
          >
            Đã hiểu & Tiếp tục
          </button>
        </div>

      </div>
    </div>
  );
};

export const AboutModal: React.FC<ModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-[#FDFBF7] text-[#27211D] rounded-3xl max-w-2xl w-full p-6 sm:p-8 border border-stone-200 shadow-2xl relative max-h-[88vh] overflow-y-auto">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-2.5 mb-4">
          <div className="w-10 h-10 rounded-2xl bg-orange-600 text-white flex items-center justify-center text-xl shadow-xs">
            <ChefHat className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] font-bold uppercase text-orange-600 tracking-wider">
              VỀ ĐỘI NGŨ PHÁT TRIỂN
            </span>
            <h2 className="text-2xl font-bold font-serif-display text-stone-900">
              Sứ mệnh vì bữa cơm gia đình Việt
            </h2>
          </div>
        </div>

        {/* Content */}
        <div className="space-y-4 text-stone-700 text-sm leading-relaxed">
          <p>
            Dự án ra đời từ sự thấu hiểu nỗi vất vả của các bà mẹ, người nội trợ bận rộn: Vừa phải đi làm, vừa lo toan bữa cơm gia đình sao cho vừa miệng con trẻ, vừa hợp bệnh lý huyết áp, tiểu đường của ông bà, cha mẹ.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 text-xs">
            <div className="p-3.5 rounded-2xl bg-white border border-stone-200 space-y-1">
              <span className="font-bold text-stone-900 block">AI Integration & RAG</span>
              <p className="text-stone-500">Mô hình ngôn ngữ kết hợp tra cứu bảng thành phần dinh dưỡng và vòng lặp kiểm tra code an toàn.</p>
            </div>

            <div className="p-3.5 rounded-2xl bg-white border border-stone-200 space-y-1">
              <span className="font-bold text-stone-900 block">UI/UX cho người nội trợ</span>
              <p className="text-stone-500">Thao tác một tay khi đang nấu, chữ to rõ ràng, mâm cơm hiển thị sinh động và timeline trực quan.</p>
            </div>

            <div className="p-3.5 rounded-2xl bg-white border border-stone-200 space-y-1">
              <span className="font-bold text-stone-900 block">Dữ liệu & Kiểm thử an toàn</span>
              <p className="text-stone-500">20 bộ hồ sơ mẫu, kiểm tra nghiêm ngặt 0 món vi phạm dị ứng hoặc chống chỉ định bệnh.</p>
            </div>

            <div className="p-3.5 rounded-2xl bg-white border border-stone-200 space-y-1">
              <span className="font-bold text-stone-900 block">Chuẩn Y Khoa Việt Nam</span>
              <p className="text-stone-500">Thông tư 43/2014/TT-BYT & 10 lời khuyên dinh dưỡng hợp lý của Viện Dinh Dưỡng Quốc Gia.</p>
            </div>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-stone-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-full bg-orange-600 text-white font-semibold text-xs hover:bg-orange-700 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>

      </div>
    </div>
  );
};

export const ContactModal: React.FC<ModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-[#FDFBF7] text-[#27211D] rounded-3xl max-w-lg w-full p-6 sm:p-8 border border-stone-200 shadow-2xl relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-2.5 mb-5">
          <div className="w-10 h-10 rounded-2xl bg-sky-600 text-white flex items-center justify-center text-xl shadow-xs">
            📞
          </div>
          <div>
            <span className="text-[11px] font-bold uppercase text-sky-700 tracking-wider">
              LIÊN HỆ & HỖ TRỢ
            </span>
            <h2 className="text-2xl font-bold font-serif-display text-stone-900">
              Bếp Nhà Mình
            </h2>
          </div>
        </div>

        <div className="space-y-3.5 text-stone-700 text-sm">
          <p className="text-stone-600 text-xs sm:text-sm">
            Bạn có câu hỏi, góp ý về thực đơn dinh dưỡng hoặc muốn kết nối cùng đội ngũ phát triển? Hãy liên hệ với chúng tôi:
          </p>

          <div className="p-4 rounded-2xl bg-white border border-stone-200 space-y-2.5 text-xs sm:text-sm">
            <div className="flex items-center justify-between">
              <span className="text-stone-500 font-medium">Email:</span>
              <span className="font-semibold text-stone-900">contact@bepnhaminh.ai.vn</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-stone-500 font-medium">Hotline hỗ trợ:</span>
              <span className="font-semibold text-orange-600">1900 6868 (8:00 - 20:00)</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-stone-500 font-medium">Địa chỉ:</span>
              <span className="font-semibold text-stone-900">227 Nguyễn Văn Cừ, Q.5, TP.HCM</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-stone-500 font-medium">Cộng đồng:</span>
              <span className="font-semibold text-blue-600">facebook.com/bepnhaminh.official</span>
            </div>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-stone-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-full bg-stone-900 text-white font-semibold text-xs hover:bg-stone-800 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};
