import React from 'react';

/**
 * 4 circular illustrated graphics inspired by SideChef "How it works" (Image 4):
 * 1. Tell Us Your Preferences (Phone + Carrot + Salmon + Mushroom + Checkmarks)
 * 2. Review Your Menu (Hands holding recipe cards + Swap badge)
 * 3. Optional: Shop Your Groceries Online (Blue grocery basket + ADD TO CART + cursor)
 * 4. Cook & Enjoy (Non-stick frying pan + tossing fresh ingredients)
 */

export const PreferencesIllustration: React.FC = () => (
  <svg
    viewBox="0 0 200 200"
    className="w-36 h-36 sm:w-44 sm:h-44 mx-auto select-none drop-shadow-sm transition-transform duration-300 hover:scale-105"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* Circle Background */}
    <circle cx="100" cy="100" r="88" fill="#FBEFE3" />

    {/* Fresh Mushroom top-left */}
    <g transform="translate(30, 32)">
      {/* Mushroom cap */}
      <path d="M12 28 C12 14, 38 14, 38 28 Z" fill="#F5EBE1" stroke="#4A3E3D" strokeWidth="2.5" />
      {/* Mushroom stem */}
      <path d="M21 28 L21 37 C21 39, 29 39, 29 37 L29 28" fill="#FFFFFF" stroke="#4A3E3D" strokeWidth="2.5" />
      {/* Badge Checkmark */}
      <circle cx="36" cy="36" r="8.5" fill="#1C3D47" />
      <path d="M32.5 36 L35 38.5 L39.5 33.5" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </g>

    {/* Crisp Carrot on the left */}
    <g transform="translate(18, 55)">
      {/* Carrot body */}
      <path d="M28 8 L40 55 C37 57, 33 57, 30 55 L20 12 Z" fill="#FB923C" stroke="#4A3E3D" strokeWidth="2.5" strokeLinejoin="round" />
      {/* Carrot creases */}
      <path d="M25 24 L31 22 M27 34 L33 33 M29 44 L34 43" stroke="#EA580C" strokeWidth="1.5" strokeLinecap="round" />
      {/* Carrot greens */}
      <path d="M24 10 C20 4, 18 -2, 22 -6 M25 9 C26 2, 28 -4, 27 -7 M27 10 C32 4, 36 -2, 33 -6" stroke="#22C55E" strokeWidth="2" strokeLinecap="round" />
      {/* Badge Checkmark */}
      <circle cx="38" cy="40" r="8.5" fill="#1C3D47" />
      <path d="M34.5 40 L37 42.5 L41.5 37.5" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </g>

    {/* Phone in the center */}
    <g transform="translate(68, 38) rotate(4)">
      {/* Phone body */}
      <rect x="0" y="0" width="62" height="106" rx="12" fill="#FFFFFF" stroke="#1F2937" strokeWidth="3.5" />
      {/* Phone screen */}
      <rect x="5" y="10" width="52" height="86" rx="6" fill="#F8FAFC" />
      {/* Speaker grill */}
      <line x1="26" y1="5" x2="36" y2="5" stroke="#9CA3AF" strokeWidth="2" strokeLinecap="round" />

      {/* Checklist items on screen */}
      {/* Item 1 */}
      <circle cx="16" cy="26" r="6" fill="#1C3D47" />
      <path d="M13.5 26 L15.5 28 L18.5 24.5" stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="26" y="23" width="25" height="5" rx="2.5" fill="#E2E8F0" />

      {/* Item 2 */}
      <circle cx="16" cy="44" r="6" fill="#1C3D47" />
      <path d="M13.5 44 L15.5 46 L18.5 42.5" stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="26" y="41" width="22" height="5" rx="2.5" fill="#E2E8F0" />

      {/* Item 3 */}
      <circle cx="16" cy="62" r="6" fill="#1C3D47" />
      <path d="M13.5 62 L15.5 64 L18.5 60.5" stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="26" y="59" width="24" height="5" rx="2.5" fill="#E2E8F0" />

      {/* Bottom home indicator */}
      <line x1="24" y1="91" x2="38" y2="91" stroke="#CBD5E1" strokeWidth="2.5" strokeLinecap="round" />
    </g>

    {/* Salmon fillet steak on bottom-right */}
    <g transform="translate(108, 92)">
      {/* Salmon skin & meat horseshoe */}
      <path d="M14 36 C4 20, 18 2, 42 6 C62 9, 64 28, 56 42 C48 48, 42 42, 36 32 C30 40, 24 44, 14 36 Z" fill="#FF8264" stroke="#4A3E3D" strokeWidth="2.5" strokeLinejoin="round" />
      {/* White fat stripes */}
      <path d="M24 16 C34 16, 48 20, 52 30 M28 22 C36 24, 44 26, 46 36" stroke="#FFF1EE" strokeWidth="2" strokeLinecap="round" />
      {/* Badge Checkmark */}
      <circle cx="48" cy="46" r="8.5" fill="#1C3D47" />
      <path d="M44.5 46 L47 48.5 L51.5 43.5" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </g>
  </svg>
);

