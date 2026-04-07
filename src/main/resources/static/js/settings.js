(function () {
    const form = document.getElementById("stt-settings-form");
    const feedback = document.getElementById("settings-feedback");
    const vendorField = document.getElementById("vendorName");
    const vendorPalette = document.getElementById("vendor-palette");

    if (!form) {
        return;
    }

    function selectVendor(vendorName) {
        if (!vendorField || !vendorPalette) {
            return;
        }
        vendorField.value = vendorName;
        const cards = vendorPalette.querySelectorAll(".vendor-card");
        cards.forEach(function (card) {
            const selected = card.dataset.vendor === vendorName;
            card.classList.toggle("is-selected", selected);
            card.setAttribute("aria-pressed", selected ? "true" : "false");
        });
    }

    if (vendorPalette) {
        vendorPalette.querySelectorAll(".vendor-card").forEach(function (card) {
            card.addEventListener("click", function () {
                selectVendor(card.dataset.vendor);
            });
        });
    }

    if (vendorField && vendorField.value) {
        selectVendor(vendorField.value);
    } else {
        selectVendor("GOOGLE");
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        feedback.classList.remove("is-error", "is-success");
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
            feedback.classList.add("is-success");
        } catch (error) {
            feedback.textContent = error.message || "설정 저장 실패";
            feedback.classList.add("is-error");
        }
    });
})();
