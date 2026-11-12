/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: {
          50: '#f7f8f9',
          100: '#eceef1',
          200: '#dcdfe4',
          300: '#c3c8d0',
          400: '#9aa1ac',
          500: '#6f7783',
          600: '#525a66',
          700: '#3d444e',
          800: '#2b3038',
          900: '#1c2027',
        },
        accent: {
          50: '#eef4ff',
          100: '#dbe7ff',
          200: '#bfd3ff',
          500: '#2f6fed',
          600: '#2459c9',
          700: '#1c478f',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', 'sans-serif'],
        mono: ['SFMono-Regular', 'Consolas', 'Liberation Mono', 'monospace'],
      },
    },
  },
  plugins: [],
};