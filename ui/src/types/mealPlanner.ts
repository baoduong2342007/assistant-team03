export type DayOfWeek = 'mon' | 'tue' | 'wed' | 'thu' | 'fri' | 'sat' | 'sun';

export type ServingSize = 1 | 2 | 3 | 4 | 5; // 5 represents 5+

export type GoalType = 'quick_easy' | 'budget_friendly' | 'eat_healthy';

export type DietType =
  | 'none'
  | 'vegan'
  | 'vegetarian'
  | 'pescatarian'
  | 'paleo'
  | 'low_carb'
  | 'keto';

export type AllergenType =
  | 'gluten'
  | 'dairy'
  | 'egg'
  | 'soy'
  | 'peanut'
  | 'treenut'
  | 'fish'
  | 'shellfish';

export interface RecipeIngredient {
  name: string;
  amount: string;
  category: 'produce' | 'meat' | 'pantry' | 'dairy' | 'spice';
  checked?: boolean;
}

export interface PlannerRecipe {
  id: string;
  title: string;
  description: string;
  image: string;
  cookTime: number; // minutes
  costPerServing: number; // VND
  rating: number;
  reviewCount: number;
  calories: number;
  protein: number; // g
  carbs: number; // g
  fat: number; // g
  tags: string[];
  diet: DietType[];
  allergens: AllergenType[];
  ingredients: RecipeIngredient[];
  instructions: string[];
}

export interface MealPlannerPreferences {
  selectedDays: DayOfWeek[];
  servingSize: ServingSize;
  priorityGoal: GoalType;
  diet: DietType;
  allergies: AllergenType[];
  dislikedIngredients: string[];
}

export type WeeklySchedule = Record<DayOfWeek, PlannerRecipe[]>;

export interface UserAuth {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  isLoggedIn: boolean;
}

export interface DayInfo {
  key: DayOfWeek;
  code: string; // T2, T3...
  fullLabel: string; // Thứ Hai...
  shortLabel: string; // T2...
  dateLabel: string; // 28/10...
  nameEn: string; // Monday, Friday...
}
