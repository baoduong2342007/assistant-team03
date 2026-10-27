import React, { useState } from 'react';
import { 
  Send, Mic, MicOff, Camera, Clock, CheckCircle2, AlertTriangle, ShieldCheck, 
  HeartPulse, ChefHat, Sparkles, RefreshCw, ChevronRight, Info, Award, User, 
  Calendar, Flame, Volume2, ArrowLeft, ArrowUpRight
} from 'lucide-react';
import { FamilyProfile, ChatMessage, MealPlan, TimelineStep } from '../types';
import { INITIAL_CHAT_MESSAGES, SAMPLE_MEAL_PLAN, SAMPLE_TIMELINE_STEPS } from '../data/mockData';

interface ChatbotInterfaceProps {
  profile: FamilyProfile;
  onBackToHome: () => void;
  onEditProfile: () => void;
}

export const ChatbotInterface: React.FC<ChatbotInterfaceProps> = ({
  profile,
  onBackToHome,
  onEditProfile,
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>(INITIAL_CHAT_MESSAGES);
  const [inputPrompt, setInputPrompt] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [timelineSteps, setTimelineSteps] = useState<TimelineStep[]>(SAMPLE_TIMELINE_STEPS);
  const [activeTab, setActiveTab] = useState<'chat' | 'timeline' | 'nutrition'>('chat');
  const [selectedMealPlan, setSelectedMealPlan] = useState<MealPlan>(SAMPLE_MEAL_PLAN);

  // Toggle step complete
  const toggleTimelineStep = (id: string) => {
    setTimelineSteps(prev =>
      prev.map(step => (step.id === id ? { ...step, completed: !step.completed } : step))
    );
  };

  const handleSendMessage = (textToSend?: string) => {
    const text = textToSend || inputPrompt;
    if (!text.trim()) return;

    const userMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      sender: 'user',
      text: text,
      timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages(prev => [...prev, userMsg]);
    setInputPrompt('');

    // Simulated Smart Vietnamese Nutritionist Assistant response
    setTimeout(() => {
      let replyText = '';
      let replyMealPlan: MealPlan | undefined = undefined;
      let replyTimeline: TimelineStep[] | undefined = undefined;
      let suggestions: string[] = [];
      let medicalAlert: any = undefined;

      const lower = text.toLowerCase();

      if (lower.includes('gout') || lower.includes('axit uric')) {
        replyText = `Dạ theo khuyến nghị của Viện Dinh Dưỡng: Người bị Gout cần hạn chế thực phẩm giàu purin (nội tạng, hải sản có vỏ, thịt đỏ đậm). Với món cá lóc đồng kho, hàm lượng purin ở mức trung bình (50-150mg/100g), Ba vẫn ăn được 1-2 khúc nhỏ với điều kiện:\n1. Nấu chín kỹ, bỏ nước luộc đầu.\n2. Ăn kèm nhiều rau xanh (cải ngọt, bí đỏ) và uống đủ 2-2.5 lít nước để tăng đào thải axit uric.`;
        medicalAlert = {
          title: 'Quy tắc an toàn Gout (Viện Dinh Dưỡng)',
          content: 'Tránh hoàn toàn lòng non, óc, gan, tôm cua biển đậm và bia rượu. Cá đồng nước ngọt như cá lóc an toàn hơn nhiều so với hải sản sâu.',
          source: 'Tài liệu Chế độ ăn cho bệnh Gout - Bệnh viện Bạch Mai & Viện Dinh Dưỡng Quốc Gia',
        };
        suggestions = ['Đổi món cá sang đậu hũ non sốt nấm', 'Xem lượng nước cần uống hôm nay', 'Kiểm tra món canh cua'];
      } else if (lower.includes('đổi') || lower.includes('thay thế') || lower.includes('thanh nhiệt')) {
        replyText = `Bếp đã tính toán lại để đổi món canh cho mâm cơm thanh mát hơn:\n• Món mặn giữ nguyên: Cá lóc đồng kho tộ (giảm mặn cho Ba).\n• Canh mới: Canh mướp hương nấu nấm rơm & tôm chay thanh nhiệt, không cholesterol.\n• Rau: Cải ngọt luộc giữ trọn vitamin.\n\nThời gian nấu vẫn đúng 45 phút, kịp dọn mâm lúc ${profile.dinnerTime}!`;
        suggestions = ['Xem timeline mới', 'Cách nấu canh mướp không bị đen', 'Gợi ý món tráng miệng'];
      } else if (lower.includes('timeline') || lower.includes('giờ') || lower.includes('muộn')) {
        replyText = `Bếp đã tự động dịch chuyển toàn bộ Timeline nấu theo giờ ăn mới của gia đình!\n• 17:15: Rã đông cá lóc\n• 18:00: Bắt đầu sơ chế\n• 18:20: Bắt đầu kho cá\n• 18:30: Bấm cơm\n• 19:15: Dọn mâm tròn vị!`;
        suggestions = ['Cài chuông nhắc việc', 'Món nào có thể chuẩn bị từ sáng?'];
      } else {
        replyText = `Dạ Bếp Nhà Mình đã ghi nhận yêu cầu của chị ${profile.cookName}! Mâm cơm được tự động cân đối: 625 kcal/người, 28g protein sạch, natri kiểm soát dưới 735mg an toàn tuyệt đối cho người huyết áp cao. Chị có muốn Bếp xuất danh sách đi chợ hay hẹn giờ nhắc rã đông không ạ?`;
        suggestions = ['Xuất danh sách đi chợ', 'Xem công thức ướp cá ít muối', 'Ba huyết áp cao uống trà gì sau ăn?'];
      }

      const botMsg: ChatMessage = {
        id: `msg-${Date.now() + 1}`,
        sender: 'assistant',
        text: replyText,
        timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        mealPlan: replyMealPlan,
        timeline: replyTimeline,
        suggestions: suggestions,
        medicalAlert: medicalAlert,
      };

      setMessages(prev => [...prev, botMsg]);
    }, 700);
  };

  return (
    <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col pt-16">
      
      {/* Top Header Bar */}
      <header className="bg-white/90 backdrop-blur-md border-b border-stone-200/80 px-4 sm:px-8 py-3 sticky top-0 z-40 shadow-xs">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-3">
          
          {/* Left: Chatbot identity & back link */}
          <div className="flex items-center gap-3">
            <button
              onClick={onBackToHome}
              className="w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center transition-colors cursor-pointer"
              title="Về trang chủ"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>

            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-orange-600 to-amber-500 text-white flex items-center justify-center shadow-sm">
              <span className="text-xl">🍲</span>
            </div>

            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-stone-900 text-base sm:text-lg">
                  Bếp Nhà Mình AI
                </h1>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                  Đang trực tuyến
                </span>
              </div>
              <p className="text-xs text-stone-500">
                Trợ lý dinh dưỡng gia đình chuẩn Thông tư 43/2014/TT-BYT
              </p>
            </div>
          </div>

          {/* Right: Active Profile summary badge */}
          <div className="flex items-center gap-2 flex-wrap">
            <div className="bg-amber-50/90 border border-amber-200/80 rounded-2xl px-3 py-1.5 flex items-center gap-2 text-xs">
              <User className="w-3.5 h-3.5 text-amber-700" />
              <span className="font-bold text-stone-900">{profile.cookName}</span>
              <span className="text-stone-300">•</span>
              <span className="text-stone-600">{profile.memberCount} thành viên</span>
              <span className="text-stone-300">•</span>
              <span className="text-rose-700 font-semibold bg-rose-50 px-1.5 py-0.5 rounded border border-rose-200/60 text-[11px]">
                Ba: Huyết áp cao
              </span>
              <span className="text-stone-300">•</span>
              <span className="text-orange-700 font-semibold">Giờ ăn: {profile.dinnerTime}</span>
            </div>

            <button
              onClick={onEditProfile}
              className="px-3 py-1.5 rounded-xl text-xs font-semibold bg-white border border-stone-200 hover:bg-stone-50 text-stone-700 transition-colors cursor-pointer"
            >
              Sửa hồ sơ
            </button>
          </div>

        </div>
      </header>

      {/* Main Content Area: Chat area on left, Timeline & Nutrition on right */}
      <div className="flex-1 max-w-7xl mx-auto w-full p-4 sm:p-6 grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* ================= LEFT / MAIN: CHAT STREAM (8 cols) ================= */}
        <main className="lg:col-span-7 xl:col-span-8 flex flex-col bg-white rounded-3xl border border-stone-200/80 shadow-sm overflow-hidden h-[calc(100vh-140px)]">
          
          {/* Chat Stream Header */}
          <div className="px-5 py-3 border-b border-stone-100 flex items-center justify-between bg-stone-50/60">
            <div className="flex items-center gap-2 text-xs font-semibold text-stone-600">
              <Sparkles className="w-4 h-4 text-amber-600" />
              <span>Hội thoại dinh dưỡng & Đề xuất mâm cơm thông minh</span>
            </div>
            <div className="text-[11px] text-stone-400">
              Mô hình tối ưu ràng buộc bệnh lý v2.6
            </div>
          </div>

          {/* Messages List */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex gap-3 ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                {/* Assistant Avatar */}
                {msg.sender === 'assistant' && (
                  <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-600 to-orange-500 text-white flex items-center justify-center shrink-0 mt-1 shadow-xs text-sm">
                    🍲
                  </div>
                )}

                <div className={`space-y-3 max-w-2xl ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}>
                  
                  {/* Bubble Text */}
                  <div
                    className={`p-4 rounded-3xl text-sm leading-relaxed ${
                      msg.sender === 'user'
                        ? 'bg-gradient-to-r from-orange-600 to-amber-600 text-white rounded-tr-xs shadow-sm font-medium'
                        : 'bg-[#FAF7F2] text-stone-800 border border-stone-200/70 rounded-tl-xs'
                    }`}
                  >
                    <p className="whitespace-pre-line">{msg.text}</p>
                    <span className={`block text-[10px] mt-1.5 ${msg.sender === 'user' ? 'text-orange-200 text-right' : 'text-stone-400'}`}>
                      {msg.timestamp}
                    </span>
                  </div>

                  {/* Medical Alert Card if present */}
                  {msg.medicalAlert && (
                    <div className="p-4 rounded-2xl bg-rose-50/90 border border-rose-200/80 text-rose-950 text-xs space-y-1.5 shadow-xs">
                      <div className="flex items-center gap-1.5 font-bold text-rose-800">
                        <HeartPulse className="w-4 h-4 text-rose-600" />
                        <span>{msg.medicalAlert.title}</span>
                      </div>
                      <p className="text-rose-900 leading-relaxed">
                        {msg.medicalAlert.content}
                      </p>
                      <span className="text-[10px] text-rose-700/80 block italic">
                        Nguồn: {msg.medicalAlert.source}
                      </span>
                    </div>
                  )}

                  {/* EMBEDDED MEAL PLAN CARD (as requested in wireframe and PDF) */}
                  {msg.mealPlan && (
                    <div className="bg-white rounded-3xl border border-stone-200 shadow-md overflow-hidden p-5 space-y-4">
                      
                      {/* Meal Header */}
                      <div className="flex items-start justify-between border-b border-stone-100 pb-3">
                        <div>
                          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-orange-100 text-orange-800 uppercase tracking-wide">
                            MÂM CƠM TỐI NAY
                          </div>
                          <h3 className="text-lg font-bold text-stone-900 mt-1">
                            {msg.mealPlan.title}
                          </h3>
                          <p className="text-xs text-stone-500">
                            {msg.mealPlan.tagline}
                          </p>
                        </div>
                        <div className="text-right">
                          <span className="text-xs font-semibold text-stone-400 block">Dự toán</span>
                          <span className="text-sm font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-lg border border-emerald-100">
                            {msg.mealPlan.estimatedCost}
                          </span>
                        </div>
                      </div>

                      {/* 3-Layer Safety Verification Stamp (from PDF page 2 & 8) */}
                      <div className="p-3 rounded-2xl bg-emerald-50/70 border border-emerald-200/70 text-xs space-y-1.5">
                        <div className="flex items-center gap-1.5 font-bold text-emerald-900">
                          <ShieldCheck className="w-4 h-4 text-emerald-700" />
                          <span>Hệ thống kiểm tra 3 lớp an toàn:</span>
                        </div>
                        <ul className="text-emerald-950 space-y-1 text-[11px]">
                          <li className="flex items-center gap-1.5">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                            <span><strong>Dị ứng:</strong> {msg.mealPlan.safetyCheck.allergyResult}</span>
                          </li>
                          <li className="flex items-center gap-1.5">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                            <span><strong>Quy tắc bệnh:</strong> {msg.mealPlan.safetyCheck.conditionResult}</span>
                          </li>
                          <li className="flex items-center gap-1.5">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                            <span><strong>Cân đối chất:</strong> {msg.mealPlan.safetyCheck.nutritionResult}</span>
                          </li>
                        </ul>
                      </div>

                      {/* Dishes List */}
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                        {msg.mealPlan.items.map((dish) => (
                          <div
                            key={dish.id}
                            className="p-3 rounded-2xl bg-stone-50/90 border border-stone-200/70 flex flex-col justify-between space-y-2 hover:bg-stone-100/70 transition-colors"
                          >
                            <div className="flex gap-3">
                              <img
                                src={dish.imageUrl}
                                alt={dish.name}
                                className="w-16 h-16 rounded-xl object-cover shrink-0 border border-stone-200"
                              />
                              <div className="space-y-0.5">
                                <span className="text-[10px] font-bold text-orange-600 uppercase">
                                  {dish.category} • {dish.cookingMinutes}p
                                </span>
                                <h4 className="font-bold text-stone-900 text-xs sm:text-sm leading-tight">
                                  {dish.name}
                                </h4>
                                <span className="text-[11px] text-stone-500 block line-clamp-1">
                                  {dish.description}
                                </span>
                              </div>
                            </div>

                            {/* Medical adjustment note */}
                            <div className="bg-amber-100/60 border border-amber-200/70 rounded-xl p-2 text-[11px] text-amber-900 leading-snug">
                              <span className="font-bold text-amber-950">Lưu ý Ba: </span>
                              {dish.pathologyAdjustments}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Quick nutrition pills */}
                      <div className="flex flex-wrap items-center justify-between pt-2 border-t border-stone-100 text-xs text-stone-600">
                        <div className="flex items-center gap-3">
                          <span>Năng lượng: <strong className="text-stone-900">{msg.mealPlan.nutritionSummary.totalCalories} kcal</strong></span>
                          <span>Đạm: <strong className="text-stone-900">{msg.mealPlan.nutritionSummary.totalProtein}g</strong></span>
                        </div>
                        <span className="text-[11px] text-emerald-700 font-medium">
                          {msg.mealPlan.nutritionSummary.sodiumWarning}
                        </span>
                      </div>

                    </div>
                  )}

                  {/* Suggestion Chips */}
                  {msg.suggestions && msg.suggestions.length > 0 && (
                    <div className="flex flex-wrap gap-2 pt-1">
                      {msg.suggestions.map((sug) => (
                        <button
                          key={sug}
                          onClick={() => handleSendMessage(sug)}
                          className="px-3.5 py-1.5 rounded-full text-xs font-semibold bg-white border border-orange-200 text-stone-700 hover:bg-orange-50 hover:border-orange-300 hover:text-orange-900 shadow-2xs transition-all cursor-pointer flex items-center gap-1"
                        >
                          <span>{sug}</span>
                          <ChevronRight className="w-3 h-3 text-orange-500" />
                        </button>
                      ))}
                    </div>
                  )}

                </div>
              </div>
            ))}
          </div>

          {/* Chat Input Bar */}
          <div className="p-3 sm:p-4 bg-white border-t border-stone-200/80">
            
            {/* Quick Helper Prompts */}
            <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none text-xs text-stone-600">
              <span className="text-[10px] font-bold uppercase text-stone-400 shrink-0">Gợi ý nhanh:</span>
              <button
                onClick={() => handleSendMessage('Tối nay nhà muốn ăn cá kho')}
                className="px-2.5 py-1 rounded-full bg-stone-100 hover:bg-stone-200 shrink-0 cursor-pointer"
              >
                🐟 Nấu cá kho tộ
              </button>
              <button
                onClick={() => handleSendMessage('Ba bị gout ăn cá được không?')}
                className="px-2.5 py-1 rounded-full bg-stone-100 hover:bg-stone-200 shrink-0 cursor-pointer"
              >
                🩺 Ba bị gout ăn cá được không?
              </button>
              <button
                onClick={() => handleSendMessage('Đổi món canh sang rau luộc thanh nhiệt')}
                className="px-2.5 py-1 rounded-full bg-stone-100 hover:bg-stone-200 shrink-0 cursor-pointer"
              >
                🥬 Đổi món canh
              </button>
              <button
                onClick={() => handleSendMessage('Nhà ăn tối lúc 19:00, tính lại timeline')}
                className="px-2.5 py-1 rounded-full bg-stone-100 hover:bg-stone-200 shrink-0 cursor-pointer"
              >
                ⏱️ Dời giờ ăn sang 19:00
              </button>
            </div>

            {/* Main Input Form */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="flex items-center gap-2 bg-[#FDFBF7] rounded-full border border-stone-300/80 p-1.5 sm:p-2 shadow-inner focus-within:border-orange-500 focus-within:ring-2 focus-within:ring-orange-500/10 transition-all"
            >
              <button
                type="button"
                onClick={() => setIsRecording(!isRecording)}
                className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors cursor-pointer ${
                  isRecording
                    ? 'bg-rose-500 text-white animate-pulse'
                    : 'text-stone-500 hover:text-stone-800 hover:bg-stone-200/60'
                }`}
                title={isRecording ? 'Đang nghe... bấm để dừng' : 'Nói với Bếp qua micro'}
              >
                {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              </button>

              <button
                type="button"
                onClick={() => handleSendMessage('AI đọc đơn thuốc/phiếu khám của bác sĩ')}
                className="w-9 h-9 rounded-full flex items-center justify-center text-stone-500 hover:text-stone-800 hover:bg-stone-200/60 transition-colors cursor-pointer hidden sm:flex"
                title="Chụp/tải ảnh lời dặn ăn uống của bác sĩ"
              >
                <Camera className="w-4 h-4" />
              </button>

              <input
                type="text"
                value={inputPrompt}
                onChange={(e) => setInputPrompt(e.target.value)}
                placeholder="Nhắn với Bếp: 'Hôm nay ăn gì?', 'Tối nay muốn ăn cá kho'..."
                className="flex-1 bg-transparent px-3 text-sm text-stone-900 focus:outline-none placeholder:text-stone-400"
              />

              <button
                type="submit"
                disabled={!inputPrompt.trim()}
                className="w-9 h-9 rounded-full bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-700 hover:to-amber-700 text-white flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed shadow-sm transition-all cursor-pointer"
              >
                <Send className="w-4 h-4 ml-0.5" />
              </button>
            </form>

          </div>

        </main>

        {/* ================= RIGHT: TIMELINE NẤU TÍNH NGƯỢC (4 cols) ================= */}
        <aside className="lg:col-span-5 xl:col-span-4 flex flex-col space-y-4">
          
          {/* Timeline Card */}
          <div className="bg-white rounded-3xl border border-stone-200/80 shadow-sm p-5 space-y-4">
            
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-700 flex items-center justify-center">
                  <Clock className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="font-bold text-stone-900 text-sm sm:text-base leading-tight">
                    Timeline Nấu Tính Ngược
                  </h2>
                  <span className="text-[11px] text-stone-500">
                    Đích đến: Dọn mâm lúc {profile.dinnerTime}
                  </span>
                </div>
              </div>

              <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200">
                {timelineSteps.filter(s => s.completed).length}/{timelineSteps.length} việc
              </span>
            </div>

            {/* Checklist items */}
            <div className="space-y-2.5 max-h-[380px] overflow-y-auto pr-1">
              {timelineSteps.map((step) => (
                <div
                  key={step.id}
                  onClick={() => toggleTimelineStep(step.id)}
                  className={`p-3 rounded-2xl border transition-all cursor-pointer select-none ${
                    step.completed
                      ? 'bg-stone-50/70 border-stone-200 opacity-60'
                      : 'bg-[#FCFAF7] border-orange-200/70 hover:border-orange-300 shadow-2xs'
                  }`}
                >
                  <div className="flex items-start gap-2.5">
                    
                    <button
                      type="button"
                      className={`w-5 h-5 rounded-md flex items-center justify-center mt-0.5 transition-colors ${
                        step.completed
                          ? 'bg-emerald-600 text-white'
                          : 'border-2 border-stone-300 hover:border-orange-500'
                      }`}
                    >
                      {step.completed && <CheckCircle2 className="w-4 h-4" />}
                    </button>

                    <div className="flex-1 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className={`text-xs font-bold ${step.completed ? 'line-through text-stone-500' : 'text-stone-900'}`}>
                          {step.time} — {step.action}
                        </span>
                        {step.durationMinutes && (
                          <span className="text-[10px] text-stone-400 font-medium">
                            {step.durationMinutes}p
                          </span>
                        )}
                      </div>

                      <p className="text-[11px] text-stone-600 leading-relaxed">
                        {step.detail}
                      </p>

                      {step.forSpecialMember && (
                        <div className="inline-block px-2 py-0.5 rounded bg-amber-100 text-amber-900 font-semibold text-[10px]">
                          ★ {step.forSpecialMember}
                        </div>
                      )}
                    </div>

                  </div>
                </div>
              ))}
            </div>

            {/* Tip note for busy cooks */}
            <div className="p-3 rounded-2xl bg-orange-50/70 border border-orange-200/70 text-[11px] text-orange-950 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-orange-600 shrink-0" />
              <span>
                <strong>Giao diện 1 tay:</strong> Đang nấu có thể bấm đánh dấu việc đã làm. Đổi món hoặc dời giờ ăn, timeline tự tính lại tức thì!
              </span>
            </div>

          </div>

          {/* Quick Family Nutrition Card */}
          <div className="bg-white rounded-3xl border border-stone-200/80 shadow-sm p-5 space-y-3">
            <h3 className="font-bold text-stone-900 text-xs sm:text-sm uppercase tracking-wide flex items-center gap-1.5">
              <HeartPulse className="w-4 h-4 text-rose-600" />
              <span>Bộ quy tắc bệnh lý đang áp dụng</span>
            </h3>

            <div className="space-y-2 text-xs">
              <div className="p-2.5 rounded-xl bg-stone-50 border border-stone-200/70">
                <div className="flex items-center justify-between font-bold text-stone-800">
                  <span>Ba (Bác Tuấn - 58 tuổi)</span>
                  <span className="text-rose-600 text-[10px] bg-rose-50 px-1.5 py-0.2 rounded border border-rose-200">
                    Huyết áp cao
                  </span>
                </div>
                <p className="text-stone-500 text-[11px] mt-1">
                  • Giảm muối dưới 5g/ngày. Múc riêng trước khi nêm mắm đậm.<br />
                  • Bổ sung bí đỏ, chuối, cam giàu kali giúp cân bằng áp lực tim mạch.
                </p>
              </div>

              <div className="p-2.5 rounded-xl bg-stone-50 border border-stone-200/70">
                <div className="flex items-center justify-between font-bold text-stone-800">
                  <span>Bộ lọc dị ứng</span>
                  <span className="text-emerald-700 text-[10px] bg-emerald-50 px-1.5 py-0.2 rounded border border-emerald-200">
                    Đã bật 100%
                  </span>
                </div>
                <p className="text-stone-500 text-[11px] mt-1">
                  Loại trừ tuyệt đối nguyên liệu kỵ bệnh trước khi gửi vào hệ thống.
                </p>
              </div>
            </div>
          </div>

        </aside>

      </div>

    </div>
  );
};
