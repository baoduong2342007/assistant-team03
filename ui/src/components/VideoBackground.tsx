import React, { useState, useEffect } from 'react';

interface BackgroundSlide {
  id: string;
  url: string;
}

const BACKGROUND_SLIDES: BackgroundSlide[] = [
  {
    id: 'braised-feast',
    url: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=1920&auto=format&fit=crop&q=85',
  },
  {
    id: 'steaming-soup',
    url: 'https://images.unsplash.com/photo-1476718406336-bb5a9690ee2a?w=1920&auto=format&fit=crop&q=85',
  },
  {
    id: 'fresh-veggies',
    url: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=1920&auto=format&fit=crop&q=85',
  },
  {
    id: 'family-table',
    url: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1920&auto=format&fit=crop&q=85',
  },
];

export const VideoBackground: React.FC = () => {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    // Tự động chuyển cảnh sau 7 giây (nằm trong khoảng 5-10s theo yêu cầu)
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % BACKGROUND_SLIDES.length);
    }, 7000);

    return () => clearInterval(timer);
  }, []);

  return (
    <div className="relative w-full h-[52vh] sm:h-[55vh] min-h-[380px] max-h-[580px] overflow-hidden select-none">
      {/* Background Slideshow với hiệu ứng Ken Burns phóng to nhẹ dần và chuyển cảnh mờ dần (fade in/out) */}
      {BACKGROUND_SLIDES.map((slide, index) => {
        const isActive = index === currentIndex;
        return (
          <div
            key={slide.id}
            className={`absolute inset-0 bg-cover bg-center transition-opacity duration-1000 ease-in-out ${
              isActive ? 'opacity-100 z-10' : 'opacity-0 z-0'
            }`}
          >
            {/* Phóng to nhẹ dần 1.0 -> 1.08 trong 7s */}
            <div
              className={`w-full h-full bg-cover bg-center ${
                isActive ? 'animate-ken-burns' : ''
              }`}
              style={{
                backgroundImage: `url(${slide.url})`,
              }}
            />
          </div>
        );
      })}

      {/* Hiệu ứng khói bốc lên nhẹ nhàng */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden z-10 flex items-center justify-center opacity-30">
        <div className="w-48 h-48 rounded-full bg-white/20 filter blur-2xl animate-steam-1 absolute -bottom-10 left-1/3" />
        <div className="w-56 h-56 rounded-full bg-white/15 filter blur-3xl animate-steam-2 absolute -bottom-8 left-1/2" />
        <div className="w-40 h-40 rounded-full bg-amber-100/20 filter blur-2xl animate-steam-3 absolute -bottom-12 right-1/3" />
      </div>

      {/* Lớp phủ chuyển mờ trên cùng */}
      <div className="absolute top-0 left-0 right-0 h-28 bg-gradient-to-b from-black/30 via-black/10 to-transparent z-10 pointer-events-none" />

      {/* Ám ấm nhẹ phong cách ẩm thực */}
      <div className="absolute inset-0 bg-amber-950/15 mix-blend-multiply pointer-events-none z-10" />

      {/* Lớp chuyển mờ dần xuống màu nền trang #FDFBF7 */}
      <div
        className="absolute inset-0 z-20 pointer-events-none"
        style={{
          background:
            'linear-gradient(to bottom, rgba(0,0,0,0.02) 0%, rgba(253,251,247,0) 35%, rgba(253,251,247,0.7) 70%, #FDFBF7 100%)',
        }}
      />
    </div>
  );
};
