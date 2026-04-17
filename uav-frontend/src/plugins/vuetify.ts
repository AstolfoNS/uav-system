// src/plugins/vuetify.ts
import "vuetify/styles";
import "@mdi/font/css/materialdesignicons.css";
import { createVuetify } from "vuetify";

export default createVuetify({
  theme: {
    defaultTheme: "uavLight",
    themes: {
      uavLight: {
        dark: false,
        colors: {
          background: "#f1f8fd",
          surface: "#ffffff",
          primary: "#24a9f0",
          secondary: "#1a84c3",
          accent: "#80d9ff",
          info: "#36b8ff",
          success: "#2f9e44",
          warning: "#f2a93a",
          error: "#d62828",
        },
      },
    },
  },
});
