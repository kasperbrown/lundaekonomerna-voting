document.addEventListener("DOMContentLoaded", () => {
    const roundList = document.getElementById("roundList");

    if (!roundList) {
        return;
    }

    let draggedItem = null;

    roundList.querySelectorAll(".round-list-item").forEach(item => {
        item.addEventListener("dragstart", () => {
            draggedItem = item;
            item.classList.add("dragging");
        });

        item.addEventListener("dragend", () => {
            item.classList.remove("dragging");
            draggedItem = null;
            saveRoundOrder(roundList);
        });
    });

    roundList.addEventListener("dragover", event => {
        event.preventDefault();

        const afterElement = getDragAfterElement(roundList, event.clientY);

        if (afterElement == null) {
            roundList.appendChild(draggedItem);
        } else {
            roundList.insertBefore(draggedItem, afterElement);
        }
    });
});

function getDragAfterElement(container, y) {
    const draggableElements = [
        ...container.querySelectorAll(".round-list-item:not(.dragging)")
    ];

    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;

        if (offset < 0 && offset > closest.offset) {
            return {
                offset: offset,
                element: child
            };
        }

        return closest;
    }, {
        offset: Number.NEGATIVE_INFINITY
    }).element;
}

function saveRoundOrder(roundList) {
    const meetingId = roundList.getAttribute("data-meeting-id");

    const roundIds = [...roundList.querySelectorAll(".round-list-item")]
        .map(item => item.getAttribute("data-round-id"))
        .join(",");

    fetch(`/admin/meetings/${meetingId}/rounds/reorder`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `roundIds=${encodeURIComponent(roundIds)}`
    });
}