export interface FamilyMember {
  id: string;
  name: string;
  relation: string; // 'Ba', 'Mẹ', 'Con trai', 'Con gái', 'Ông', 'Bà'
  age: number;
  gender: 'Nam' | 'Nữ';
  conditions: string[]; // 'Huyết áp cao', 'Tiểu đường type 2', 'Gout', 'Mỡ máu', 'Dạ dày'
  allergies: string[]; // 'Tôm', 'Cua', 'Hải sản', 'Đậu phộng', 'Sữa'
  notes?: string;
}

export interface FamilyProfile {
  cookName: string;
  memberCount: number;
  members: FamilyMember[];
  excludedIngredients: string[]; // 'Hành ngò', 'Thịt bò', 'Mắm tôm', 'Nội tạng'
  regionTaste: 'Bắc' | 'Trung' | 'Nam';
  flavorIntensity: 'Đậm đà' | 'Vừa vị' | 'Thanh nhạt';
  mealsPerDay: number;
  dinnerTime: string; // '18:30'
  cookingTimeMinutes: number; // 45
  appliances: string[]; // 'Nồi cơm điện', 'Nồi chiên không dầu', 'Lò vi sóng', 'Bếp từ'
  budgetPerMeal: string; // '100.000đ - 160.000đ'
  favoriteDishes: string[]; // 'Cá kho tộ', 'Canh chua', 'Thịt kho trứng'
  doctorNoteText?: string;
  doctorNoteImage?: string;
}

export interface MealItem {
  id: string;
  name: string;
  category: 'Món mặn' | 'Món canh' | 'Món xào/rau' | 'Món phụ' | 'Cơm/Tinh bột';
  description: string;
  imageUrl: string;
  cookingMinutes: number;
  pathologyAdjustments: string; // Ví dụ: "Ba huyết áp cao: giảm mắm, múc riêng trước khi nêm thêm"
  calories: number;
  proteinGrams: number;
  carbsGrams: number;
  fatGrams: number;
  sodiumMg: number;
  safeForConditions: string[];
  safeForAllergies: boolean;
}

export interface MealPlan {
  id: string;
  title: string;
  tagline: string;
  estimatedCost: string;
  targetDinnerTime: string;
  prepStartTime: string;
  items: MealItem[];
  safetyCheck: {
    allergyResult: string;
    conditionResult: string;
    nutritionResult: string;
    isFullyCompliant: boolean;
  };
  nutritionSummary: {
    totalCalories: number;
    totalProtein: number;
    fiber: string;
    sodiumWarning?: string;
  };
}

export interface TimelineStep {
  id: string;
  time: string;
  action: string;
  detail: string;
  durationMinutes?: number;
  category: 'defrost' | 'prep' | 'cook' | 'rice' | 'serve';
  completed: boolean;
  forSpecialMember?: string;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
  mealPlan?: MealPlan;
  timeline?: TimelineStep[];
  suggestions?: string[];
  medicalAlert?: {
    title: string;
    content: string;
    source: string;
  };
}
