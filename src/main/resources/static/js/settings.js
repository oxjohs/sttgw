(function () {
    const form = document.getElementById("stt-settings-form");
    const feedback = document.getElementById("settings-feedback");

    if (!form) {
        return;
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        feedback.textContent = "저장 중...";

        const payload = {
            vendorName: form.vendorName.value,
            languageCode: form.languageCode.value,
            credentialPath: form.credentialPath.value,
            extraConfig: form.extraConfig.value,
            streamingEnabled: form.streamingEnabled.checked,
            active: form.active.checked
        };

        try {
            const response = await fetch("/api/settings/stt", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            const body = await response.json();
            if (!response.ok) {
                throw new Error(body.message || "설정 저장에 실패했습니다.");
            }

            feedback.textContent = "저장 완료";
        } catch (error) {
            feedback.textContent = error.message || "설정 저장 실패";
        }
    });
})();
