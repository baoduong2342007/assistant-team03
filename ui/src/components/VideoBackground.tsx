import React, { useState, useRef } from 'react';
import { Volume2, VolumeX, Sparkles, Flame } from 'lucide-react';

interface CookingScene {
  id: string;
  name: string;
  url: string;
  fallbackImage: string;
  description: string;
}

const COOKING_SCENES: CookingScene[] = [
  {
    id: 'braised-fish',
    name: 'Nồi cá kho sôi nhẹ',
    // High quality royalty-free culinary videos
    url: 'https://assets.mixkit.co/videos/preview/mixkit-vegetables-and-meat-simmering-in-a-pan-43403-large.mp4',
    fallbackImage: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=1600&auto=format&fit=crop&q=80',
    description: 'Nước sốt sánh màu cánh gián sôi liu riu, tiêu thơm dậy mùi ấm cúng',
  },
  {
    id: 'steaming-meal',
    name: 'Mâm cơm bốc khói',
    url: 'https://assets.mixkit.co/videos/preview/mixkit-person-pouring-vegetable-soup-into-a-bowl-43402-large.mp4',
    fallbackImage: 'https://images.unsplash.com/photo-1476718406336-bb5a9690ee2a?w=1600&auto=format&fit=crop&q=80',
    description: 'Bát canh nóng hổi khói nghi ngút, chuẩn vị mâm cơm chiều sum vầy',
  },
  {
    id: 'fresh-greens',
    name: 'Rau xanh tươi & Sơ chế',
    url: 'https://assets.mixkit.co/videos/preview/mixkit-chef-cutting-vegetables-with-a-sharp-knife-43399-large.mp4',
    fallbackImage: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=1600&auto=format&fit=crop&q=80',
    description: 'Rau cải non mướt, tươi giòn sạch mọng nước, giàu chất xơ',
  },
];

export const VideoBackground: React.FC = () => {
  const [activeSceneIndex, setActiveSceneIndex] = useState(0);
  const [isMuted, setIsMuted] = useState(true);
  const [videoLoaded, setVideoLoaded] = useState(false);
  const [videoError, setVideoError] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  const activeScene = COOKING_SCENES[activeSceneIndex];

  return (
    <div className="relative w-full h-[52vh] sm:h-[55vh] min-h-[380px] max-h-[580px] overflow-hidden select-none">
      {/* Video Element */}
      <video
        ref={videoRef}
        key={activeScene.url}
        autoPlay
        loop
        muted={isMuted}
        playsInline
        onLoadedData={() => setVideoLoaded(true)}
        onError={() => setVideoError(true)}
        className="absolute inset-0 w-full h-full object-cover object-center scale-105 transform motion-safe:transition-transform duration-1000 ease-out"
        poster={activeScene.fallbackImage}
      >
        <source src={activeScene.url} type="video/mp4" />
      </video>

      {/* Fallback image layer if video fails or while loading */}
      {(!videoLoaded || videoError) && (
        <div
          className="absolute inset-0 bg-cover bg-center transition-opacity duration-700"
          style={{ backgroundImage: `url(${activeScene.fallbackImage})` }}
        />
      )}

      {/* Realistic Rising Steam effect layers */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden z-10 flex items-center justify-center opacity-40">
        <div className="w-48 h-48 rounded-full bg-white/20 filter blur-2xl animate-steam-1 absolute -bottom-10 left-1/3" />
        <div className="w-56 h-56 rounded-full bg-white/15 filter blur-3xl animate-steam-2 absolute -bottom-8 left-1/2" />
        <div className="w-40 h-40 rounded-full bg-amber-100/20 filter blur-2xl animate-steam-3 absolute -bottom-12 right-1/3" />
      </div>

      {/* Top subtle shade for floating navbar readability */}
      <div className="absolute top-0 left-0 right-0 h-28 bg-gradient-to-b from-black/40 via-black/15 to-transparent z-10 pointer-events-none" />

      {/* Ambient warm vignette for cinematic Mona Coffee feel */}
      <div className="absolute inset-0 bg-amber-950/15 mix-blend-multiply pointer-events-none z-10" />

      {/* Bottom Gradient Fade: Mờ dần từ video sang nền sáng #FDFBF7 */}
      <div
        className="absolute inset-0 z-20 pointer-events-none"
        style={{
          background: 'linear-gradient(to bottom, rgba(0,0,0,0.05) 0%, rgba(253,251,247,0) 35%, rgba(253,251,247,0.65) 70%, #FDFBF7 100%)',
        }}
      />

      {/* Top-Right Scene Switcher & Mute pill */}
      <div className="absolute top-20 right-4 sm:right-8 z-30 flex items-center gap-2">
        <div className="bg-black/40 backdrop-blur-md rounded-full p-1 border border-white/20 shadow-lg flex items-center gap-1 text-xs text-white">
          {COOKING_SCENES.map((scene, idx) => (
            <button
              key={scene.id}
              onClick={() => {
                setActiveSceneIndex(idx);
                setVideoLoaded(false);
                setVideoError(false);
              }}
              className={`px-2.5 py-1 rounded-full transition-all text-[11px] sm:text-xs cursor-pointer ${
                activeSceneIndex === idx
                  ? 'bg-white text-stone-900 font-semibold shadow-sm'
                  : 'text-white/80 hover:text-white hover:bg-white/10'
              }`}
              title={scene.description}
            >
              {scene.name}
            </button>
          ))}
        </div>
      </div>

      {/* Bottom Scene description badge */}
      <div className="absolute bottom-6 left-4 sm:left-8 z-30 hidden sm:flex items-center gap-2 text-xs font-medium text-stone-800 bg-white/70 backdrop-blur-md px-3 py-1.5 rounded-full border border-stone-200/60 shadow-sm">
        <Flame className="w-3.5 h-3.5 text-orange-600 animate-pulse" />
        <span>Góc nhìn bàn ăn chuẩn The Mona Coffee: <span className="text-stone-900 font-semibold">{activeScene.name}</span></span>
      </div>
    </div>
  );
};
