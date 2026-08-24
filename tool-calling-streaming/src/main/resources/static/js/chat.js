// Vanilla SSE-over-POST client.
// Native EventSource only supports GET, so we POST with fetch() and parse the
// text/event-stream body off a ReadableStream ourselves — same approach every
// production chat frontend uses.

const messages = document.getElementById('messages');

// One conversation per page load — the server keys chat memory off this id.
const conversationId = crypto.randomUUID();

// Elements are resolved by class, and by visibility when the UI picker is
// comparing variants (hidden variants keep their markup in the DOM).
const isVisible = (el) => el && el.offsetParent !== null;
const getInput = () =>
    [...document.querySelectorAll('.chat-input')].find(isVisible) || document.querySelector('.chat-input');
const getSendBtn = () =>
    [...document.querySelectorAll('.send-btn')].find(isVisible) || document.querySelector('.send-btn');

getInput().focus();

document.addEventListener('submit', (e) => {
    if (!e.target.matches('.chat-form')) return;
    e.preventDefault();
    sendMessage();
});

document.addEventListener('keydown', (e) => {
    if (!e.target.matches('.chat-input')) return;
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
    // Tab completes the placeholder suggestion when the box is empty.
    if (e.key === 'Tab' && e.target.value.trim() === '') {
        e.preventDefault();
        e.target.value = e.target.placeholder;
    }
});

// Suggestion chips (empty-state variant): click fills the input.
document.addEventListener('click', (e) => {
    const chip = e.target.closest('[data-prompt]');
    if (!chip) return;
    const input = getInput();
    input.value = chip.dataset.prompt;
    input.focus();
});

async function sendMessage() {
    const input = getInput();
    const message = input.value.trim() || input.placeholder;
    if (!message || getSendBtn().disabled) return;

    input.value = '';
    setBusy(true);
    document.getElementById('empty-state')?.remove();

    appendUserBubble(message);
    const turn = createAssistantTurn();

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message, conversationId })
        });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        await readEventStream(response.body, (event) => handleEvent(turn, event));
    } catch (err) {
        turn.showError(err.message);
    } finally {
        turn.finish();
        setBusy(false);
    }
}

// --- SSE parsing -----------------------------------------------------------

async function readEventStream(body, onEvent) {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });

        // Events are separated by a blank line.
        let sep;
        while ((sep = buffer.indexOf('\n\n')) !== -1) {
            const raw = buffer.slice(0, sep);
            buffer = buffer.slice(sep + 2);
            const event = parseEvent(raw);
            if (event) onEvent(event);
        }
    }
}

function parseEvent(raw) {
    let type = 'message';
    const data = [];
    for (const line of raw.split('\n')) {
        if (line.startsWith('event:')) type = line.slice(6).trim();
        else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''));
    }
    if (data.length === 0 && type === 'message') return null;
    return { type, data: data.join('\n') };
}

// --- Event dispatch --------------------------------------------------------

function handleEvent(turn, event) {
    switch (event.type) {
        case 'token':
            // JSON-encoded on the server: raw SSE data loses leading spaces,
            // because the spec strips one space after "data:".
            turn.appendText(JSON.parse(event.data));
            break;
        case 'tool': {
            const toolEvent = JSON.parse(event.data);
            if (toolEvent.phase === 'STARTED') turn.startPill(toolEvent.id, toolEvent.name);
            else turn.completePill(toolEvent.id);
            break;
        }
        case 'done':
            turn.finish();
            break;
        case 'error':
            turn.showError(event.data);
            break;
    }
}

// --- DOM -------------------------------------------------------------------

function appendUserBubble(text) {
    const row = document.createElement('div');
    row.className = 'flex justify-end';
    const bubble = document.createElement('div');
    bubble.className = 'max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-br-sm bg-[var(--accent)] px-4 py-2.5 text-sm text-white';
    bubble.textContent = text;
    row.appendChild(bubble);
    messages.appendChild(row);
    scrollToBottom();
}

// An assistant turn holds interleaved children — text segments and tool
// pills, in arrival order — so tools appear inline, not in a separate log.
function createAssistantTurn() {
    const row = document.createElement('div');
    row.className = 'flex justify-start';
    const container = document.createElement('div');
    container.className = 'max-w-[85%] space-y-2 rounded-2xl rounded-bl-sm bg-[var(--surface)] px-4 py-3 text-sm';
    row.appendChild(container);
    messages.appendChild(row);
    scrollToBottom();

    const pills = new Map();
    let textEl = null;

    function currentTextEl() {
        // Start a new text segment if the last child is a pill (or nothing yet).
        if (!textEl || textEl !== container.lastElementChild) {
            if (textEl) textEl.classList.remove('cursor');
            textEl = document.createElement('div');
            textEl.className = 'whitespace-pre-wrap leading-relaxed cursor';
            container.appendChild(textEl);
        }
        return textEl;
    }

    return {
        appendText(text) {
            currentTextEl().textContent += text;
            scrollToBottom();
        },
        startPill(id, name) {
            if (pills.has(id)) return;
            const pill = document.createElement('div');
            pill.className = 'inline-flex items-center gap-1.5 rounded-full border border-[var(--border-strong)] bg-[var(--surface-2)] px-2.5 py-1 text-xs text-[var(--text-soft)]';
            pill.innerHTML =
                '<svg class="h-3 w-3 animate-spin text-[var(--accent-text)]" viewBox="0 0 24 24" fill="none">' +
                '<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>' +
                '<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z"></path></svg>' +
                '<span></span>';
            pill.querySelector('span').textContent = 'Calling ' + name + '...';
            const wrap = document.createElement('div');
            wrap.appendChild(pill);
            container.appendChild(wrap);
            pills.set(id, { pill, name });
            scrollToBottom();
        },
        completePill(id) {
            const entry = pills.get(id);
            if (!entry || entry.done) return;
            entry.done = true;
            entry.pill.className = 'inline-flex items-center gap-1.5 rounded-full border border-[var(--accent-border)] bg-[var(--accent-soft)] px-2.5 py-1 text-xs text-[var(--accent-text)]';
            entry.pill.innerHTML =
                '<svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">' +
                '<path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5"/></svg>' +
                '<span></span>';
            entry.pill.querySelector('span').textContent = entry.name;
        },
        showError(text) {
            const el = document.createElement('div');
            el.className = 'text-xs text-red-600';
            el.textContent = 'Error: ' + text;
            container.appendChild(el);
            scrollToBottom();
        },
        finish() {
            if (textEl) textEl.classList.remove('cursor');
            // Safety net: resolve any pill that never got a COMPLETED event.
            for (const id of pills.keys()) this.completePill(id);
        }
    };
}

function setBusy(busy) {
    document.querySelectorAll('.send-btn').forEach((el) => el.disabled = busy);
    document.querySelectorAll('.chat-input').forEach((el) => el.disabled = busy);
    if (!busy) getInput().focus();
}

function scrollToBottom() {
    messages.scrollTop = messages.scrollHeight;
}