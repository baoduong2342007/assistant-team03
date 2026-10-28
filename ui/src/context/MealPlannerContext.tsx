import React, { createContext, useContext, useState, useEffect, useMemo } from 'react';
import {
  DayOfWeek,
  ServingSize,
  GoalType,
  DietType,
  AllergenType,
  PlannerRecipe,
  MealPlannerPreferences,
  WeeklySchedule,
  UserAuth,
  RecipeIngredient,
} from '../types/mealPlanner';
import {
  INITIAL_PREFERENCES,
  INITIAL_WEEKLY_SCHEDULE,
  MOCK_RECIPES,
} from '../data/mealPlannerMockData';

export interface GroceryItemDisplay {
  id: string;
  name: string;
  amount: string;
  category: 'produce' | 'meat' | 'pantry' | 'dairy' | 'spice';
  checked: boolean;
  estimatedPrice: number;
  recipeSource: string;
}

interface MealPlannerContextType {
  preferences: MealPlannerPreferences;
  updatePreferences: (updates: Partial<MealPlannerPreferences>) => void;
  toggleDay: (day: DayOfWeek) => void;
  setServingSize: (size: ServingSize) => void;
  setPriorityGoal: (goal: GoalType) => void;
  setDiet: (diet: DietType) => void;
  toggleAllergen: (allergen: AllergenType) => void;
  toggleDislikedIngredient: (ingredient: string) => void;
  addDislikedIngredient: (ingredient: string) => void;

  weeklyPlan: WeeklySchedule;
  swapRecipe: (day: DayOfWeek, currentRecipeId: string) => PlannerRecipe | null;
  removeRecipe: (day: DayOfWeek, recipeId: string) => void;
  addRecipe: (day: DayOfWeek, recipe: PlannerRecipe) => void;
  moveRecipe: (fromDay: DayOfWeek, toDay: DayOfWeek, recipeId: string, toIndex?: number) => void;
  generateWeeklyPlan: () => void;

  user: UserAuth | null;
  login: (email: string, name?: string) => void;
  logout: () => void;

  // Computed & helper stats
  stats: {
    estimatedCartTotal: number; // in VND
    averageCostPerServing: number; // in VND
    plannedMealsCount: number;
    selectedDaysCount: number;
  };
  currencyMode: 'VND' | 'USD';
  setCurrencyMode: (mode: 'VND' | 'USD') => void;
  formatPrice: (amountVnd: number) => string;

  allRecipes: PlannerRecipe[];
  groceryItems: GroceryItemDisplay[];
  toggleGroceryItem: (itemId: string) => void;
  markAllGroceries: (checked: boolean) => void;
}

const MealPlannerContext = createContext<MealPlannerContextType | undefined>(undefined);

const PREFS_STORAGE_KEY = 'bep_meal_preferences_v1';
const PLAN_STORAGE_KEY = 'bep_weekly_plan_v1';
const USER_STORAGE_KEY = 'bep_user_auth_v1';
const GROCERY_CHECKED_KEY = 'bep_grocery_checked_v1';

