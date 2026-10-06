import React, { useEffect, useState } from "react";

// Point this at your Spring backend. In production use the deployed URL and
// make sure the backend CORS config allows the Teams tab host.
const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/dashboard`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <Shell><p style={{ color: "#D13438" }}>Failed to load: {error}</p></Shell>;
  if (!data) return <Shell><p>Loading standup…</p></Shell>;

  return (
    <Shell>
      <header style={styles.header}>
        <h1 style={styles.h1}>🐞 BugBot — Daily Standup</h1>
        <span style={styles.subtle}>{data.totalActive} active tickets</span>
      </header>

      {/* The LLM 3-bullet summary */}
      <section style={styles.summary}>
        {data.standupBullets.map((b, i) => (
          <div key={i} style={styles.bullet}>{b.replace(/^-\s*/, "• ")}</div>
        ))}
      </section>

      {/* Tickets grouped by role */}
      {data.roleGroups.map((group) => (
        <section key={group.role} style={styles.group}>
          <h2 style={styles.h2}>
            {group.role}
            <span style={styles.pill}>{group.total}</span>
            {group.highSeverity > 0 && (
              <span style={{ ...styles.pill, ...styles.pillDanger }}>
                {group.highSeverity} high-sev
              </span>
            )}
          </h2>

          {group.tickets.map((t) => (
            <article key={t.key} style={styles.ticket}>
              <div>
                <strong>
                  {t.highSeverity ? "🔴 " : "⚪ "}
                  {t.key}
                </strong>{" "}
                {t.summary}
                <div style={styles.meta}>
                  {t.status} · {t.priority} · {t.assignee}
                </div>
              </div>
              <div style={styles.actions}>
                <a href={t.jiraUrl} target="_blank" rel="noreferrer" style={styles.link}>
                  Jira
                </a>
                {/* Contextual jump link: opens the failing code / trace in VS Code */}
                <a href={t.vsCodeDeepLink} style={styles.linkPrimary}>
                  Open in VS Code
                </a>
              </div>
            </article>
          ))}
        </section>
      ))}
    </Shell>
  );
}

function Shell({ children }) {
  return <div style={styles.shell}>{children}</div>;
}

const styles = {
  shell: { fontFamily: "Segoe UI, system-ui, sans-serif", padding: 16, maxWidth: 820, margin: "0 auto", color: "#242424" },
  header: { display: "flex", alignItems: "baseline", gap: 12 },
  h1: { fontSize: 22, margin: 0 },
  subtle: { color: "#616161", fontSize: 13 },
  summary: { background: "#F5F5F5", borderRadius: 8, padding: 12, margin: "12px 0" },
  bullet: { padding: "4px 0", fontSize: 15 },
  group: { marginTop: 20 },
  h2: { fontSize: 16, display: "flex", alignItems: "center", gap: 8, borderBottom: "1px solid #E0E0E0", paddingBottom: 6 },
  pill: { background: "#E8E8E8", borderRadius: 10, padding: "2px 8px", fontSize: 12, fontWeight: 600 },
  pillDanger: { background: "#FDE7E9", color: "#D13438" },
  ticket: { display: "flex", justifyContent: "space-between", gap: 12, padding: "10px 0", borderBottom: "1px solid #F0F0F0" },
  meta: { color: "#616161", fontSize: 12, marginTop: 2 },
  actions: { display: "flex", gap: 8, alignItems: "center", whiteSpace: "nowrap" },
  link: { color: "#5B5FC7", textDecoration: "none", fontSize: 13 },
  linkPrimary: { background: "#5B5FC7", color: "#fff", borderRadius: 6, padding: "4px 10px", textDecoration: "none", fontSize: 13 },
};

