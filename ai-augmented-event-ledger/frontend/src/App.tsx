import { FormEvent, useEffect, useMemo, useState } from "react";

type EventType = "CREDIT" | "DEBIT";

type EventResponse = {
  eventId: string;
  accountId: string;
  type: EventType;
  amount: number;
  currency: string;
  eventTimestamp: string;
  metadata: Record<string, unknown>;
  receivedAt: string;
  duplicate: boolean;
};

type AuditEntry = {
  id: number;
  eventId: string;
  accountId: string;
  action: string;
  traceId: string;
  detail: string;
  createdAt: string;
};

type BalanceResponse = {
  accountId: string;
  balance: number;
  currency: string;
};

type AccountDetailsResponse = {
  accountId: string;
  balance: number;
  currency: string;
  recentTransactions: Array<{
    eventId: string;
    type: EventType;
    amount: number;
    currency: string;
    eventTimestamp: string;
  }>;
};

type HealthResponse = {
  service: string;
  status: string;
  timestamp: string;
  diagnostics: Record<string, unknown>;
};

type MetricsSnapshot = {
  service: string;
  requestCounts: Record<string, number>;
  errorCounts: Record<string, number>;
};

type ApiError = {
  error?: string;
  messages?: string[];
  traceId?: string;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

const initialEvent = {
  eventId: "evt-001",
  accountId: "acct-123",
  type: "CREDIT" as EventType,
  amount: "150.00",
  currency: "USD",
  eventTimestamp: "2026-05-15T14:02:11Z",
  metadata: '{\n  "source": "ui-console",\n  "channel": "react"\n}'
};

const emptyState = "No data loaded yet";

async function request<T>(path: string, options: RequestInit = {}): Promise<{ data: T; traceId: string | null; status: number }> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  const traceId = response.headers.get("X-Trace-Id");
  const contentType = response.headers.get("Content-Type") || "";
  const body = contentType.includes("application/json") ? await response.json() : await response.text();
  if (!response.ok) {
    const errorBody = body as ApiError;
    const message = errorBody.messages?.join(", ") || errorBody.error || String(body) || response.statusText;
    throw new Error(message);
  }
  return { data: body as T, traceId, status: response.status };
}

function formatMoney(value: number | string | undefined, currency = "USD") {
  const numeric = Number(value || 0);
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(numeric);
}

function formatTime(value?: string) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
}

function statusLabel(status?: number) {
  if (status === 201) {
    return "Created";
  }
  if (status === 202) {
    return "Queued";
  }
  if (status === 200) {
    return "OK";
  }
  return status ? String(status) : "Ready";
}

