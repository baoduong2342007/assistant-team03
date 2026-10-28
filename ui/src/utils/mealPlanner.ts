import type { DayOfWeek, MealPlannerPreferences, PlannerRecipe, WeeklySchedule } from '../types/mealPlanner';

export function isRecipeSuitable(recipe: PlannerRecipe, preferences: MealPlannerPreferences): boolean {
  if (preferences.diet !== 'none' && !recipe.diet.includes(preferences.diet)) return false;
  if (preferences.allergies.some((allergen) => recipe.allergens.includes(allergen))) return false;
  return !recipe.ingredients.some((ingredient) =>
    preferences.dislikedIngredients.some((disliked) =>
      ingredient.name.toLocaleLowerCase('vi').includes(disliked.trim().toLocaleLowerCase('vi'))
    )
  );
}

export function findRecipeReplacement(
  recipes: PlannerRecipe[], dayRecipes: PlannerRecipe[], currentRecipeId: string,
  preferences: MealPlannerPreferences,
): PlannerRecipe | null {
  const usedIds = new Set(dayRecipes.map((recipe) => recipe.id));
  const candidates = recipes.filter((recipe) =>
    recipe.id !== currentRecipeId && !usedIds.has(recipe.id) && isRecipeSuitable(recipe, preferences)
  );
  return candidates.length ? candidates[Math.floor(Math.random() * candidates.length)] : null;
}

export function createWeeklyPlan(recipes: PlannerRecipe[], preferences: MealPlannerPreferences): WeeklySchedule {
  const days: DayOfWeek[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
  const pool = recipes.filter((recipe) => isRecipeSuitable(recipe, preferences));
  // Keep constraints even when fewer than three recipes match, or none do.
  const shuffled = [...pool].sort(() => Math.random() - 0.5);
  let index = 0;
  return Object.fromEntries(days.map((day) => [
    day, preferences.selectedDays.includes(day) && shuffled.length
      ? [shuffled[index++ % shuffled.length]] : [],
  ])) as WeeklySchedule;
}