export const MealPlannerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // 1. Preferences State
  const [preferences, setPreferences] = useState<MealPlannerPreferences>(() => {
    try {
      const saved = localStorage.getItem(PREFS_STORAGE_KEY);
      if (saved) return JSON.parse(saved);
    } catch {
      // fallback
    }
    return INITIAL_PREFERENCES;
  });

  // 2. Weekly Plan State
  const [weeklyPlan, setWeeklyPlan] = useState<WeeklySchedule>(() => {
    try {
      const saved = localStorage.getItem(PLAN_STORAGE_KEY);
      if (saved) return JSON.parse(saved);
    } catch {
      // fallback
    }
    return INITIAL_WEEKLY_SCHEDULE;
  });

  // 3. User Authentication State
  const [user, setUser] = useState<UserAuth | null>(() => {
    try {
      const saved = localStorage.getItem(USER_STORAGE_KEY);
      if (saved) return JSON.parse(saved);
    } catch {
      // fallback
    }
    return null;
  });

  // 4. Grocery Checked state (ingredients user already has in pantry)
  const [checkedGroceryIds, setCheckedGroceryIds] = useState<Record<string, boolean>>(() => {
    try {
      const saved = localStorage.getItem(GROCERY_CHECKED_KEY);
      if (saved) return JSON.parse(saved);
    } catch {
      // fallback
    }
    return {};
  });

  // 5. Currency display state
  const [currencyMode, setCurrencyMode] = useState<'VND' | 'USD'>('VND');

  // Sync to localStorage
  useEffect(() => {
    try {
      localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(preferences));
    } catch {}
  }, [preferences]);

  useEffect(() => {
    try {
      localStorage.setItem(PLAN_STORAGE_KEY, JSON.stringify(weeklyPlan));
    } catch {}
  }, [weeklyPlan]);

  useEffect(() => {
    try {
      if (user) {
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
      } else {
        localStorage.removeItem(USER_STORAGE_KEY);
      }
    } catch {}
  }, [user]);

  useEffect(() => {
    try {
      localStorage.setItem(GROCERY_CHECKED_KEY, JSON.stringify(checkedGroceryIds));
    } catch {}
  }, [checkedGroceryIds]);

  // Preference update helpers
  const updatePreferences = (updates: Partial<MealPlannerPreferences>) => {
    setPreferences((prev) => ({ ...prev, ...updates }));
  };

  const toggleDay = (day: DayOfWeek) => {
    setPreferences((prev) => {
      const exists = prev.selectedDays.includes(day);
      const nextDays = exists
        ? prev.selectedDays.filter((d) => d !== day)
        : [...prev.selectedDays, day];
      return { ...prev, selectedDays: nextDays };
    });
  };

  const setServingSize = (size: ServingSize) => {
    setPreferences((prev) => ({ ...prev, servingSize: size }));
  };

  const setPriorityGoal = (goal: GoalType) => {
    setPreferences((prev) => ({ ...prev, priorityGoal: goal }));
  };

  const setDiet = (diet: DietType) => {
    setPreferences((prev) => ({ ...prev, diet }));
  };

  const toggleAllergen = (allergen: AllergenType) => {
    setPreferences((prev) => {
      const exists = prev.allergies.includes(allergen);
      const nextAllergies = exists
        ? prev.allergies.filter((a) => a !== allergen)
        : [...prev.allergies, allergen];
      return { ...prev, allergies: nextAllergies };
    });
  };

  const toggleDislikedIngredient = (ingredient: string) => {
    setPreferences((prev) => {
      const exists = prev.dislikedIngredients.includes(ingredient);
      const nextDisliked = exists
        ? prev.dislikedIngredients.filter((i) => i !== ingredient)
        : [...prev.dislikedIngredients, ingredient];
      return { ...prev, dislikedIngredients: nextDisliked };
    });
  };

  const addDislikedIngredient = (ingredient: string) => {
    const trimmed = ingredient.trim();
    if (!trimmed) return;
    setPreferences((prev) => {
      if (prev.dislikedIngredients.includes(trimmed)) return prev;
      return { ...prev, dislikedIngredients: [...prev.dislikedIngredients, trimmed] };
    });
  };

  // Helper to check if a recipe satisfies user constraints
  const isRecipeSuitable = (recipe: PlannerRecipe): boolean => {
    // Diet check
    if (preferences.diet !== 'none' && !recipe.diet.includes(preferences.diet)) {
      return false;
    }
    // Allergens check
    if (preferences.allergies.some((allergen) => recipe.allergens.includes(allergen))) {
      return false;
    }
    // Disliked ingredients check
    if (preferences.dislikedIngredients.length > 0) {
      const lowerDisliked = preferences.dislikedIngredients.map((d) => d.toLowerCase());
      const hasDisliked = recipe.ingredients.some((ing) =>
        lowerDisliked.some((dis) => ing.name.toLowerCase().includes(dis))
      );
      if (hasDisliked) return false;
    }
    return true;
  };

  // Swap recipe on a day
  const swapRecipe = (day: DayOfWeek, currentRecipeId: string): PlannerRecipe | null => {
    const currentDayRecipes = weeklyPlan[day] || [];
    const usedIds = new Set(currentDayRecipes.map((r) => r.id));

    // Candidate recipes from pool
    const candidates = MOCK_RECIPES.filter(
      (r) => !usedIds.has(r.id) && r.id !== currentRecipeId && isRecipeSuitable(r)
    );

    const replacement =
      candidates.length > 0
        ? candidates[Math.floor(Math.random() * candidates.length)]
        : MOCK_RECIPES.find((r) => r.id !== currentRecipeId) || null;

    if (replacement) {
      setWeeklyPlan((prev) => {
        const dayRecipes = prev[day] || [];
        const nextDayRecipes = dayRecipes.map((r) => (r.id === currentRecipeId ? replacement : r));
        return { ...prev, [day]: nextDayRecipes };
      });
      return replacement;
    }
    return null;
  };

  // Remove recipe from a day
  const removeRecipe = (day: DayOfWeek, recipeId: string) => {
    setWeeklyPlan((prev) => {
      const dayRecipes = prev[day] || [];
      return { ...prev, [day]: dayRecipes.filter((r) => r.id !== recipeId) };
    });
  };

  // Add recipe to a day
  const addRecipe = (day: DayOfWeek, recipe: PlannerRecipe) => {
    setWeeklyPlan((prev) => {
      const dayRecipes = prev[day] || [];
      return { ...prev, [day]: [...dayRecipes, recipe] };
    });
  };

  // Move recipe (Drag & Drop) between days or reorder
  const moveRecipe = (
    fromDay: DayOfWeek,
    toDay: DayOfWeek,
    recipeId: string,
    toIndex?: number
  ) => {
    setWeeklyPlan((prev) => {
      const sourceRecipes = [...(prev[fromDay] || [])];
      const targetRecipes = fromDay === toDay ? sourceRecipes : [...(prev[toDay] || [])];

      const itemIndex = sourceRecipes.findIndex((r) => r.id === recipeId);
      if (itemIndex === -1) return prev;

      const [movedItem] = sourceRecipes.splice(itemIndex, 1);

      if (fromDay === toDay) {
        const destIdx = typeof toIndex === 'number' ? toIndex : sourceRecipes.length;
        sourceRecipes.splice(destIdx, 0, movedItem);
        return { ...prev, [fromDay]: sourceRecipes };
      } else {
        const destIdx = typeof toIndex === 'number' ? toIndex : targetRecipes.length;
        targetRecipes.splice(destIdx, 0, movedItem);
        return {
          ...prev,
          [fromDay]: sourceRecipes,
          [toDay]: targetRecipes,
        };
      }
    });
  };

  // Auto-generate fresh weekly plan according to preferences
  const generateWeeklyPlan = () => {
    const days: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
    const suitablePool = MOCK_RECIPES.filter((r) => isRecipeSuitable(r));
    const pool = suitablePool.length >= 3 ? suitablePool : MOCK_RECIPES;

    const newSchedule: WeeklySchedule = {
      mon: [],
      tue: [],
      wed: [],
      thu: [],
      fri: [],
      sat: [],
      sun: [],
    };

    let poolIndex = 0;
    const shuffled = [...pool].sort(() => Math.random() - 0.5);

    days.forEach((day) => {
      // If the user selected this day, assign recipe
      if (preferences.selectedDays.includes(day)) {
        const recipe = shuffled[poolIndex % shuffled.length];
        newSchedule[day] = [recipe];
        poolIndex++;
      } else {
        newSchedule[day] = [];
      }
    });

    setWeeklyPlan(newSchedule);
  };

  // Auth helpers
  const login = (email: string, name?: string) => {
    const newUser: UserAuth = {
      id: 'user_' + Date.now(),
      email,
      name: name || email.split('@')[0],
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=80',
      isLoggedIn: true,
    };
    setUser(newUser);
  };

  const logout = () => {
    setUser(null);
  };

  // Stats calculation
  const stats = useMemo(() => {
    let totalMealCount = 0;
    let sumCostPerServing = 0;

    const days: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
    days.forEach((d) => {
      const recipes = weeklyPlan[d] || [];
      recipes.forEach((r) => {
        totalMealCount++;
        sumCostPerServing += r.costPerServing;
      });
    });

    const averageCostPerServing =
      totalMealCount > 0 ? Math.round(sumCostPerServing / totalMealCount) : 0;
    const estimatedCartTotal = sumCostPerServing * preferences.servingSize;

    return {
      estimatedCartTotal,
      averageCostPerServing,
      plannedMealsCount: totalMealCount,
      selectedDaysCount: preferences.selectedDays.length,
    };
  }, [weeklyPlan, preferences.servingSize, preferences.selectedDays]);

  // Price formatter
  const formatPrice = (amountVnd: number): string => {
    if (currencyMode === 'USD') {
      const usd = (amountVnd / 24500).toFixed(2);
      return `$${usd}`;
    }
    return `${amountVnd.toLocaleString('vi-VN')} ₫`;
  };

  // Aggregated Grocery Items
  const groceryItems = useMemo<GroceryItemDisplay[]>(() => {
    const itemsMap = new Map<string, GroceryItemDisplay>();
    const days: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];

    days.forEach((d) => {
      const recipes = weeklyPlan[d] || [];
      recipes.forEach((rec) => {
        rec.ingredients.forEach((ing, idx) => {
          const key = ing.name.toLowerCase().trim();
          const itemId = `grocery_${key.replace(/\s+/g, '_')}`;

          if (itemsMap.has(key)) {
            const existing = itemsMap.get(key)!;
            itemsMap.set(key, {
              ...existing,
              amount: `${existing.amount} + ${ing.amount}`,
              estimatedPrice: existing.estimatedPrice + Math.round((rec.costPerServing * 0.2) * (preferences.servingSize / 2)),
            });
          } else {
            itemsMap.set(key, {
              id: itemId,
              name: ing.name,
              amount: ing.amount,
              category: ing.category,
              checked: !!checkedGroceryIds[itemId],
              estimatedPrice: Math.round((rec.costPerServing * 0.25) * (preferences.servingSize / 2)),
              recipeSource: rec.title,
            });
          }
        });
      });
    });

    return Array.from(itemsMap.values());
  }, [weeklyPlan, checkedGroceryIds, preferences.servingSize]);

  const toggleGroceryItem = (itemId: string) => {
    setCheckedGroceryIds((prev) => ({
      ...prev,
      [itemId]: !prev[itemId],
    }));
  };

  const markAllGroceries = (checked: boolean) => {
    const updated: Record<string, boolean> = {};
    groceryItems.forEach((item) => {
      updated[item.id] = checked;
    });
    setCheckedGroceryIds(updated);
  };

  return (
    <MealPlannerContext.Provider
      value={{
        preferences,
        updatePreferences,
        toggleDay,
        setServingSize,
        setPriorityGoal,
        setDiet,
        toggleAllergen,
        toggleDislikedIngredient,
        addDislikedIngredient,

        weeklyPlan,
        swapRecipe,
        removeRecipe,
        addRecipe,
        moveRecipe,
        generateWeeklyPlan,

        user,
        login,
        logout,

        stats,
        currencyMode,
        setCurrencyMode,
        formatPrice,

        allRecipes: MOCK_RECIPES,
        groceryItems,
        toggleGroceryItem,
        markAllGroceries,
      }}
    >
      {children}
    </MealPlannerContext.Provider>
  );
};

export const useMealPlanner = (): MealPlannerContextType => {
  const context = useContext(MealPlannerContext);
  if (!context) {
    throw new Error('useMealPlanner must be used within a MealPlannerProvider');
  }
  return context;
};
