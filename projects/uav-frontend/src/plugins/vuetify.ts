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
          background: "#eaf1f6",
          surface: "#f7fbff",
          primary: "#0f4c81",
          secondary: "#0fa3b1",
          accent: "#7cc6d6",
          info: "#2f8fb1",
          success: "#2e9b57",
          warning: "#e0a100",
          error: "#d64545",
        },
      },
    },
  },
});
