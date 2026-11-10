/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#1f108e",
          container: "#3730a3", // Indigo #3730A3
          light: "#e2dfff",
          dark: "#0f0069",
        },
        secondary: {
          DEFAULT: "#0d9488", // Teal #0D9488
          container: "#86f2e4",
          light: "#89f5e7",
          dark: "#00201d",
        },
        surface: {
          DEFAULT: "#f7f9fb",
          bright: "#ffffff",
          low: "#f2f4f6",
          container: "#eceef0",
          high: "#e6e8ea",
          highest: "#e0e3e5",
        },
        status: {
          new: {
            bg: "#eff6ff",
            text: "#1d4ed8",
            border: "#bfdbfe",
          },
          assigned: {
            bg: "#f5f3ff",
            text: "#6d28d9",
            border: "#ddd6fe",
          },
          inProgress: {
            bg: "#f0fdfa",
            text: "#0f766e",
            border: "#99f6e4",
          },
          overdue: {
            bg: "#fef2f2",
            text: "#b91c1c",
            border: "#fecaca",
          },
          escalated: {
            bg: "#450a0a",
            text: "#fecaca",
            border: "#dc2626",
          },
          resolved: {
            bg: "#ecfdf5",
            text: "#047857",
            border: "#a7f3d0",
          },
          rejected: {
            bg: "#f1f5f9",
            text: "#475569",
            border: "#cbd5e1",
          },
        },
        gov: {
          karnataka: "#f59e0b", // Saffron/Gold accent
          navy: "#1e1b4b",
          green: "#047857",
          alert: "#dc2626",
        }
      },
      fontFamily: {
        sans: ["Inter", '"Noto Sans Kannada"', "sans-serif"],
        kannada: ['"Noto Sans Kannada"', "sans-serif"],
      },
    },
  },
  plugins: [],
}

