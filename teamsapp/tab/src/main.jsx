import React from "react";
import { createRoot } from "react-dom/client";
import { app } from "@microsoft/teams-js";
import Dashboard from "./Dashboard.jsx";

// Initialise the Teams SDK (safe to call outside Teams too — it no-ops).
app.initialize().catch(() => {
  /* running outside Teams (e.g. plain browser dev) */
});

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <Dashboard />
  </React.StrictMode>
);

