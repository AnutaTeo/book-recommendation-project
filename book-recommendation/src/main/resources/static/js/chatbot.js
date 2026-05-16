document.addEventListener("DOMContentLoaded", () => {
    const toggleButton = document.createElement("button");
    toggleButton.className = "chatbot-button";
    toggleButton.innerText = "💬";

    const chatWindow = document.createElement("div");
    chatWindow.className = "chatbot-window";

    chatWindow.innerHTML = `
        <div class="chatbot-header">Book Assistant</div>

        <div class="chatbot-messages" id="chatbotMessages">
            <div class="chatbot-message bot-message">
                Hi! I can help you find books using the RDF data.
            </div>
        </div>

        <div class="chatbot-starters" id="chatbotStarters"></div>

        <div class="chatbot-input-area">
            <input id="chatbotInput" type="text" placeholder="Ask about books...">
            <button id="chatbotSend">Send</button>
        </div>
    `;

    document.body.appendChild(toggleButton);
    document.body.appendChild(chatWindow);

    toggleButton.addEventListener("click", () => {
        chatWindow.style.display =
            chatWindow.style.display === "flex" ? "none" : "flex";
    });

    const messages = document.getElementById("chatbotMessages");
    const input = document.getElementById("chatbotInput");
    const sendButton = document.getElementById("chatbotSend");
    const startersDiv = document.getElementById("chatbotStarters");

    async function loadStarters() {
        startersDiv.innerHTML = "";

        try {
            const response = await fetch(`/api/chatbot/starters?pageUrl=${encodeURIComponent(window.location.pathname)}`);
            const data = await response.json();

            if (!data.starters || data.starters.length === 0) {
                throw new Error("No starters returned");
            }

            data.starters.forEach(addStarterButton);
        } catch (error) {
            [
                "What books are available?",
                "Find a book by author and theme.",
                "Recommend a book for Alice."
            ].forEach(addStarterButton);
        }
    }

    function addStarterButton(text) {
        const button = document.createElement("button");
        button.innerText = text;
        button.onclick = () => {
            input.value = text;
            sendMessage();
        };
        startersDiv.appendChild(button);
    }

    function addMessage(text, sender) {
        const div = document.createElement("div");
        div.className = `chatbot-message ${sender}-message`;
        div.innerText = text;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }

    async function sendMessage() {
        const text = input.value.trim();
        if (!text) return;

        addMessage(text, "user");
        input.value = "";

        addMessage("Thinking...", "bot");

        try {
            const response = await fetch("/api/chatbot/ask", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    message: text,
                    pageUrl: window.location.pathname
                })
            });

            const data = await response.json();
            messages.lastChild.innerText = data.answer;
        } catch (error) {
            messages.lastChild.innerText = "Chatbot backend error.";
        }
    }

    sendButton.addEventListener("click", sendMessage);

    input.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            sendMessage();
        }
    });

    loadStarters();
});