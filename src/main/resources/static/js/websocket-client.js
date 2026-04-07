(function () {
    let socket = null;
    let reconnectTimer = null;

    function emit(name, detail) {
        window.dispatchEvent(new CustomEvent(name, {detail}));
    }

    function updateStatus(status) {
        emit("sttgw:ws-status", {status});
    }

    function connect() {
        const protocol = window.location.protocol === "https:" ? "wss" : "ws";
        socket = new WebSocket(`${protocol}://${window.location.host}/ws/stt`);
        updateStatus("connecting");

        socket.addEventListener("open", function () {
            updateStatus("connected");
        });

        socket.addEventListener("message", function (event) {
            try {
                emit("sttgw:ws-message", JSON.parse(event.data));
            } catch (error) {
                console.error("WebSocket 메시지 파싱 실패", error);
            }
        });

        socket.addEventListener("close", function () {
            updateStatus("disconnected");
            if (reconnectTimer) {
                clearTimeout(reconnectTimer);
            }
            reconnectTimer = window.setTimeout(connect, 3000);
        });

        socket.addEventListener("error", function () {
            updateStatus("disconnected");
        });
    }

    window.sttgwWebSocket = {
        connect
    };

    connect();
})();
