import React, { useRef, useState } from 'react';
import { VideoBackground } from './VideoBackground';
import {
  ArrowRight, Play, ShieldCheck, HeartPulse, Clock, Sparkles, ChefHat,
  ChevronLeft, ChevronRight, CheckCircle2, Search
} from 'lucide-react';
import {
  PreferencesIllustration,
  MenuReviewIllustration,
  GroceryShopIllustration,
  CookAndEnjoyIllustration,
} from './HowItWorksIllustrations';

interface HeroLandingProps {
  onBeginNow: () => void;
  onExploreDirectChat: (query?: string) => void;
  onOpenIntro: () => void;
}

interface MealShowcase {
  id: string;
  name: string;
  subtitle: string;
  image: string;
  tags: string[];
  medicalNote: string;
  nutrition: string;
  time: string;
}

const MEAL_SHOWCASE_ITEMS: MealShowcase[] = [
  {
    id: 'meal-1',
    name: 'Mâm Cơm Nam Bộ Chuẩn Vị',
    subtitle: 'Cá bống kho tộ màu cánh gián • Canh chua cá lóc • Rau muống luộc dầm sấu',
    image: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=900&auto=format&fit=crop&q=80',
    tags: ['Đủ 3 món Mặn - Canh - Rau', 'Chuẩn vị truyền thống'],
    medicalNote: 'Tách phần nhạt giảm muối cho Ba trước khi rim mắm đậm',
    nutrition: '620 kcal • 32g Đạm • Giàu chất xơ',
    time: '35 phút',
  },
  {
    id: 'meal-2',
    name: 'Thực Đơn Healthy Giàu Đạm Nạc',
    subtitle: 'Ức gà áp chảo sốt cam mật ong • Canh bí đỏ thịt bằm • Bông cải xanh xào tỏi',
    image: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=900&auto=format&fit=crop&q=80',
    tags: ['Kiểm soát mỡ máu', 'Dưới 35k/phần'],
    medicalNote: 'Ít cholesterol, sử dụng dầu ô-liu, không bột ngọt',
    nutrition: '540 kcal • 42g Đạm • 12g Xơ',
    time: '25 phút',
  },
  {
    id: 'meal-3',
    name: 'Thực Đơn Carb Chậm Cho Tiểu Đường',
    subtitle: 'Cá hồi áp chảo măng tây • Cơm gạo lứt hạt sen • Canh rong biển đậu non',
    image: 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=900&auto=format&fit=crop&q=80',
    tags: ['Chỉ số GI thấp < 55', 'Chuẩn Viện Dinh Dưỡng'],
    medicalNote: 'Hạn chế tăng đột ngột đường huyết sau bữa ăn cho Mẹ',
    nutrition: '510 kcal • Giàu Omega-3 • Không đường cát',
    time: '30 phút',
  },
  {
    id: 'meal-4',
    name: 'Bữa Tối Nhanh Gọn Dưới 20 Phút',
    subtitle: 'Bò xào hoa thiên lý • Canh cải cúc tôm tươi • Đậu phụ non sốt cà chua',
    image: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=900&auto=format&fit=crop&q=80',
    tags: ['Nấu nhanh 20 phút', 'Tiết kiệm thời gian'],
    medicalNote: 'Timeline tính ngược: sơ chế nhanh, xào nấu đồng thời kịp giờ cơm',
    nutrition: '580 kcal • Bổ sung sắt & kẽm tự nhiên',
    time: '20 phút',
  },
  {
    id: 'meal-5',
    name: 'Mâm Cơm Thanh Nhiệt Mùa Hè',
    subtitle: 'Canh cua mồng tơi mướp hương • Cà pháo giòn • Thịt ba chỉ luộc mắm tỏi',
    image: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=900&auto=format&fit=crop&q=80',
    tags: ['Thanh nhiệt giải độc', 'Bữa cơm sum vầy'],
    medicalNote: 'Nhiều canxi tự nhiên từ cua đồng, mát ruột dễ tiêu hóa',
    nutrition: '590 kcal • 100% An toàn dị ứng',
    time: '30 phút',
  },
  {
    id: 'meal-6',
    name: 'Bữa Cơm Tiết Kiệm Dưới 35k/Khẩu Phần',
    subtitle: 'Thịt kho trứng cút • Canh rau ngót nấu thịt bằm • Dưa giá chua ngọt',
    image: 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=900&auto=format&fit=crop&q=80',
    tags: ['Tiết kiệm ngân sách', 'Chỉ 32.000đ/người'],
    medicalNote: 'Nguyên liệu gần gũi, đi chợ dễ dàng, tiết kiệm mà đủ chất',
    nutrition: '560 kcal • Cân bằng dinh dưỡng',
    time: '30 phút',
  },
  {
    id: 'meal-7',
    name: 'Thực Đơn Tốt Cho Tim Mạch & Huyết Áp',
    subtitle: 'Cá chép om dưa giảm mặn • Canh mướp đắng nhồi thịt nạc • Rau lang luộc',
    image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=900&auto=format&fit=crop&q=80',
    tags: ['Giảm muối < 3g', 'Tốt cho huyết áp'],
    medicalNote: 'Đạt chuẩn Thông tư 43/2014/TT-BYT cho người cao tuổi',
    nutrition: '520 kcal • Ít Natri • Giàu Kali',
    time: '35 phút',
  },
  {
    id: 'meal-8',
    name: 'Mâm Cơm Sum Vầy Cuối Tuần',
    subtitle: 'Gà ta hấp lá chanh • Xôi gấc dẻo thơm hạt sen • Canh măng tươi sườn non',
    image: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&auto=format&fit=crop&q=80',
    tags: ['Bữa ăn cuối tuần', 'Đậm đà hương vị Việt'],
    medicalNote: 'Cân bằng đạm và rau xanh, tạo cảm giác ấm cúng sum họp',
    nutrition: '680 kcal • Giàu năng lượng',
    time: '45 phút',
  },
];

