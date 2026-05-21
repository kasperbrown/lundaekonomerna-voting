const PAGE_SCROLL_KEY = "globalPageScroll";
const MATRIX_SCROLL_KEY = "matrixScroll";

function saveScrollPositions() {

    sessionStorage.setItem(PAGE_SCROLL_KEY, window.scrollY);

    const matrixWrapper = document.querySelector(".meeting-matrix-wrapper");

    if (matrixWrapper) {
        sessionStorage.setItem(MATRIX_SCROLL_KEY, matrixWrapper.scrollTop);
    }
}

window.addEventListener("load", () => {

    if (sessionStorage.getItem("restoreScroll") === "true") {

        const savedPageScroll = sessionStorage.getItem(PAGE_SCROLL_KEY);

        if (savedPageScroll !== null) {
            window.scrollTo(0, parseInt(savedPageScroll));
        }

        const matrixWrapper = document.querySelector(".meeting-matrix-wrapper");

        if (matrixWrapper) {

            const savedMatrixScroll = sessionStorage.getItem(MATRIX_SCROLL_KEY);

            if (savedMatrixScroll !== null) {
                matrixWrapper.scrollTop = parseInt(savedMatrixScroll);
            }
        }

        sessionStorage.removeItem("restoreScroll");
    } else {

        window.scrollTo(0, 0);

        const matrixWrapper = document.querySelector(".meeting-matrix-wrapper");

        if (matrixWrapper) {
            matrixWrapper.scrollTop = 0;
        }
    }
});

document.querySelectorAll("form").forEach(form => {

    form.addEventListener("submit", () => {

        sessionStorage.setItem("restoreScroll", "true");
        saveScrollPositions();
    });
});

document.querySelectorAll("a").forEach(link => {

    link.addEventListener("click", () => {

        sessionStorage.removeItem("restoreScroll");
        sessionStorage.removeItem(PAGE_SCROLL_KEY);
        sessionStorage.removeItem(MATRIX_SCROLL_KEY);
    });
});