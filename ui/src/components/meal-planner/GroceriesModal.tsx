import React, { useState } from 'react';
import {
  X,
  ShoppingBag,
  Check,
  Copy,
  Printer,
  Sparkles,
  DollarSign,
  Info,
} from 'lucide-react';
import { useMealPlanner, GroceryItemDisplay } from '../../context/MealPlannerContext';

interface GroceriesModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const GroceriesModal: React.FC<GroceriesModalProps> = ({ isOpen, onClose }) => {
  const {
    groceryItems,
    toggleGroceryItem,
    markAllGroceries,
    preferences,
    formatPrice,
    stats,
  } = useMealPlanner();

  const [copied, setCopied] = useState(false);
  const [activeCategory, setActiveCategory] = useState<string>('all');

  if (!isOpen) return null;

  // Categories
  const categories = [
    { id: 'all', label: 'Tất Cả' },
    { id: 'produce', label: '🥬 Rau Củ Quả' },
    { id: 'meat', label: '🥩 Thịt & Hải Sản' },
    { id: 'dairy', label: '🥛 Sữa & Trứng' },
    { id: 'pantry', label: '🍚 Gạo & Đồ Khô' },
    { id: 'spice', label: '🧂 Gia Vị' },
  ];

  const filteredItems = groceryItems.filter((item) => {
    if (activeCategory === 'all') return true;
    return item.category === activeCategory;
  });

  // Calculate items already in pantry vs items to buy
  const itemsToBuy = groceryItems.filter((i) => !i.checked);
  const itemsAlreadyHave = groceryItems.filter((i) => i.checked);

  // Adjusted total based on items to buy
  const adjustedTotal = itemsToBuy.reduce((sum, item) => sum + item.estimatedPrice, 0);

  const handleCopyClipboard = () => {
    const text = itemsToBuy
      .map((item) => `- [ ] ${item.name} (${item.amount}) ~ ${formatPrice(item.estimatedPrice)}`)
      .join('\n');
    const fullText = `🛒 DANH SÁCH ĐI CHỢ CHO ${preferences.servingSize} NGƯỜI (${preferences.selectedDays.length} NGÀY):\n\n${text}\n\nTổng ước tính: ${formatPrice(
      adjustedTotal
    )}`;

    navigator.clipboard.writeText(fullText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden border border-stone-200">
        
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-stone-100 flex items-center justify-between bg-stone-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-orange-100 text-orange-700 flex items-center justify-center font-bold">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-xl font-bold font-serif-display text-stone-900">
                Danh Sách Đi Chợ Của Bạn (My Groceries)
              </h2>
              <p className="text-xs text-stone-500">
                Định lượng cho <strong>{preferences.servingSize} người</strong> • {stats.plannedMealsCount} bữa ăn trên lịch tuần
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Total Cost & Quick Actions */}
        <div className="px-6 py-4 bg-orange-50/60 border-b border-orange-100 flex items-center justify-between flex-wrap gap-3">
          <div>
            <div className="text-xs text-stone-500 font-semibold">Ước tính cần thanh toán:</div>
            <div className="text-2xl font-bold font-serif-display text-orange-700">
              {formatPrice(adjustedTotal || stats.estimatedCartTotal)}
            </div>
            <div className="text-[11px] text-stone-500">
              Đã trừ {itemsAlreadyHave.length} món bạn đã có sẵn ở nhà
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleCopyClipboard}
              className="px-3.5 py-2 rounded-xl bg-white border border-stone-200 text-stone-700 hover:text-stone-900 text-xs font-bold flex items-center gap-1.5 shadow-xs transition-colors cursor-pointer"
            >
              <Copy className="w-3.5 h-3.5" />
              <span>{copied ? 'Đã sao chép!' : 'Chép vào điện thoại'}</span>
            </button>
            <button
              type="button"
              onClick={() => window.print()}
              className="px-3.5 py-2 rounded-xl bg-white border border-stone-200 text-stone-700 hover:text-stone-900 text-xs font-bold flex items-center gap-1.5 shadow-xs transition-colors cursor-pointer"
            >
              <Printer className="w-3.5 h-3.5" />
              <span>In ra giấy</span>
            </button>
          </div>
        </div>

        {/* Category Filters */}
        <div className="px-6 py-2.5 border-b border-stone-100 flex items-center gap-2 overflow-x-auto">
          {categories.map((cat) => (
            <button
              key={cat.id}
              type="button"
              onClick={() => setActiveCategory(cat.id)}
              className={`px-3 py-1 rounded-full text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                activeCategory === cat.id
                  ? 'bg-[#014043] text-white shadow-xs'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        {/* Grocery Item List */}
        <div className="flex-1 overflow-y-auto p-6 space-y-2">
          {filteredItems.length === 0 ? (
            <div className="text-center py-12 text-stone-400 text-sm">
              Chưa có nguyên liệu nào trong nhóm này.
            </div>
          ) : (
            filteredItems.map((item) => {
              return (
                <div
                  key={item.id}
                  onClick={() => toggleGroceryItem(item.id)}
                  className={`p-3 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
                    item.checked
                      ? 'bg-stone-50 border-stone-200 opacity-60'
                      : 'bg-white border-stone-200/80 hover:border-orange-300 shadow-xs'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-5 h-5 rounded-md flex items-center justify-center border transition-all ${
                        item.checked
                          ? 'bg-emerald-600 border-emerald-600 text-white'
                          : 'border-stone-300 bg-white'
                      }`}
                    >
                      {item.checked && <Check className="w-3.5 h-3.5" />}
                    </div>

                    <div>
                      <div
                        className={`text-xs sm:text-sm font-bold text-stone-800 ${
                          item.checked ? 'line-through text-stone-400' : ''
                        }`}
                      >
                        {item.name}
                      </div>
                      <div className="text-[11px] text-stone-400">
                        {item.amount} • Dùng cho: {item.recipeSource}
                      </div>
                    </div>
                  </div>

                  <div className="text-right">
                    <div
                      className={`text-xs font-bold ${
                        item.checked ? 'text-stone-400 line-through' : 'text-stone-700'
                      }`}
                    >
                      ~ {formatPrice(item.estimatedPrice)}
                    </div>
                    <div className="text-[10px] text-stone-400">
                      {item.checked ? 'Đã có ở nhà' : 'Cần mua'}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer Actions */}
        <div className="p-4 sm:p-5 border-t border-stone-100 bg-stone-50/50 flex items-center justify-between flex-wrap gap-2">
          <div className="text-xs text-stone-500">
            Mẹo: Nhấn vào món đã có sẵn trong tủ bếp để trừ ra khỏi hóa đơn đi chợ.
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-6 py-2.5 rounded-full bg-[#014043] hover:bg-[#002f32] text-white text-xs sm:text-sm font-bold transition-all cursor-pointer shadow-xs"
          >
            Đã Xong
          </button>
        </div>

      </div>
    </div>
  );
};
