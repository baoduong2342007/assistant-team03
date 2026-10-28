import React, { useState } from 'react';
import { 
  ArrowLeft, ArrowRight, Check, Sparkles, User, Users, HeartPulse, 
  ShieldAlert, Ban, UtensilsCrossed, Clock, ChefHat, Wallet, FileText, Upload, Plus, Trash2, CheckCircle2 
} from 'lucide-react';
import { FamilyProfile, FamilyMember } from '../types';
import { INITIAL_DEFAULT_PROFILE } from '../data/mockData';

interface OnboardingFlowProps {
  onComplete: (profile: FamilyProfile) => void;
  onBackToLanding: () => void;
}

export const OnboardingFlow: React.FC<OnboardingFlowProps> = ({
  onComplete,
  onBackToLanding,
}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const totalSteps = 6; // Grouped logically into 6 delightful screens covering all 10 PDF questions

  // Profile Form State
  const [profile, setProfile] = useState<FamilyProfile>({
    ...INITIAL_DEFAULT_PROFILE,
  });

  // Doctor note preview simulation
  const [showDocUploadModal, setShowDocUploadModal] = useState(false);
  const [isScanningNote, setIsScanningNote] = useState(false);

  const handleNext = () => {
    if (currentStep < totalSteps) {
      setCurrentStep(prev => prev + 1);
    } else {
      onComplete(profile);
    }
  };

  const handlePrev = () => {
    if (currentStep > 1) {
      setCurrentStep(prev => prev - 1);
    } else {
      onBackToLanding();
    }
  };

  // Quick fill with sample data from PDF
  const handleLoadSampleProfile = () => {
    setProfile({ ...INITIAL_DEFAULT_PROFILE });
  };

  const toggleConditionForBa = (condition: string) => {
    setProfile(prev => {
      const updatedMembers = [...prev.members];
      const ba = updatedMembers[0];
      if (ba) {
        if (ba.conditions.includes(condition)) {
          ba.conditions = ba.conditions.filter(c => c !== condition);
        } else {
          ba.conditions = [...ba.conditions, condition];
        }
      }
      return { ...prev, members: updatedMembers };
    });
  };

  const toggleAllergy = (allergy: string) => {
    setProfile(prev => {
      const updatedMembers = [...prev.members];
      const ba = updatedMembers[0];
      if (ba) {
        if (ba.allergies.includes(allergy)) {
          ba.allergies = ba.allergies.filter(a => a !== allergy);
        } else {
          ba.allergies = [...ba.allergies, allergy];
        }
      }
      return { ...prev, members: updatedMembers };
    });
  };

  const toggleExcluded = (item: string) => {
    setProfile(prev => {
      const exists = prev.excludedIngredients.includes(item);
      return {
        ...prev,
        excludedIngredients: exists
          ? prev.excludedIngredients.filter(i => i !== item)
          : [...prev.excludedIngredients, item],
      };
    });
  };

  const toggleAppliance = (appliance: string) => {
    setProfile(prev => {
      const exists = prev.appliances.includes(appliance);
      return {
        ...prev,
        appliances: exists
          ? prev.appliances.filter(a => a !== appliance)
          : [...prev.appliances, appliance],
      };
    });
  };

  return (
    <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] flex flex-col justify-between pt-16 sm:pt-20 pb-10 px-4 sm:px-6">
      <div className="max-w-xl mx-auto w-full">
        
        {/* Top Floating Mini-Nav for Onboarding */}
        <div className="flex items-center justify-between mb-4">
          <button
            onClick={handlePrev}
            className="w-10 h-10 rounded-full bg-white border border-stone-200 shadow-sm flex items-center justify-center text-stone-600 hover:text-stone-900 hover:bg-stone-50 transition-colors cursor-pointer"
            title="Quay lại"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>

          {/* Progress Indicator */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-stone-500">
              {currentStep} / {totalSteps}
            </span>
          </div>

          <button
            onClick={handleLoadSampleProfile}
            className="text-xs font-semibold px-3 py-1.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200/80 hover:bg-amber-100 transition-colors"
            title="Điền tự động hồ sơ mẫu"
          >
            Hồ sơ mẫu ⚡
          </button>
        </div>

        {/* Progress Bar (Warm Peach & Terracotta) */}
        <div className="w-full bg-stone-200/80 h-1.5 rounded-full overflow-hidden mb-6">
          <div
            className="bg-gradient-to-r from-orange-500 to-amber-500 h-full transition-all duration-300 rounded-full"
            style={{ width: `${(currentStep / totalSteps) * 100}%` }}
          />
        </div>

        {/* Card Screen Container (Inspired by image_506389f0.png) */}
        <div className="bg-gradient-to-b from-[#FFF5EC] via-white to-white rounded-3xl p-6 sm:p-8 border border-orange-100 shadow-[0_12px_40px_rgba(245,158,11,0.08)] relative overflow-hidden">
          
          {/* Subtle warm background glow */}
          <div className="absolute -top-16 -right-16 w-48 h-48 rounded-full bg-orange-200/30 filter blur-3xl pointer-events-none" />
          <div className="absolute -bottom-16 -left-16 w-48 h-48 rounded-full bg-amber-200/20 filter blur-3xl pointer-events-none" />

          {/* ================= STEP 1: LÀM QUEN (Exact match with image_506389f0) ================= */}
          {currentStep === 1 && (
            <div className="space-y-6">
              <div>
                <span className="inline-block text-[11px] font-bold tracking-wider text-orange-600 uppercase mb-2">
                  BƯỚC 1 · LÀM QUEN
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 leading-snug">
                  Cho chúng tôi gọi bạn là gì?
                </h2>
                <p className="text-stone-600 text-sm mt-2 leading-relaxed">
                  Tên sẽ xuất hiện trong những lời chào buổi sáng và thực đơn gợi ý hằng ngày.
                </p>
              </div>

              {/* Main Input Box */}
              <div>
                <div className="relative">
                  <input
                    type="text"
                    value={profile.cookName}
                    onChange={(e) => setProfile({ ...profile, cookName: e.target.value })}
                    placeholder="Quyền"
                    className="w-full px-5 py-4 rounded-2xl bg-white border-2 border-orange-200/90 text-stone-900 font-semibold text-lg focus:outline-none focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 shadow-inner transition-all"
                    maxLength={20}
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs text-stone-400">
                    {profile.cookName.length}/20
                  </span>
                </div>
                <p className="text-xs text-stone-400 mt-2">
                  Có thể là tên gọi ở nhà · 1–20 ký tự
                </p>
              </div>

              {/* Quick Suggestions (Exact match to image) */}
              <div>
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block mb-2.5">
                  HOẶC GỢI Ý
                </span>
                <div className="flex flex-wrap gap-2.5">
                  {['Bạn', 'Đầu bếp', 'Mẹ ơi', 'Quyền', 'Chị Hai'].map((name) => (
                    <button
                      key={name}
                      type="button"
                      onClick={() => setProfile({ ...profile, cookName: name })}
                      className={`px-4 py-2 rounded-xl text-sm font-medium transition-all cursor-pointer ${
                        profile.cookName === name
                          ? 'bg-[#014043] text-white font-semibold shadow-xs'
                          : 'bg-white border border-stone-200 text-stone-700 hover:border-orange-300 hover:bg-orange-50/50'
                      }`}
                    >
                      {name}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* ================= STEP 2: THÀNH VIÊN GIA ĐÌNH ================= */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <div>
                <span className="inline-block text-[11px] font-bold tracking-wider text-orange-600 uppercase mb-2">
                  BƯỚC 2 · THÀNH VIÊN
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 leading-snug">
                  Nhà mình có mấy người ăn cơm ở nhà?
                </h2>
                <p className="text-stone-600 text-sm mt-2 leading-relaxed">
                  Tuổi và giới tính từng người giúp Bếp tính chính xác nhu cầu năng lượng (kcal) và đạm theo Viện Dinh Dưỡng.
                </p>
              </div>

              {/* Number of Members Counter */}
              <div className="flex items-center justify-between p-4 rounded-2xl bg-white border border-orange-100 shadow-sm">
                <div>
                  <span className="font-bold text-stone-900 block">Số người ăn cơm thường xuyên</span>
                  <span className="text-xs text-stone-500">Mâm cơm ấm cúng hằng ngày</span>
                </div>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setProfile({ ...profile, memberCount: Math.max(1, profile.memberCount - 1) })}
                    className="w-8 h-8 rounded-full bg-stone-100 text-stone-700 font-bold hover:bg-stone-200 flex items-center justify-center cursor-pointer"
                  >
                    -
                  </button>
                  <span className="font-extrabold text-xl text-orange-600 w-6 text-center">
                    {profile.memberCount}
                  </span>
                  <button
                    type="button"
                    onClick={() => setProfile({ ...profile, memberCount: Math.min(10, profile.memberCount + 1) })}
                    className="w-8 h-8 rounded-full bg-orange-100 text-orange-700 font-bold hover:bg-orange-200 flex items-center justify-center cursor-pointer"
                  >
                    +
                  </button>
                </div>
              </div>

              {/* Member list cards */}
              <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
                {profile.members.map((member) => (
                  <div
                    key={member.id}
                    className="p-3.5 rounded-2xl bg-white border border-stone-200/80 flex items-center justify-between text-sm shadow-sm"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-orange-50 text-orange-600 flex items-center justify-center font-bold text-xs">
                        {member.relation[0]}
                      </div>
                      <div>
                        <div className="font-semibold text-stone-900 flex items-center gap-2">
                          <span>{member.name}</span>
                          <span className="text-xs text-stone-400">({member.age} tuổi • {member.gender})</span>
                        </div>
                        {member.conditions.length > 0 && (
                          <div className="flex items-center gap-1 mt-0.5">
                            <span className="inline-block px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-50 text-rose-600 border border-rose-100">
                              {member.conditions.join(', ')}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ================= STEP 3: BỆNH LÝ & LỜI DẶN BÁC SĨ ================= */}
          {currentStep === 3 && (
            <div className="space-y-6">
              <div>
                <span className="inline-block text-[11px] font-bold tracking-wider text-orange-600 uppercase mb-2">
                  BƯỚC 3 · BỆNH LÝ & LỜI DẶN BÁC SĨ
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 leading-snug">
                  Có ai đang có bệnh hoặc kiêng gì không?
                </h2>
                <p className="text-stone-600 text-sm mt-2 leading-relaxed">
                  Bếp chuyển bệnh lý thành quy tắc ăn uống chính thống (như giảm muối cho người huyết áp cao, giảm carb nhanh cho tiểu đường).
                </p>
              </div>

              {/* Popular conditions chips */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block">
                  CHỌN BỆNH LÝ TRONG NHÀ (ÁP DỤNG CHO BA/THÀNH VIÊN)
                </span>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {[
                    { label: 'Huyết áp cao', note: 'Cần giảm muối & mắm' },
                    { label: 'Tiểu đường type 2', note: 'Kiểm soát tinh bột nhanh' },
                    { label: 'Gout (Axit Uric)', note: 'Kiêng nội tạng, purin cao' },
                    { label: 'Mỡ máu / Tim mạch', note: 'Hạn chế dầu mỡ động vật' },
                    { label: 'Dạ dày / Trào ngược', note: 'Tránh quá cay chua' },
                    { label: 'Bệnh thận', note: 'Kiểm soát đạm & kali' },
                  ].map((cond) => {
                    const isSelected = profile.members[0]?.conditions.includes(cond.label);
                    return (
                      <button
                        key={cond.label}
                        type="button"
                        onClick={() => toggleConditionForBa(cond.label)}
                        className={`p-3 rounded-xl text-left border transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-rose-50/80 border-rose-300 ring-2 ring-rose-400/20 shadow-sm'
                            : 'bg-white border-stone-200 hover:border-orange-200'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className={`text-xs font-bold ${isSelected ? 'text-rose-700' : 'text-stone-800'}`}>
                            {cond.label}
                          </span>
                          {isSelected && <Check className="w-4 h-4 text-rose-600" />}
                        </div>
                        <span className="text-[11px] text-stone-500 block mt-0.5">{cond.note}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Doctor Note upload feature showcase (from PDF) */}
              <div className="p-4 rounded-2xl bg-amber-50/70 border border-amber-200/70 text-xs text-amber-900">
                <div className="flex items-start gap-2.5">
                  <FileText className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
                  <div className="space-y-1">
                    <span className="font-bold block">Tính năng quét lời dặn ăn uống của Bác sĩ (AI Vision)</span>
                    <p className="text-amber-800/80 leading-relaxed">
                      "Ba được dặn ăn nhạt dưới 5g muối, hạn chế đồ kho mặn, bổ sung rau củ có kali." — Bếp đã lưu vào hồ sơ tự động.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ================= STEP 4: DỊ ỨNG & NGUYÊN LIỆU KIÊNG ================= */}
          {currentStep === 4 && (
            <div className="space-y-6">
              <div>
                <span className="inline-block text-[11px] font-bold tracking-wider text-rose-600 uppercase mb-2">
                  BƯỚC 4 · DỊ ỨNG LÀ BỘ LỌC TUYỆT ĐỐI
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 leading-snug">
                  Ai dị ứng thực phẩm hoặc không ăn món nào?
                </h2>
                <p className="text-stone-600 text-sm mt-2 leading-relaxed">
                  Nguyên tắc an toàn sức khỏe: Mọi món có chất dị ứng đều bị code loại trừ tuyệt đối trước khi AI đề xuất.
                </p>
              </div>

              {/* Allergies list */}
              <div>
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block mb-2">
                  DỊ ỨNG NGUY HIỂM (LOẠI BỎ TRIỆT ĐỂ)
                </span>
                <div className="flex flex-wrap gap-2">
                  {['Tôm & Cua', 'Đậu phộng', 'Mực & Hải sản vỏ cứng', 'Sữa bò', 'Trứng gà', 'Không có ai dị ứng'].map((item) => {
                    const isSelected = profile.members[0]?.allergies.includes(item);
                    return (
                      <button
                        key={item}
                        type="button"
                        onClick={() => toggleAllergy(item)}
                        className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-rose-600 text-white shadow-sm shadow-rose-600/30'
                            : 'bg-white border border-stone-200 text-stone-700 hover:border-rose-200 hover:bg-rose-50/40'
                        }`}
                      >
                        {item}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Disliked ingredients */}
              <div>
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block mb-2">
                  NGUYÊN LIỆU CẢ NHÀ KHÔNG ĂN / KHÔNG THÍCH
                </span>
                <div className="flex flex-wrap gap-2">
                  {['Hành ngò', 'Mắm tôm sống', 'Nội tạng động vật', 'Thịt mỡ nhiều', 'Mướp đắng', 'Mù tạt'].map((item) => {
                    const isSelected = profile.excludedIngredients.includes(item);
                    return (
                      <button
                        key={item}
                        type="button"
                        onClick={() => toggleExcluded(item)}
                        className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-amber-600 text-white shadow-sm'
                            : 'bg-white border border-stone-200 text-stone-700 hover:border-amber-200'
                        }`}
                      >
                        {isSelected ? `✕ ${item}` : item}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* ================= STEP 5: KHẨU VỊ & THỜI GIAN BỮA TỐI ================= */}
          {currentStep === 5 && (
            <div className="space-y-6">
              <div>
                <span className="inline-block text-[11px] font-bold tracking-wider text-orange-600 uppercase mb-2">
                  BƯỚC 5 · KHẨU VỊ & THỜI GIAN
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 leading-snug">
                  Khẩu vị gia đình & Giờ ăn cơm?
                </h2>
                <p className="text-stone-600 text-sm mt-2 leading-relaxed">
                  Giờ ăn tối là mốc chuẩn để Bếp tính ngược timeline (rã đông, ướp gia vị, nấu) chính xác từng phút.
                </p>
              </div>

              {/* Regional taste */}
              <div>
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block mb-2.5">
                  VÙNG MIỀN & KHẨU VỊ CHÍNH
                </span>
                <div className="grid grid-cols-3 gap-2.5">
                  {[
                    { id: 'Bac', label: 'Miền Bắc', sub: 'Thanh dịu, vừa vặn' },
                    { id: 'Trung', label: 'Miền Trung', sub: 'Đậm đà, thơm cay' },
                    { id: 'Nam', label: 'Miền Nam', sub: 'Hài hòa, ngọt thanh' },
                  ].map((r) => (
                    <button
                      key={r.id}
                      type="button"
                      onClick={() => setProfile({ ...profile, regionTaste: r.id as any })}
                      className={`p-3 rounded-2xl text-center border transition-all cursor-pointer ${
                        profile.regionTaste === r.id
                          ? 'bg-orange-50 border-orange-400 ring-2 ring-orange-400/20'
                          : 'bg-white border-stone-200 hover:border-orange-200'
                      }`}
                    >
                      <span className="font-bold text-stone-900 text-xs sm:text-sm block">{r.label}</span>
                      <span className="text-[10px] text-stone-500 block mt-0.5">{r.sub}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Dinner time & Prep time */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="p-3.5 rounded-2xl bg-white border border-stone-200">
                  <div className="flex items-center gap-2 mb-1.5 text-xs font-bold text-stone-700">
                    <Clock className="w-3.5 h-3.5 text-orange-600" />
                    <span>Giờ dọn mâm tối nay</span>
                  </div>
                  <input
                    type="time"
                    value={profile.dinnerTime}
                    onChange={(e) => setProfile({ ...profile, dinnerTime: e.target.value })}
                    className="w-full text-xl font-extrabold text-stone-900 bg-transparent focus:outline-none"
                  />
                  <span className="text-[11px] text-stone-400 block mt-1">Timeline sẽ lùi từ giờ này</span>
                </div>

                <div className="p-3.5 rounded-2xl bg-white border border-stone-200">
                  <div className="flex items-center gap-2 mb-1.5 text-xs font-bold text-stone-700">
                    <ChefHat className="w-3.5 h-3.5 text-orange-600" />
                    <span>Thời gian nấu tối đa</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      value={profile.cookingTimeMinutes}
                      onChange={(e) => setProfile({ ...profile, cookingTimeMinutes: Number(e.target.value) })}
                      className="w-16 text-xl font-extrabold text-stone-900 bg-transparent focus:outline-none"
                      min={15}
                      max={120}
                    />
                    <span className="text-sm font-semibold text-stone-600">phút</span>
                  </div>
                  <span className="text-[11px] text-stone-400 block mt-1">Ví dụ: 30p, 45p, 60p</span>
                </div>
              </div>
            </div>
          )}

          {/* ================= STEP 6: THIẾT BỊ BẾP & NGÂN SÁCH (TỔNG KẾT) ================= */}
          {currentStep === 6 && (
            <div className="space-y-6">
              <div>
                <span className="inline-block text-[11px] font-bold tracking-wider text-emerald-600 uppercase mb-2">
                  BƯỚC 6 · THIẾT BỊ & HOÀN TẤT
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold font-serif-display text-stone-900 leading-snug">
                  Bếp có những dụng cụ gì & Ngân sách?
                </h2>
                <p className="text-stone-600 text-sm mt-2 leading-relaxed">
                  Bếp phối hợp các thiết bị song song (ví dụ vừa cắm cơm vừa hấp, chiên không dầu) để bạn thảnh thơi.
                </p>
              </div>

              {/* Appliances */}
              <div>
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block mb-2">
                  DỤNG CỤ CÓ SẴN TRONG BẾP
                </span>
                <div className="flex flex-wrap gap-2">
                  {['Nồi cơm điện', 'Nồi chiên không dầu', 'Lò vi sóng', 'Bếp từ', 'Nồi áp suất'].map((app) => {
                    const isSelected = profile.appliances.includes(app);
                    return (
                      <button
                        key={app}
                        type="button"
                        onClick={() => toggleAppliance(app)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-stone-900 text-white shadow-sm'
                            : 'bg-white border border-stone-200 text-stone-700 hover:border-stone-400'
                        }`}
                      >
                        {isSelected ? `✓ ${app}` : app}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Budget selector */}
              <div>
                <span className="text-xs font-bold text-stone-400 uppercase tracking-wider block mb-2">
                  NGÂN SÁCH DỰ KIẾN MỖI BỮA
                </span>
                <div className="grid grid-cols-2 gap-2">
                  {['80k - 120k', '120k - 160k', '160k - 220k', 'Trên 250k'].map((b) => (
                    <button
                      key={b}
                      type="button"
                      onClick={() => setProfile({ ...profile, budgetPerMeal: b })}
                      className={`p-3 rounded-xl text-xs font-bold text-left border transition-all cursor-pointer ${
                        profile.budgetPerMeal.includes(b.split(' ')[0])
                          ? 'bg-amber-500 text-white border-amber-600 shadow-sm'
                          : 'bg-white border-stone-200 text-stone-800 hover:border-amber-300'
                      }`}
                    >
                      {b} / bữa
                    </button>
                  ))}
                </div>
              </div>

              {/* Final Ready Box */}
              <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200/80 text-emerald-900 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                <div className="text-xs">
                  <span className="font-bold block">Hồ sơ dinh dưỡng đã sẵn sàng!</span>
                  <span className="text-emerald-800">
                    Bếp Nhà Mình đã chuẩn bị sẵn kịch bản mâm cơm và timeline tối nay cho {profile.cookName}.
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* ================= Bottom Button: Tiếp tục -> (Exact match to image_506389f0.png) ================= */}
          <div className="mt-8 pt-4 border-t border-orange-100/60">
            <button
              type="button"
              onClick={handleNext}
              className="w-full py-3.5 rounded-full text-base font-bold text-white bg-[#014043] hover:bg-[#002f32] shadow-sm shadow-teal-950/20 active:scale-[0.99] transition-all flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>{currentStep === totalSteps ? 'Hoàn tất & Vào Bếp AI' : 'Tiếp tục'}</span>
              <ArrowRight className="w-5 h-5" />
            </button>
          </div>

        </div>

        {/* Footnote */}
        <p className="text-center text-xs text-stone-400 mt-6">
          Dữ liệu dinh dưỡng được đối chiếu Thông tư 43/2014/TT-BYT & Viện Dinh Dưỡng Quốc Gia.
        </p>

      </div>
    </div>
  );
};