export const MenuReviewIllustration: React.FC = () => (
  <svg
    viewBox="0 0 200 200"
    className="w-36 h-36 sm:w-44 sm:h-44 mx-auto select-none drop-shadow-sm transition-transform duration-300 hover:scale-105"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* Circle Background */}
    <circle cx="100" cy="100" r="88" fill="#FBEFE3" />

    {/* Two Recipe Cards */}
    {/* Left card: Orange savory dish */}
    <g transform="translate(42, 38) rotate(-6)">
      <rect x="0" y="0" width="48" height="66" rx="8" fill="#FFFFFF" stroke="#4A3E3D" strokeWidth="2" />
      {/* Dish photo area */}
      <rect x="4" y="4" width="40" height="35" rx="5" fill="#FFEDD5" />
      {/* Illustrated food in photo */}
      <circle cx="24" cy="21" r="12" fill="#F97316" />
      <path d="M17 21 C19 16, 29 16, 31 21" stroke="#EA580C" strokeWidth="2" strokeLinecap="round" />
      <circle cx="22" cy="18" r="2" fill="#22C55E" />
      <circle cx="27" cy="22" r="1.5" fill="#22C55E" />
      {/* Text lines */}
      <rect x="6" y="44" width="28" height="4" rx="2" fill="#4A3E3D" />
      <rect x="6" y="52" width="18" height="3" rx="1.5" fill="#9CA3AF" />
    </g>

    {/* Right card: Green stir-fry / soup with swap button */}
    <g transform="translate(88, 46) rotate(8)">
      <rect x="0" y="0" width="48" height="66" rx="8" fill="#FFFFFF" stroke="#4A3E3D" strokeWidth="2" />
      {/* Dish photo area */}
      <rect x="4" y="4" width="40" height="35" rx="5" fill="#ECFDF5" />
      {/* Illustrated food in photo */}
      <circle cx="24" cy="21" r="12" fill="#10B981" />
      <circle cx="21" cy="19" r="3" fill="#EF4444" />
      <circle cx="26" cy="22" r="2.5" fill="#F59E0B" />
      {/* Text lines */}
      <rect x="6" y="44" width="26" height="4" rx="2" fill="#4A3E3D" />
      <rect x="6" y="52" width="16" height="3" rx="1.5" fill="#9CA3AF" />

      {/* Red Swap/Close Badge */}
      <circle cx="44" cy="4" r="8.5" fill="#EF4444" stroke="#FFFFFF" strokeWidth="1.5" />
      <path d="M41 1 L47 7 M47 1 L41 7" stroke="#FFFFFF" strokeWidth="1.8" strokeLinecap="round" />
    </g>

    {/* Two Hands holding the cards from bottom */}
    {/* Left Hand */}
    <path d="M26 180 C34 140, 52 110, 68 102 C72 100, 78 103, 76 109 C72 118, 62 134, 58 180 Z" fill="#C58B68" stroke="#4A3E3D" strokeWidth="2.5" strokeLinejoin="round" />
    {/* Left thumb */}
    <path d="M60 112 C64 105, 72 108, 70 116 C67 122, 64 126, 61 128" fill="#C58B68" stroke="#4A3E3D" strokeWidth="2" />

    {/* Right Hand */}
    <path d="M174 180 C166 140, 148 114, 132 106 C128 104, 122 107, 124 113 C128 122, 138 136, 142 180 Z" fill="#C58B68" stroke="#4A3E3D" strokeWidth="2.5" strokeLinejoin="round" />
    {/* Right thumb */}
    <path d="M140 114 C136 108, 128 110, 130 118 C133 124, 136 128, 139 130" fill="#C58B68" stroke="#4A3E3D" strokeWidth="2" />

    {/* Small pill button in center: VIEW RECIPE */}
    <g transform="translate(72, 122)">
      <rect x="0" y="0" width="56" height="16" rx="8" fill="#1C3D47" />
      <text x="28" y="11" fill="#FFFFFF" fontSize="7" fontWeight="bold" textAnchor="middle" fontFamily="sans-serif">
        VIEW RECIPES
      </text>
    </g>
  </svg>
);

