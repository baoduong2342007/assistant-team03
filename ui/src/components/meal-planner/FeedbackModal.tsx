import React, { useState } from 'react';
import { X, Star, Send, Heart, CheckCircle2 } from 'lucide-react';

interface FeedbackModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const FeedbackModal: React.FC<FeedbackModalProps> = ({ isOpen, onClose }) => {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submitted, setSubmitted] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setTimeout(() => {
      setSubmitted(false);
      onClose();
    }, 1500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl overflow-hidden border border-stone-200">
        
        {/* Header */}
        <div className="p-5 border-b border-stone-100 flex items-center justify-between bg-stone-50/50">
          <div className="flex items-center gap-2 text-stone-900 font-bold font-serif-display text-lg">
            <Heart className="w-5 h-5 text-rose-500 fill-rose-500" />
            <span>Góp Ý & Phản Hồi</span>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {submitted ? (
          <div className="p-8 text-center space-y-3">
            <div className="w-14 h-14 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-bold text-stone-900">Cảm ơn bạn rất nhiều!</h3>
            <p className="text-xs text-stone-500">
              Ý kiến của bạn giúp Bếp ngày càng hoàn thiện tính năng lập kế hoạch bữa ăn tốt hơn.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            <div>
              <label className="block text-xs font-bold text-stone-700 mb-2 text-center">
                Bạn đánh giá trải nghiệm Lập Kế Hoạch Bữa Ăn như thế nào?
              </label>

              {/* 5-star rating */}
              <div className="flex justify-center gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className="p-1 hover:scale-125 transition-transform cursor-pointer"
                  >
                    <Star
                      className={`w-7 h-7 ${
                        star <= rating
                          ? 'fill-amber-400 text-amber-400'
                          : 'fill-stone-200 text-stone-300'
                      }`}
                    />
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-stone-700 mb-1.5">
                Ý kiến đóng góp hoặc tính năng bạn muốn có thêm:
              </label>
              <textarea
                rows={3}
                required
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Ví dụ: Tôi muốn xuất danh sách đi chợ sang Zalo..."
                className="w-full bg-stone-50 p-3 text-xs sm:text-sm rounded-xl border border-stone-200 focus:bg-white focus:border-[#014043] outline-none"
              />
            </div>

            <button
              type="submit"
              className="w-full py-3 rounded-2xl bg-[#014043] hover:bg-[#002f32] text-white text-xs sm:text-sm font-bold shadow-md cursor-pointer flex items-center justify-center gap-2 transition-all"
            >
              <Send className="w-4 h-4" />
              <span>GỬI PHẢN HỒI</span>
            </button>
          </form>
        )}

      </div>
    </div>
  );
};
