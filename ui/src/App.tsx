/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { HeroLanding } from './components/HeroLanding';
import { OnboardingFlow } from './components/OnboardingFlow';
import { ChatbotInterface } from './components/ChatbotInterface';
import { IntroductionModal, AboutModal } from './components/InfoModals';
import { FamilyProfile } from './types';
import { INITIAL_DEFAULT_PROFILE } from './data/mockData';

// Meal Planner System Imports
import { MealPlannerProvider } from './context/MealPlannerContext';
import { MealPlannerLanding } from './components/meal-planner/MealPlannerLanding';
import { MealPlannerPreferences } from './components/meal-planner/MealPlannerPreferences';
import { MealPlannerAuth } from './components/meal-planner/MealPlannerAuth';
import { MealPlannerApp } from './components/meal-planner/MealPlannerApp';
import { RecipeDetailModal } from './components/meal-planner/RecipeDetailModal';
import { GroceriesModal } from './components/meal-planner/GroceriesModal';
import { PlannerRecipe } from './types/mealPlanner';

export type AppView =
  | 'landing'
  | 'onboarding'
  | 'chat'
  | 'meal-planner'
  | 'meal-planner-preference'
  | 'meal-planner-auth'
  | 'meal-planner-app';

export default function App() {
  // Determine initial view based on browser URL
  const getViewFromUrl = (): AppView => {
    const path = window.location.pathname;
    if (path === '/meal-planner/preference') return 'meal-planner-preference';
    if (path.startsWith('/account/signup')) return 'meal-planner-auth';
    if (path === '/meal-planner/app') return 'meal-planner-app';
    if (path === '/onboarding') return 'onboarding';
    if (path === '/chat') return 'chat';
    if (path === '/home') return 'landing';
    return 'meal-planner';
  };

  const [currentView, setCurrentView] = useState<AppView>(getViewFromUrl);
  const [familyProfile, setFamilyProfile] = useState<FamilyProfile>(INITIAL_DEFAULT_PROFILE);
  const [isIntroOpen, setIsIntroOpen] = useState(false);
  const [isAboutOpen, setIsAboutOpen] = useState(false);
  const [previewRecipe, setPreviewRecipe] = useState<PlannerRecipe | null>(null);
  const [isGroceriesModalOpen, setIsGroceriesModalOpen] = useState(false);

  // Sync URL when currentView changes
  const navigateToView = (view: AppView, replaceHistory = false) => {
    setCurrentView(view);
    window.scrollTo({ top: 0, behavior: 'smooth' });

    let targetPath = '/';
    if (view === 'meal-planner') targetPath = '/meal-planner';
    else if (view === 'meal-planner-preference') targetPath = '/meal-planner/preference';
    else if (view === 'meal-planner-auth') targetPath = '/account/signup?next=/meal-planner/app';
    else if (view === 'meal-planner-app') targetPath = '/meal-planner/app';
    else if (view === 'onboarding') targetPath = '/onboarding';
    else if (view === 'chat') targetPath = '/chat';
    else if (view === 'landing') targetPath = '/home';

    if (window.location.pathname !== targetPath) {
      if (replaceHistory) {
        window.history.replaceState({}, '', targetPath);
      } else {
        window.history.pushState({}, '', targetPath);
      }
    }
  };

  // Popstate listener for browser Back/Forward buttons
  useEffect(() => {
    const handlePopState = () => {
      setCurrentView(getViewFromUrl());
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  // Update initial URL if launched directly on root
  useEffect(() => {
    if (window.location.pathname === '/' || window.location.pathname === '') {
      window.history.replaceState({}, '', '/meal-planner');
    }
  }, []);

  return (
    <MealPlannerProvider>
      <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] selection:bg-teal-100 selection:text-teal-950 flex flex-col justify-between">
        
        <div>
          {/* Bếp Nhà Mình Floating Navbar (Original Authentic Navbar from latest commit) */}
          {currentView !== 'meal-planner-auth' && (
            <Navbar
              currentView={currentView}
              onNavigate={(view) => {
                navigateToView(view as AppView);
              }}
              onOpenIntro={() => setIsIntroOpen(true)}
              onOpenAbout={() => setIsAboutOpen(true)}
            />
          )}

          {/* ====================================================
              MEAL PLANNER 4-STAGE FLOW SWITCHER
             ==================================================== */}
          
          {/* Giai đoạn 1: Trang giới thiệu & Thiết lập nhanh (/meal-planner - Image 2) */}
          {currentView === 'meal-planner' && (
            <MealPlannerLanding
              onNavigateToWizard={() => navigateToView('meal-planner-preference')}
              onSelectRecipePreview={(recipe) => setPreviewRecipe(recipe)}
            />
          )}

          {/* Giai đoạn 2: Khảo sát chi tiết khẩu vị & chế độ ăn (/meal-planner/preference - Image 3) */}
          {currentView === 'meal-planner-preference' && (
            <MealPlannerPreferences
              onBackToLanding={() => navigateToView('meal-planner')}
              onSaveAndContinue={() => navigateToView('meal-planner-auth')}
            />
          )}

          {/* Giai đoạn 3: Xác thực người dùng (/account/signup?next=/meal-planner/app - Image 4) */}
          {currentView === 'meal-planner-auth' && (
            <MealPlannerAuth
              nextUrl="/meal-planner/app"
              onSuccess={() => navigateToView('meal-planner-app')}
              onBackToPreferences={() => navigateToView('meal-planner-preference')}
            />
          )}

          {/* Giai đoạn 4: Bảng kế hoạch tuần tương tác & Kéo-Thả (/meal-planner/app - Image 5) */}
          {currentView === 'meal-planner-app' && (
            <MealPlannerApp
              onNavigateToPreferences={() => navigateToView('meal-planner-preference')}
            />
          )}

          {/* ====================================================
              ORIGINAL APPLICATION VIEWS (Preserved for compatibility)
             ==================================================== */}
          {currentView === 'landing' && (
            <HeroLanding
              onBeginNow={() => navigateToView('meal-planner')}
              onExploreDirectChat={() => navigateToView('chat')}
              onOpenIntro={() => setIsIntroOpen(true)}
            />
          )}

          {currentView === 'onboarding' && (
            <OnboardingFlow
              onComplete={(completedProfile) => {
                setFamilyProfile(completedProfile);
                navigateToView('chat');
              }}
              onBackToLanding={() => navigateToView('landing')}
            />
          )}

          {currentView === 'chat' && (
            <ChatbotInterface
              profile={familyProfile}
              onBackToHome={() => navigateToView('landing')}
              onEditProfile={() => navigateToView('onboarding')}
            />
          )}
        </div>

        {/* Global Recipe Detail Modal */}
        {previewRecipe && (
          <RecipeDetailModal
            recipe={previewRecipe}
            onClose={() => setPreviewRecipe(null)}
          />
        )}

        {/* Global Groceries Modal */}
        <GroceriesModal
          isOpen={isGroceriesModalOpen}
          onClose={() => setIsGroceriesModalOpen(false)}
        />

        {/* Modals for Floating Navigation bar */}
        <IntroductionModal
          isOpen={isIntroOpen}
          onClose={() => setIsIntroOpen(false)}
        />

        <AboutModal
          isOpen={isAboutOpen}
          onClose={() => setIsAboutOpen(false)}
        />

      </div>
    </MealPlannerProvider>
  );
}