export const GroceryShopIllustration: React.FC = () => (
  <svg
    viewBox="0 0 200 200"
    className="w-36 h-36 sm:w-44 sm:h-44 mx-auto select-none drop-shadow-sm transition-transform duration-300 hover:scale-105"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* Circle Background */}
    <circle cx="100" cy="100" r="88" fill="#FBEFE3" />

    {/* Groceries inside basket */}
    <g transform="translate(62, 38)">
      {/* Loaf of Baguette/Bread */}
      <path d="M14 45 L26 8 C28 4, 34 5, 36 10 L44 45 Z" fill="#E0A96D" stroke="#4A3E3D" strokeWidth="2" strokeLinejoin="round" />
      <path d="M23 20 L28 18 M26 29 L32 27 M29 38 L35 36" stroke="#B45309" strokeWidth="1.5" strokeLinecap="round" />

      {/* Sauce/Oil bottle */}
      <rect x="42" y="18" width="16" height="34" rx="4" fill="#FDE047" stroke="#4A3E3D" strokeWidth="2" />
      <rect x="46" y="10" width="8" height="8" rx="2" fill="#DC2626" stroke="#4A3E3D" strokeWidth="2" />
      <rect x="44" y="27" width="12" height="12" rx="2" fill="#FFFFFF" />

      {/* Celery/Greens */}
      <path d="M60 45 C66 30, 74 18, 80 14 M60 45 C72 32, 84 26, 88 22" stroke="#22C55E" strokeWidth="3" strokeLinecap="round" />
    </g>

    {/* Blue Grocery Hand Basket */}
    <g transform="translate(45, 68)">
      {/* Basket Handle */}
      <path d="M18 20 C18 -6, 92 -6, 92 20" stroke="#0284C7" strokeWidth="3.5" fill="none" strokeLinecap="round" />

      {/* Basket Body */}
      <path d="M8 18 L18 64 C20 68, 90 68, 92 64 L102 18 Z" fill="#0284C7" stroke="#0369A1" strokeWidth="2.5" strokeLinejoin="round" />

      {/* Basket Grid Lines / Slots */}
      <g stroke="#38BDF8" strokeWidth="2" strokeLinecap="round">
        <line x1="22" y1="28" x2="88" y2="28" />
        <line x1="26" y1="40" x2="84" y2="40" />
        <line x1="30" y1="52" x2="80" y2="52" />
        <line x1="34" y1="22" x2="40" y2="60" />
        <line x1="48" y1="22" x2="52" y2="60" />
        <line x1="62" y1="22" x2="62" y2="60" />
        <line x1="76" y1="22" x2="72" y2="60" />
      </g>
    </g>

    {/* Floating Pill: ADD TO CART */}
    <g transform="translate(48, 126)">
      <rect x="0" y="0" width="104" height="28" rx="14" fill="#0F172A" filter="drop-shadow(0 6px 12px rgba(0,0,0,0.18))" />
      <text x="48" y="18" fill="#FFFFFF" fontSize="9.5" fontWeight="bold" textAnchor="middle" fontFamily="sans-serif" letterSpacing="0.5">
        ADD TO CART
      </text>
      {/* Sparkles on button */}
      <path d="M88 10 L89.5 13.5 L93 15 L89.5 16.5 L88 20 L86.5 16.5 L83 15 L86.5 13.5 Z" fill="#FDE047" />

      {/* Mouse Cursor Pointer clicking button */}
      <g transform="translate(86, 14)">
        <path d="M0 0 L0 18 L4.5 13.5 L9 22 L12 20.5 L7.5 12 L13.5 12 Z" fill="#FFFFFF" stroke="#0F172A" strokeWidth="1.5" strokeLinejoin="round" />
      </g>
    </g>
  </svg>
);

