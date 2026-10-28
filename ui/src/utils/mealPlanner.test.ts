import assert from 'node:assert/strict';
import test from 'node:test';
import { INITIAL_PREFERENCES, INITIAL_WEEKLY_SCHEDULE, MOCK_RECIPES } from '../data/mealPlannerMockData';
import { createWeeklyPlan, findRecipeReplacement, isRecipeSuitable } from './mealPlanner';
import type { MealPlannerPreferences } from '../types/mealPlanner';

const preferences = (overrides: Partial<MealPlannerPreferences> = {}): MealPlannerPreferences => ({
  ...INITIAL_PREFERENCES, ...overrides,
});

test('the initial schedule contains real recipes, including Sunday', () => {
  const recipes = Object.values(INITIAL_WEEKLY_SCHEDULE).flat();
  assert.equal(recipes.length, 5);
  assert.ok(recipes.every((recipe) => recipe && MOCK_RECIPES.some((known) => known.id === recipe.id)));
  assert.equal(INITIAL_WEEKLY_SCHEDULE.sun[0].id, 'bep-cedar-salmon');
});

test('generation reuses a small suitable pool without relaxing allergy constraints', () => {
  const vegan = MOCK_RECIPES.find((recipe) => recipe.id === 'rec-5')!;
  const fish = MOCK_RECIPES.find((recipe) => recipe.id === 'rec-1')!;
  const plan = createWeeklyPlan([vegan, fish], preferences({ allergies: ['fish'], selectedDays: ['mon', 'wed'] }));
  assert.deepEqual(plan.mon.map((recipe) => recipe.id), [vegan.id]);
  assert.deepEqual(plan.wed.map((recipe) => recipe.id), [vegan.id]);
  assert.deepEqual(plan.tue, []);
  assert.deepEqual(plan.sun, []);
});

test('generation leaves selected days empty when all recipes are excluded', () => {
  const fish = MOCK_RECIPES.find((recipe) => recipe.id === 'rec-1')!;
  const plan = createWeeklyPlan([fish], preferences({ allergies: ['fish'] }));
  assert.ok(Object.values(plan).every((day) => day.length === 0));
});

test('no selected days and an empty catalogue both produce empty schedules', () => {
  assert.ok(Object.values(createWeeklyPlan(MOCK_RECIPES, preferences({ selectedDays: [] }))).every((day) => !day.length));
  assert.ok(Object.values(createWeeklyPlan([], preferences())).every((day) => !day.length));
});

test('swap returns null when only an excluded alternative remains', () => {
  const current = MOCK_RECIPES.find((recipe) => recipe.id === 'rec-5')!;
  const fish = MOCK_RECIPES.find((recipe) => recipe.id === 'rec-1')!;
  assert.equal(findRecipeReplacement([current, fish], [current], current.id, preferences({ allergies: ['fish'] })), null);
});

test('swap does not duplicate another recipe already on the same day', () => {
  const [current, other] = MOCK_RECIPES;
  assert.equal(findRecipeReplacement([current, other], [current, other], current.id, preferences()), null);
});

test('diet and case-insensitive disliked ingredients are applied together', () => {
  const fish = MOCK_RECIPES.find((recipe) => recipe.id === 'rec-1')!;
  assert.equal(isRecipeSuitable(fish, preferences({ diet: 'vegan' })), false);
  assert.equal(isRecipeSuitable(fish, preferences({ dislikedIngredients: ['  CÁ HỒI  '] })), false);
  assert.equal(isRecipeSuitable(fish, preferences({ diet: 'pescatarian' })), true);
});