export const HeroLanding: React.FC<HeroLandingProps> = ({
  onBeginNow,
  onExploreDirectChat,
  onOpenIntro,
}) => {
  const [cravingQuery, setCravingQuery] = useState('');
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onExploreDirectChat(cravingQuery.trim());
  };

  // Draggable Carousel Mouse State (Kéo chuột qua trái/phải trên hàng ngang các món ăn)
  const carouselRef = useRef<HTMLDivElement>(null);
  const [isMouseDown, setIsMouseDown] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeftState, setScrollLeftState] = useState(0);

  const handleMouseDown = (e: React.MouseEvent) => {
    if (!carouselRef.current) return;
    setIsMouseDown(true);
    setStartX(e.pageX - carouselRef.current.offsetLeft);
    setScrollLeftState(carouselRef.current.scrollLeft);
  };

  const handleMouseLeave = () => {
    setIsMouseDown(false);
  };

  const handleMouseUp = () => {
    setIsMouseDown(false);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isMouseDown || !carouselRef.current) return;
    e.preventDefault();
    const x = e.pageX - carouselRef.current.offsetLeft;
    const walk = (x - startX) * 1.6;
    carouselRef.current.scrollLeft = scrollLeftState - walk;
  };

  const scrollCarousel = (direction: 'left' | 'right') => {
    if (!carouselRef.current) return;
    const distance = direction === 'left' ? -420 : 420;
    carouselRef.current.scrollBy({ left: distance, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col justify-between selection:bg-orange-200 selection:text-orange-950">

      {/* =========================================================================
          TẦNG 1: VIDEO YOUTUBE FULL KHUNG HÌNH (Chuẩn 4K MgXXclCbqqs)
          ========================================================================= */}
      <section className="w-full bg-stone-950 relative border-b border-stone-800 shadow-md">
        <div className="w-full h-[calc(100vh-4rem)] sm:h-[calc(100vh-5rem)] min-h-[520px]">
          <iframe
            src="https://www.youtube.com/embed/MgXXclCbqqs?vq=hd2160&enablejsapi=1&rel=0&modestbranding=1&playsinline=1"
            title="Bếp Nhà Mình - Video YouTube 4K Cinematic Food"
            className="w-full h-full object-cover border-0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowFullScreen
          />
        </div>
      </section>

      {/* =========================================================================
          TẦNG 2: FEATURE MÓN ĂN HÀNG NGANG (NẰM GIỮA VIDEO VÀ GIAO DIỆN BAN ĐẦU)
          - Chỉ giữ lại danh sách các món ăn
          - Bố trí hàng ngang cân đối khung hình
          - Người dùng sử dụng chuột nhấn kéo sang trái/phải để chuyển món
          ========================================================================= */}
      <section className="py-12 sm:py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">

        {/* Header hàng món ăn kèm nút điều khiển */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6">
          <div className="text-left space-y-1.5">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-100/90 text-orange-900 text-xs font-bold tracking-wide uppercase">
              <Sparkles className="w-3.5 h-3.5 text-orange-600" />
              <span>GỢI Ý MÂM CƠM HÔM NAY</span>
            </div>
            <h2 className="text-2xl sm:text-3.5xl font-extrabold text-[#1C3D47] tracking-tight">
              Mâm Cơm Gia Đình Chuẩn Vị & Dinh Dưỡng
            </h2>
            <p className="text-stone-500 text-xs sm:text-sm">
              Nhấn giữ và kéo chuột sang trái hoặc phải để khám phá 8 thực đơn đa dạng thiết kế riêng cho từng gia đình
            </p>
          </div>

          <div className="flex items-center gap-2 self-start sm:self-auto shrink-0">
            <span className="hidden md:inline-flex items-center gap-1.5 text-xs font-semibold text-stone-600 bg-white border border-stone-200 px-3.5 py-1.5 rounded-full shadow-2xs">
              <span>👈 Kéo chuột để chuyển món 👉</span>
            </span>
            <button
              onClick={() => scrollCarousel('left')}
              className="w-9 h-9 rounded-full bg-white hover:bg-stone-100 border border-stone-200 flex items-center justify-center text-stone-700 shadow-2xs transition-colors cursor-pointer"
              title="Xem món trước"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => scrollCarousel('right')}
              className="w-9 h-9 rounded-full bg-white hover:bg-stone-100 border border-stone-200 flex items-center justify-center text-stone-700 shadow-2xs transition-colors cursor-pointer"
              title="Xem món tiếp theo"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Khung ngang các món ăn có thể kéo chuột (Mouse Draggable) */}
        <div
          ref={carouselRef}
          onMouseDown={handleMouseDown}
          onMouseLeave={handleMouseLeave}
          onMouseUp={handleMouseUp}
          onMouseMove={handleMouseMove}
          className={`flex gap-5 overflow-x-auto pb-5 pt-1 px-1 scroll-smooth select-none no-scrollbar ${
            isMouseDown ? 'cursor-grabbing' : 'cursor-grab'
          }`}
          style={{
            scrollbarWidth: 'none',
            msOverflowStyle: 'none',
          }}
        >
          {MEAL_SHOWCASE_ITEMS.map((meal, index) => (
            <div
              key={meal.id}
              className="shrink-0 w-[290px] sm:w-[330px] bg-white rounded-3xl border border-[#EBE3D7] shadow-[0_12px_30px_rgba(28,61,71,0.08)] overflow-hidden flex flex-col group transition-transform duration-300 hover:-translate-y-1"
            >
              {/* Ảnh món ăn */}
              <div className="relative h-48 sm:h-52 w-full overflow-hidden bg-stone-100">
                <img
                  src={meal.image}
                  alt={meal.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 pointer-events-none"
                />
                <div className="absolute top-3 left-3 bg-black/60 backdrop-blur-md text-white text-[11px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1">
                  <Clock className="w-3 h-3 text-amber-400" />
                  <span>{meal.time}</span>
                </div>
                <div className="absolute top-3 right-3 bg-white/90 backdrop-blur-md text-[#1C3D47] text-[11px] font-bold px-2.5 py-1 rounded-full shadow-2xs">
                  #{index + 1}
                </div>
              </div>

              {/* Thông tin mâm cơm */}
              <div className="p-5 flex flex-col justify-between grow space-y-3 text-left">
                <div>
                  <div className="flex flex-wrap gap-1.5 mb-2">
                    {meal.tags.map(tag => (
                      <span
                        key={tag}
                        className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-orange-50 text-orange-700 border border-orange-100"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>

                  <h3 className="font-extrabold text-stone-900 text-base sm:text-lg tracking-tight mb-1">
                    {meal.name}
                  </h3>

                  <p className="text-stone-500 text-xs line-clamp-2 leading-relaxed">
                    {meal.subtitle}
                  </p>
                </div>

                <div className="pt-2 border-t border-stone-100 space-y-1.5 text-[11px]">
                  <div className="flex items-center gap-1.5 text-emerald-800 font-medium">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <span className="truncate">{meal.medicalNote}</span>
                  </div>
                  <div className="text-stone-400 font-mono">
                    {meal.nutrition}
                  </div>
                </div>

                <button
                  onClick={onBeginNow}
                  className="w-full py-2.5 rounded-xl font-bold text-xs text-[#1C3D47] bg-stone-100 hover:bg-[#1C3D47] hover:text-white transition-colors cursor-pointer mt-1"
                >
                  Chọn mâm cơm này →
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Chấm phân trang */}
        <div className="flex items-center justify-center gap-1.5 mt-3">
          {MEAL_SHOWCASE_ITEMS.map((item) => (
            <div
              key={item.id}
              className="w-2 h-2 rounded-full bg-stone-300"
            />
          ))}
        </div>

      </section>

      {/* =========================================================================
          TẦNG 3: GIAO DIỆN GỐC BAN ĐẦU (Theo đúng GitHub repo & Ảnh số 2 người dùng tải lên)
          - VideoBackground ẩm thực ấm cúng với 3 khung cảnh nấu nướng
          - Tiêu đề cực lớn ở chính giữa: "Hôm nay nhà mình ăn gì? Để Bếp lo trọn vẹn từng bữa."
          - Nút bấm tròn nổi bật: BEGIN NOW & Xem thử Mâm cơm & Chatbot AI
          - 3 Trụ cột an toàn (Dị ứng, Quy tắc bệnh lý, Timeline)
          - Ví dụ thực tế
          ========================================================================= */}
      <div className="relative overflow-hidden border-t border-stone-200/80">

        {/* Video bìa ẩm thực chạy nền (~50% màn hình, chuyển mờ dần xuống nền sáng) */}
        <VideoBackground />

        {/* Giao diện bắt đầu (Phong cách Antigravity: nền sáng, hạt trang trí nhẹ, chữ cực lớn ở chính giữa) */}
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
        <h1 className="text-4xl sm:text-6xl md:text-7xl font-bold font-serif-display tracking-tight text-stone-900 leading-tight mb-6 sm:mb-8 max-w-4xl">
          <span className="block">Hôm nay nhà mình ăn gì?</span>
          <span className="inline-block mt-0 sm:mt-0.5 pt-1 pb-3 px-1.5 text-3xl sm:text-5xl md:text-6xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-orange-700 via-amber-700 to-amber-600 leading-snug">
            Để Bếp lo trọn vẹn từng bữa.
          </span>
        </h1>

        {/* Bếp Nhà Mình Search Bar (Thay thế dòng chữ trong ngoặc) */}
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
              aria-label="Món ăn muốn tìm"
              className="min-w-0 flex-1 bg-transparent border-none outline-none text-stone-800 placeholder-stone-400 text-sm sm:text-base font-normal"
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
            className="group relative inline-flex items-center justify-center gap-2 sm:gap-3 px-4 sm:px-10 py-4 sm:py-4.5 max-w-full rounded-full text-base sm:text-lg font-bold text-white bg-[#014043] hover:bg-[#002f32] shadow-[0_12px_35px_rgba(1,64,67,0.3)] hover:shadow-[0_16px_45px_rgba(1,64,67,0.45)] active:scale-95 transition-all duration-300 cursor-pointer overflow-hidden border border-teal-800/40"
          >
            {/* Subtle light sweep animation */}
            <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full bg-gradient-to-r from-transparent via-white/20 to-transparent transition-transform duration-1000 ease-in-out" />

            <div className="w-7 h-7 rounded-full bg-white text-[#014043] flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform shrink-0">
              <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
            </div>

            <span className="tracking-wide uppercase font-extrabold text-sm sm:text-base text-center">
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
              onClick={() => onExploreDirectChat()}
              className="shrink-0 px-5 py-2.5 rounded-full text-xs sm:text-sm font-semibold bg-stone-900 text-white hover:bg-stone-800 transition-colors cursor-pointer"
            >
              Trải nghiệm kịch bản này →
            </button>
          </div>

        </section>

      </div>

      {/* =========================================================================
          TẦNG 4: HOW IT WORKS (Quy trình 4 bước đơn giản với 4 icon hình tròn SVG)
          ========================================================================= */}
      <section className="relative z-20 bg-white border-t border-stone-200 py-16 sm:py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto w-full">

          <div className="text-center mb-14 sm:mb-16">
            <h2 className="text-3xl sm:text-4xl lg:text-4.5xl font-extrabold text-[#1C3D47] tracking-tight mb-3">
              How it works
            </h2>
            <p className="text-stone-600 text-sm sm:text-base max-w-xl mx-auto leading-relaxed">
              Quy trình 4 bước đơn giản để có mâm cơm chuẩn vị, an tâm bệnh lý và đúng giờ mỗi ngày
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-6 text-center">

            {/* Bước 1: Tell Us Your Preferences */}
            <div className="flex flex-col items-center group">
              <div className="mb-5 transform group-hover:-translate-y-1 transition-transform duration-300">
                <PreferencesIllustration />
              </div>
              <h3 className="text-lg sm:text-xl font-bold text-stone-900 tracking-tight mb-1.5">
                Tell Us Your Preferences
              </h3>
              <div className="inline-block px-2.5 py-0.5 rounded-md bg-amber-100 text-amber-900 text-[11px] font-bold mb-2">
                1. Chia sẻ nhu cầu gia đình
              </div>
              <p className="text-stone-500 text-xs sm:text-sm leading-relaxed max-w-xs">
                Cho Bếp biết số lượng thành viên, bệnh lý (tiểu đường, huyết áp...), dị ứng và khẩu vị để cá nhân hoá thực đơn riêng cho nhà bạn.
              </p>
            </div>

            {/* Bước 2: Review Your Menu */}
            <div className="flex flex-col items-center group">
              <div className="mb-5 transform group-hover:-translate-y-1 transition-transform duration-300">
                <MenuReviewIllustration />
              </div>
              <h3 className="text-lg sm:text-xl font-bold text-stone-900 tracking-tight mb-1.5">
                Review Your Menu
              </h3>
              <div className="inline-block px-2.5 py-0.5 rounded-md bg-orange-100 text-orange-900 text-[11px] font-bold mb-2">
                2. Duyệt thực đơn & Đổi món
              </div>
              <p className="text-stone-500 text-xs sm:text-sm leading-relaxed max-w-xs">
                Ưng ý mâm cơm? Tuyệt vời! Bạn luôn có thể đổi món canh, mặn, xào bất cứ lúc nào từ kho hàng trăm món ăn Việt phong phú.
              </p>
            </div>

            {/* Bước 3: Optional: Shop Your Groceries Online */}
            <div className="flex flex-col items-center group">
              <div className="mb-5 transform group-hover:-translate-y-1 transition-transform duration-300">
                <GroceryShopIllustration />
              </div>
              <h3 className="text-lg sm:text-xl font-bold text-stone-900 tracking-tight mb-1.5">
                Optional: Shop Your Groceries
              </h3>
              <div className="inline-block px-2.5 py-0.5 rounded-md bg-sky-100 text-sky-900 text-[11px] font-bold mb-2">
                3. Lên danh sách & Đi chợ
              </div>
              <p className="text-stone-500 text-xs sm:text-sm leading-relaxed max-w-xs">
                Tự động xuất danh sách định lượng nguyên liệu cần mua cho cả tuần. Tiết kiệm chi phí, không còn lo thừa thiếu thực phẩm.
              </p>
            </div>

            {/* Bước 4: Cook & Enjoy */}
            <div className="flex flex-col items-center group">
              <div className="mb-5 transform group-hover:-translate-y-1 transition-transform duration-300">
                <CookAndEnjoyIllustration />
              </div>
              <h3 className="text-lg sm:text-xl font-bold text-stone-900 tracking-tight mb-1.5">
                Cook & Enjoy
              </h3>
              <div className="inline-block px-2.5 py-0.5 rounded-md bg-emerald-100 text-emerald-900 text-[11px] font-bold mb-2">
                4. Nấu nướng & Thưởng thức
              </div>
              <p className="text-stone-500 text-xs sm:text-sm leading-relaxed max-w-xs">
                Mỗi món ăn đều có hướng dẫn từng bước và timeline nấu tính ngược (rã đông, sơ chế, kho xào) để đúng giờ dọn mâm tròn vị.
              </p>
            </div>

          </div>

        </div>
      </section>

      {/* =========================================================================
          TẦNG 5: FOOTER
          ========================================================================= */}
      <footer className="border-t border-stone-200 py-6 px-4 text-center text-xs text-stone-500 bg-[#FDFBF7]">
        <p className="max-w-2xl mx-auto leading-relaxed">
          Bếp Nhà Mình — Family Meal Planner AI • Hướng dẫn đối chiếu Viện Dinh Dưỡng Quốc Gia Việt Nam & Thông tư 43/2014/TT-BYT.
        </p>
      </footer>

    </div>
  );
};
