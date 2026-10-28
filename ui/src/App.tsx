/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { HeroLanding } from './components/HeroLanding';
import { OnboardingFlow } from './components/OnboardingFlow';
import { ChatbotInterface } from './components/ChatbotInterface';
import { IntroductionModal, AboutModal, ContactModal } from './components/InfoModals';
import { FamilyProfile } from './types';
import { INITIAL_DEFAULT_PROFILE } from './data/mockData';

export default function App() {
  const [currentView, setCurrentView] = useState<'landing' | 'onboarding' | 'chat'>('landing');
  const [familyProfile, setFamilyProfile] = useState<FamilyProfile>(INITIAL_DEFAULT_PROFILE);
  const [isIntroOpen, setIsIntroOpen] = useState(false);
  const [isAboutOpen, setIsAboutOpen] = useState(false);
  const [isContactOpen, setIsContactOpen] = useState(false);

  const handleStartOnboarding = () => {
    setCurrentView('onboarding');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCompleteOnboarding = (completedProfile: FamilyProfile) => {
    setFamilyProfile(completedProfile);
    setCurrentView('chat');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDirectToChat = () => {
    setCurrentView('chat');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen bg-[#FDFBF7] text-[#27211D] selection:bg-orange-200 selection:text-orange-950">
      
      {/* Flat Navbar (Tầng 1) */}
      <Navbar
        currentView={currentView}
        onNavigate={(view) => {
          setCurrentView(view);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
        onOpenIntro={() => setIsIntroOpen(true)}
        onOpenAbout={() => setIsAboutOpen(true)}
        onOpenContact={() => setIsContactOpen(true)}
      />

      {/* Main View Switcher */}
      {currentView === 'landing' && (
        <HeroLanding
          onBeginNow={handleStartOnboarding}
          onExploreDirectChat={handleDirectToChat}
          onOpenIntro={() => setIsIntroOpen(true)}
        />
      )}

      {currentView === 'onboarding' && (
        <OnboardingFlow
          onComplete={handleCompleteOnboarding}
          onBackToLanding={() => {
            setCurrentView('landing');
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
        />
      )}

      {currentView === 'chat' && (
        <ChatbotInterface
          profile={familyProfile}
          onBackToHome={() => {
            setCurrentView('landing');
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
          onEditProfile={() => {
            setCurrentView('onboarding');
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
        />
      )}

      {/* Modals for Floating Navigation bar */}
      <IntroductionModal
        isOpen={isIntroOpen}
        onClose={() => setIsIntroOpen(false)}
      />

      <AboutModal
        isOpen={isAboutOpen}
        onClose={() => setIsAboutOpen(false)}
      />

      <ContactModal
        isOpen={isContactOpen}
        onClose={() => setIsContactOpen(false)}
      />

    </div>
  );
}
