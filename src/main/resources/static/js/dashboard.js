(function () {
    const callList = document.getElementById("active-call-list");
    const callCount = document.getElementById("call-count");
    const activeCountValue = document.getElementById("active-count-value");
    const transcriptContainer = document.getElementById("transcript-container");
    const socketChip = document.getElementById("socket-chip");
    const socketStatusText = document.getElementById("socket-status-text");

    function getCallEmptyState() {
        return document.getElementById("call-empty-state");
    }

    function getTranscriptEmptyState() {
        return document.getElementById("transcript-empty-state");
    }

    function setCount(value) {
        callCount.textContent = String(value);
        activeCountValue.textContent = String(value);
    }

    function updateCountFromDom() {
        setCount(callList.querySelectorAll(".call-row").length);
        const callEmptyState = getCallEmptyState();
        if (callList.querySelectorAll(".call-row").length === 0) {
            if (!callEmptyState) {
                const empty = document.createElement("div");
                empty.id = "call-empty-state";
                empty.className = "empty-state";
                empty.textContent = "실시간 세션이 들어오면 여기에 즉시 표시됩니다.";
                callList.appendChild(empty);
            }
        } else if (callEmptyState) {
            callEmptyState.remove();
        }
    }

    function createCallRow(call) {
        const row = document.createElement("article");
        row.className = "call-row is-entering";
        row.dataset.callId = call.callId;

        const meta = document.createElement("div");
        meta.className = "call-meta";

        const id = document.createElement("p");
        id.className = "call-id";
        id.textContent = call.callId;

        const subline = document.createElement("p");
        subline.className = "call-subline";
        subline.textContent = `${call.callerNumberMasked || "-"} • ${call.agentExtension || "-"}`;

        const state = document.createElement("div");
        state.className = "call-state";
        const pill = document.createElement("span");
        pill.className = "state-pill";
        pill.textContent = call.state || "CONNECTED";

        meta.appendChild(id);
        meta.appendChild(subline);
        state.appendChild(pill);
        row.appendChild(meta);
        row.appendChild(state);
        return row;
    }

    function upsertCall(call) {
        const callEmptyState = getCallEmptyState();
        const existing = callList.querySelector(`[data-call-id="${call.callId}"]`);
        if (existing) {
            existing.querySelector(".call-subline").textContent =
                `${call.callerNumberMasked || "-"} • ${call.agentExtension || "-"}`;
            existing.querySelector(".state-pill").textContent = call.state || "CONNECTED";
            return;
        }

        const row = createCallRow(call);
        if (callEmptyState) {
            callEmptyState.remove();
        }
        callList.prepend(row);
        window.setTimeout(function () {
            row.classList.remove("is-entering");
        }, 360);
        updateCountFromDom();
    }

    function removeCall(callId) {
        const existing = callList.querySelector(`[data-call-id="${callId}"]`);
        if (existing) {
            existing.remove();
        }
        updateCountFromDom();
    }

    function getTranscriptCard(callId) {
        let card = transcriptContainer.querySelector(`[data-call-id="${callId}"]`);
        if (card) {
            return card;
        }

        const transcriptEmptyState = getTranscriptEmptyState();
        if (transcriptEmptyState) {
            transcriptEmptyState.remove();
        }

        card = document.createElement("article");
        card.className = "transcript-card";
        card.dataset.callId = callId;

        const title = document.createElement("h3");
        title.textContent = `통화 ${callId}`;

        const meta = document.createElement("div");
        meta.className = "transcript-meta";
        meta.innerHTML = "<span>실시간 피드</span>";

        const body = document.createElement("div");
        body.className = "transcript-body";

        card.appendChild(title);
        card.appendChild(meta);
        card.appendChild(body);
        transcriptContainer.prepend(card);
        return card;
    }

    function appendTranscript(message) {
        const card = getTranscriptCard(message.callId);
        const body = card.querySelector(".transcript-body");

        if (message.isFinal) {
            const line = document.createElement("p");
            line.className = "transcript-line";
            line.textContent = message.text || "";
            body.appendChild(line);
        } else {
            let interim = body.querySelector(".transcript-line.interim");
            if (!interim) {
                interim = document.createElement("p");
                interim.className = "transcript-line interim";
                body.appendChild(interim);
            }
            interim.textContent = message.text || "";
        }
    }

    window.addEventListener("sttgw:ws-status", function (event) {
        const status = event.detail.status;
        socketChip.dataset.status = status;
        if (status === "connected") {
            socketStatusText.textContent = "실시간 연결됨";
        } else if (status === "connecting") {
            socketStatusText.textContent = "연결 준비중";
        } else {
            socketStatusText.textContent = "재연결 대기";
        }
    });

    window.addEventListener("sttgw:ws-message", function (event) {
        const message = event.detail;
        if (message.type === "SESSION_CREATED") {
            upsertCall(message.call);
            return;
        }
        if (message.type === "SESSION_COMPLETED") {
            removeCall(message.call.callId);
            return;
        }
        if (message.type === "STT_RESULT") {
            appendTranscript(message);
        }
    });

    updateCountFromDom();
})();
