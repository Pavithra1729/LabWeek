import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Teams tabs must be served over HTTPS in production. For local dev the
// Teams Toolkit provides a dev tunnel; `vite --port 53000` matches the
// CORS origin allowed by the Spring backend.
export default defineConfig({
  plugins: [react()],
  server: { port: 53000 },
});

