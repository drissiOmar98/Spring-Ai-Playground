@param String modelName
<!DOCTYPE html>
<html lang="en" class="h-full">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tool Stream · Spring AI</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        /* "Paper" palette — every color in the app resolves through these,
           so retheming is a matter of changing this one block. */
        :root {
            --bg: #fafaf9;
            --surface: #ffffff;
            --surface-2: #f5f5f4;
            --border: #e7e5e4;
            --border-strong: #d6d3d1;
            --text: #1c1917;
            --text-soft: #44403c;
            --text-muted: #78716c;
            --text-faint: #a8a29e;
            --accent: #059669;
            --accent-hover: #047857;
            --accent-text: #047857;
            --accent-soft: rgba(5, 150, 105, 0.08);
            --accent-border: #a7f3d0;
        }
        .cursor::after {
            content: '▍';
            margin-left: 1px;
            animation: blink 1s steps(2, start) infinite;
        }
        @keyframes blink {
            to { visibility: hidden; }
        }
    </style>
</head>
<body class="h-full bg-[var(--bg)] text-[var(--text)] antialiased">
<div class="mx-auto flex h-full max-w-3xl flex-col">

    <header class="flex items-center justify-between border-b border-[var(--border)] px-4 py-3">
        <div class="flex items-center gap-3">
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--accent-soft)] text-[var(--accent-text)]">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M11.42 15.17 17.25 21A2.652 2.652 0 0 0 21 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 1 1-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 0 0 4.486-6.336l-3.276 3.277a3.004 3.004 0 0 1-2.25-2.25l3.276-3.276a4.5 4.5 0 0 0-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085"/>
                </svg>
            </div>
            <h1 class="text-sm font-semibold">Tool Stream</h1>
        </div>
        <div class="flex items-center gap-2">
            <span class="rounded-full border border-[var(--border-strong)] bg-[var(--surface)] px-2.5 py-0.5 font-mono text-xs text-[var(--text-muted)]">${modelName}</span>
            <span class="flex items-center gap-1.5 rounded-full border border-[var(--accent-border)] bg-[var(--accent-soft)] px-2.5 py-0.5 text-xs text-[var(--accent-text)]">
                <span class="relative flex h-2 w-2">
                    <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-[var(--accent-text)] opacity-75"></span>
                    <span class="relative inline-flex h-2 w-2 rounded-full bg-[var(--accent-hover)]"></span>
                </span>
                live
            </span>
        </div>
    </header>

    <main id="messages" class="flex-1 space-y-4 overflow-y-auto px-4 py-6">
        <div id="empty-state" class="flex flex-col items-center gap-4 pt-10">
            <p class="text-sm text-[var(--text-faint)]">Ask a travel question. Watch the tools fire, live, inside the response.</p>
            <div class="flex flex-wrap justify-center gap-2">
                <button data-prompt="I'm traveling to Chicago next week, what should I pack?"
                        class="rounded-full border border-[var(--border-strong)] bg-[var(--surface)] px-3.5 py-1.5 text-xs text-[var(--text-soft)] transition hover:border-[var(--accent)] hover:text-[var(--accent-text)]">
                    🧳 What should I pack for Chicago?
                </button>
                <button data-prompt="What's happening in Chicago next week?"
                        class="rounded-full border border-[var(--border-strong)] bg-[var(--surface)] px-3.5 py-1.5 text-xs text-[var(--text-soft)] transition hover:border-[var(--accent)] hover:text-[var(--accent-text)]">
                    🎟️ What's happening in Chicago?
                </button>
                <button data-prompt="What's the weather looking like in Chicago next week?"
                        class="rounded-full border border-[var(--border-strong)] bg-[var(--surface)] px-3.5 py-1.5 text-xs text-[var(--text-soft)] transition hover:border-[var(--accent)] hover:text-[var(--accent-text)]">
                    ⛅ Chicago weather next week
                </button>
            </div>
        </div>
    </main>

    <footer class="border-t border-[var(--border)] px-4 py-3">
        <form class="chat-form flex items-end gap-2">
            <textarea rows="1" autofocus
                      placeholder="I'm traveling to Chicago next week, what should I pack?"
                      class="chat-input max-h-40 flex-1 resize-none rounded-xl border border-[var(--border-strong)] bg-[var(--surface)] px-4 py-2.5 text-sm placeholder-[var(--text-faint)] focus:border-[var(--accent-hover)] focus:outline-none"></textarea>
            <button type="submit"
                    class="send-btn flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--accent)] text-white transition hover:bg-[var(--accent-hover)] disabled:cursor-not-allowed disabled:opacity-40">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 12 3.269 3.125A59.769 59.769 0 0 1 21.485 12 59.768 59.768 0 0 1 3.27 20.875L5.999 12Zm0 0h7.5"/>
                </svg>
            </button>
        </form>
    </footer>

</div>

<script src="/js/chat.js"></script>
</body>
</html>