export const CookAndEnjoyIllustration: React.FC = () => (
  <svg
    viewBox="0 0 200 200"
    className="w-36 h-36 sm:w-44 sm:h-44 mx-auto select-none drop-shadow-sm transition-transform duration-300 hover:scale-105"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* Circle Background */}
    <circle cx="100" cy="100" r="88" fill="#FBEFE3" />

    {/* Flying Tossing Ingredients in mid-air */}
    {/* Tomato slice */}
    <g transform="translate(68, 38)">
      <circle cx="16" cy="16" r="14" fill="#EF4444" stroke="#4A3E3D" strokeWidth="2" />
      {/* 4 seed chambers */}
      <circle cx="12" cy="12" r="3" fill="#DC2626" />
      <circle cx="20" cy="12" r="3" fill="#DC2626" />
      <circle cx="12" cy="20" r="3" fill="#DC2626" />
      <circle cx="20" cy="20" r="3" fill="#DC2626" />
      <circle cx="13" cy="13" r="1" fill="#FEF08A" />
      <circle cx="19" cy="13" r="1" fill="#FEF08A" />
      <circle cx="13" cy="19" r="1" fill="#FEF08A" />
      <circle cx="19" cy="19" r="1" fill="#FEF08A" />
    </g>

    {/* Zucchini / cucumber slice */}
    <g transform="translate(112, 34)">
      <circle cx="12" cy="12" r="10" fill="#86EFAC" stroke="#15803D" strokeWidth="2.5" />
      <circle cx="12" cy="12" r="6" fill="#DCFCE7" />
    </g>

    {/* Purple onion ring */}
    <g transform="translate(48, 64) rotate(-15)">
      <ellipse cx="16" cy="12" rx="14" ry="10" stroke="#A855F7" strokeWidth="3" fill="none" />
      <ellipse cx="16" cy="12" rx="10" ry="7" stroke="#E9D5FF" strokeWidth="2" fill="none" />
    </g>

    {/* Red bell pepper curved strip */}
    <g transform="translate(86, 70)">
      <path d="M4 14 C12 2, 28 6, 26 22 C22 28, 12 26, 8 18" stroke="#DC2626" strokeWidth="4.5" strokeLinecap="round" fill="none" />
    </g>

    {/* Carrot slice */}
    <g transform="translate(122, 60)">
      <circle cx="8" cy="8" r="7" fill="#F97316" stroke="#4A3E3D" strokeWidth="1.8" />
      <circle cx="8" cy="8" r="3" fill="#FB923C" />
    </g>

    {/* Fresh basil/herb leaf */}
    <g transform="translate(132, 78) rotate(30)">
      <path d="M0 10 C8 0, 18 2, 20 12 C14 18, 4 16, 0 10 Z" fill="#22C55E" stroke="#166534" strokeWidth="1.5" />
      <line x1="2" y1="10" x2="16" y2="10" stroke="#166534" strokeWidth="1" />
    </g>

    {/* Non-stick Skillet / Frying Pan */}
    <g transform="translate(38, 85)">
      {/* Pan Handle pointing right */}
      <path d="M102 32 L146 54 C150 56, 150 62, 145 64 L138 67 C134 68, 130 66, 126 63 L88 38 Z" fill="#334155" stroke="#1E293B" strokeWidth="2.5" strokeLinejoin="round" />
      {/* Pan Body */}
      <ellipse cx="62" cy="38" rx="46" ry="24" fill="#1E293B" stroke="#0F172A" strokeWidth="3" />
      {/* Pan Rim interior reflection */}
      <ellipse cx="62" cy="36" rx="42" ry="20" fill="#334155" />
      <ellipse cx="62" cy="38" rx="36" ry="16" fill="#1E293B" />
    </g>

    {/* Toss Sizzle Curved Motion Lines */}
    <path d="M60 84 C56 74, 62 60, 72 54" stroke="#94A3B8" strokeWidth="2" strokeDasharray="3 3" strokeLinecap="round" fill="none" />
    <path d="M120 86 C126 76, 122 64, 116 56" stroke="#94A3B8" strokeWidth="2" strokeDasharray="3 3" strokeLinecap="round" fill="none" />
  </svg>
);