export function App() {
  const [eventForm, setEventForm] = useState(initialEvent);
  const [eventResult, setEventResult] = useState<EventResponse | null>(null);
  const [submitStatus, setSubmitStatus] = useState<number | undefined>();
  const [submitTrace, setSubmitTrace] = useState<string | null>(null);
  const [eventsAccount, setEventsAccount] = useState("acct-123");
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [eventLookupId, setEventLookupId] = useState("evt-001");
  const [selectedEvent, setSelectedEvent] = useState<EventResponse | null>(null);
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [accountId, setAccountId] = useState("acct-123");
  const [balance, setBalance] = useState<BalanceResponse | null>(null);
  const [account, setAccount] = useState<AccountDetailsResponse | null>(null);
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [metrics, setMetrics] = useState<MetricsSnapshot | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const netMovement = useMemo(() => {
    return events.reduce((total, event) => total + (event.type === "CREDIT" ? Number(event.amount) : -Number(event.amount)), 0);
  }, [events]);

  useEffect(() => {
    void refreshOps();
  }, []);

  async function run<T>(label: string, action: () => Promise<T>) {
    setBusy(label);
    setError(null);
    try {
      return await action();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : String(caught));
      return undefined;
    } finally {
      setBusy(null);
    }
  }

  async function submitEvent(event: FormEvent) {
    event.preventDefault();
    await run("Submitting event", async () => {
      const metadata = eventForm.metadata.trim() ? JSON.parse(eventForm.metadata) : {};
      const response = await request<EventResponse>("/events", {
        method: "POST",
        headers: { "X-Trace-Id": `ui-${Date.now().toString(16)}` },
        body: JSON.stringify({
          eventId: eventForm.eventId,
          accountId: eventForm.accountId,
          type: eventForm.type,
          amount: Number(eventForm.amount),
          currency: eventForm.currency,
          eventTimestamp: eventForm.eventTimestamp,
          metadata
        })
      });
      setEventResult(response.data);
      setSubmitStatus(response.status);
      setSubmitTrace(response.traceId);
      setEventsAccount(eventForm.accountId);
      setEventLookupId(eventForm.eventId);
      await Promise.all([loadEvents(eventForm.accountId), loadAudit(eventForm.eventId), loadAccount(eventForm.accountId), refreshOps()]);
    });
  }

  async function loadEvents(account: string = eventsAccount) {
    const response = await request<EventResponse[]>(`/events?account=${encodeURIComponent(account)}`);
    setEvents(response.data);
  }

  async function loadSelectedEvent(id: string = eventLookupId) {
    const response = await request<EventResponse>(`/events/${encodeURIComponent(id)}`);
    setSelectedEvent(response.data);
  }

  async function loadAudit(id: string = eventLookupId) {
    const response = await request<AuditEntry[]>(`/events/${encodeURIComponent(id)}/audit`);
    setAudit(response.data);
  }

  async function loadAccount(id: string = accountId) {
    const [balanceResponse, accountResponse] = await Promise.all([
      request<BalanceResponse>(`/accounts/${encodeURIComponent(id)}/balance`),
      request<AccountDetailsResponse>(`/accounts/${encodeURIComponent(id)}`)
    ]);
    setBalance(balanceResponse.data);
    setAccount(accountResponse.data);
  }

  async function refreshOps() {
    const [healthResponse, metricsResponse] = await Promise.all([
      request<HealthResponse>("/health"),
      request<MetricsSnapshot>("/metrics")
    ]);
    setHealth(healthResponse.data);
    setMetrics(metricsResponse.data);
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">AI-Augmented Event Ledger</p>
          <h1>Operations Console</h1>
        </div>
        <div className="topbar-actions">
          <button type="button" onClick={() => void run("Refreshing operations", refreshOps)}>
            Refresh
          </button>
          <span className={`status ${health?.status === "UP" ? "up" : "warn"}`}>{health?.status || "Unknown"}</span>
        </div>
      </header>

      {error && <div className="alert">{error}</div>}
      {busy && <div className="progress">{busy}</div>}

      <section className="summary-grid">
        <MetricTile label="Gateway Status" value={health?.status || "-"} tone={health?.status === "UP" ? "green" : "red"} />
        <MetricTile label="Event Rows" value={String(health?.diagnostics?.eventRows ?? "-")} tone="blue" />
        <MetricTile label="Audit Rows" value={String(health?.diagnostics?.auditRows ?? "-")} tone="pink" />
        <MetricTile label="Pending Outbox" value={String(health?.diagnostics?.pendingAccountEvents ?? "-")} tone="amber" />
      </section>

      <section className="workspace">
        <div className="panel submit-panel">
          <div className="panel-heading">
            <h2>Submit Event</h2>
            <span>{statusLabel(submitStatus)}</span>
          </div>
          <form onSubmit={submitEvent} className="form-grid">
            <label>
              Event ID
              <input value={eventForm.eventId} onChange={(event) => setEventForm({ ...eventForm, eventId: event.target.value })} />
            </label>
            <label>
              Account ID
              <input value={eventForm.accountId} onChange={(event) => setEventForm({ ...eventForm, accountId: event.target.value })} />
            </label>
            <label>
              Type
              <select value={eventForm.type} onChange={(event) => setEventForm({ ...eventForm, type: event.target.value as EventType })}>
                <option value="CREDIT">CREDIT</option>
                <option value="DEBIT">DEBIT</option>
              </select>
            </label>
            <label>
              Amount
              <input value={eventForm.amount} inputMode="decimal" onChange={(event) => setEventForm({ ...eventForm, amount: event.target.value })} />
            </label>
            <label>
              Currency
              <input value={eventForm.currency} maxLength={3} onChange={(event) => setEventForm({ ...eventForm, currency: event.target.value.toUpperCase() })} />
            </label>
            <label>
              Event Timestamp
              <input value={eventForm.eventTimestamp} onChange={(event) => setEventForm({ ...eventForm, eventTimestamp: event.target.value })} />
            </label>
            <label className="span-2">
              Metadata JSON
              <textarea value={eventForm.metadata} onChange={(event) => setEventForm({ ...eventForm, metadata: event.target.value })} />
            </label>
            <button type="submit" className="primary">Submit Transaction Event</button>
          </form>
          <ResultStrip eventResult={eventResult} traceId={submitTrace} status={submitStatus} />
        </div>

        <div className="panel">
          <div className="panel-heading">
            <h2>Account Query</h2>
            <span>{balance ? formatMoney(balance.balance, balance.currency) : "Balance"}</span>
          </div>
          <div className="inline-query">
            <input value={accountId} onChange={(event) => setAccountId(event.target.value)} />
            <button type="button" onClick={() => void run("Loading account", () => loadAccount())}>Load</button>
          </div>
          {account ? (
            <div className="account-block">
              <div className="balance-line">
                <span>{account.accountId}</span>
                <strong>{formatMoney(account.balance, account.currency)}</strong>
              </div>
              <DataTable
                headers={["Event", "Type", "Amount", "Timestamp"]}
                rows={account.recentTransactions.map((transaction) => [
                  transaction.eventId,
                  transaction.type,
                  formatMoney(transaction.amount, transaction.currency),
                  formatTime(transaction.eventTimestamp)
                ])}
                empty={emptyState}
              />
            </div>
          ) : (
            <p className="empty">{emptyState}</p>
          )}
        </div>
      </section>

      <section className="workspace">
        <div className="panel wide">
          <div className="panel-heading">
            <h2>Ledger Events</h2>
            <span>Net {formatMoney(netMovement)}</span>
          </div>
          <div className="inline-query">
            <input value={eventsAccount} onChange={(event) => setEventsAccount(event.target.value)} />
            <button type="button" onClick={() => void run("Loading events", () => loadEvents())}>List Events</button>
          </div>
          <DataTable
            headers={["Event", "Type", "Amount", "Occurred", "Received", "Duplicate"]}
            rows={events.map((event) => [
              event.eventId,
              event.type,
              formatMoney(event.amount, event.currency),
              formatTime(event.eventTimestamp),
              formatTime(event.receivedAt),
              event.duplicate ? "Yes" : "No"
            ])}
            empty={emptyState}
          />
        </div>
      </section>

      <section className="workspace">
        <div className="panel">
          <div className="panel-heading">
            <h2>Event Detail</h2>
            <span>{selectedEvent?.eventId || "Lookup"}</span>
          </div>
          <div className="inline-query">
            <input value={eventLookupId} onChange={(event) => setEventLookupId(event.target.value)} />
            <button type="button" onClick={() => void run("Loading event detail", async () => {
              await loadSelectedEvent();
              await loadAudit();
            })}>Load</button>
          </div>
          {selectedEvent ? (
            <dl className="detail-list">
              <div><dt>Account</dt><dd>{selectedEvent.accountId}</dd></div>
              <div><dt>Type</dt><dd>{selectedEvent.type}</dd></div>
              <div><dt>Amount</dt><dd>{formatMoney(selectedEvent.amount, selectedEvent.currency)}</dd></div>
              <div><dt>Occurred</dt><dd>{formatTime(selectedEvent.eventTimestamp)}</dd></div>
              <div><dt>Metadata</dt><dd><code>{JSON.stringify(selectedEvent.metadata)}</code></dd></div>
            </dl>
          ) : (
            <p className="empty">{emptyState}</p>
          )}
        </div>

        <div className="panel">
          <div className="panel-heading">
            <h2>Audit Trail</h2>
            <span>{audit.length} entries</span>
          </div>
          <div className="timeline">
            {audit.length === 0 && <p className="empty">{emptyState}</p>}
            {audit.map((entry) => (
              <div className="timeline-row" key={entry.id}>
                <span className="dot" />
                <div>
                  <strong>{entry.action.replaceAll("_", " ")}</strong>
                  <p>{entry.detail}</p>
                  <small>{formatTime(entry.createdAt)} · {entry.traceId}</small>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="workspace">
        <div className="panel wide">
          <div className="panel-heading">
            <h2>Gateway Metrics</h2>
            <span>{metrics?.service || "Metrics"}</span>
          </div>
          <MetricBars metrics={metrics} />
        </div>
      </section>
    </main>
  );
}

function MetricTile({ label, value, tone }: { label: string; value: string; tone: string }) {
  return (
    <div className={`metric-tile ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ResultStrip({ eventResult, status, traceId }: { eventResult: EventResponse | null; status?: number; traceId: string | null }) {
  if (!eventResult) {
    return <p className="empty">Submit an event to see response status, idempotency, and trace correlation.</p>;
  }
  return (
    <div className="result-strip">
      <span>{statusLabel(status)}</span>
      <span>{eventResult.duplicate ? "Duplicate" : "Original"}</span>
      <span>{eventResult.eventId}</span>
      <span>{traceId || "trace returned in logs"}</span>
    </div>
  );
}

function DataTable({ headers, rows, empty }: { headers: string[]; rows: string[][]; empty: string }) {
  if (rows.length === 0) {
    return <p className="empty">{empty}</p>;
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={row.join("-") || rowIndex}>
              {row.map((cell, cellIndex) => <td key={`${cell}-${cellIndex}`}>{cell}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MetricBars({ metrics }: { metrics: MetricsSnapshot | null }) {
  const entries = Object.entries(metrics?.requestCounts || {});
  const max = Math.max(1, ...entries.map(([, value]) => value));
  if (entries.length === 0) {
    return <p className="empty">{emptyState}</p>;
  }
  return (
    <div className="bars">
      {entries.map(([endpoint, count]) => {
        const errors = metrics?.errorCounts[endpoint] || 0;
        return (
          <div className="bar-row" key={endpoint}>
            <span>{endpoint}</span>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${Math.max(6, (count / max) * 100)}%` }} />
            </div>
            <strong>{count}</strong>
            <em>{errors} errors</em>
          </div>
        );
      })}
    </div>
  );
}
