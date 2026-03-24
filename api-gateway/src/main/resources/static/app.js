const state = {
    token: "",
    identity: null,
    productsPage: 0,
    productsSize: 10,
    ordersPage: 0,
    ordersSize: 10,
    currentView: "overview",
    activeDiagramStep: "client",
    latestProducts: [],
    latestOrders: []
};

const diagramContent = {
    client: {
        title: "Client Layer",
        description: "Users interact with the application through the browser UI or Postman. Every request starts here and enters the system through the single public gateway.",
        tags: ["HTML", "CSS", "JavaScript", "Postman"]
    },
    gateway: {
        title: "Gateway",
        description: "Spring Cloud Gateway is the single entry point. It serves the UI, forwards API traffic, and propagates correlation information through the system.",
        tags: ["Spring Boot", "Spring Cloud Gateway", "WebFlux", "Correlation ID Filter"]
    },
    platform: {
        title: "Platform Control",
        description: "Config Server centralizes non-secret configuration and Eureka provides service discovery so the gateway and services can locate each other dynamically.",
        tags: ["Spring Cloud Config", "Native Config Repo", "Eureka Server", "Spring Cloud Netflix"]
    },
    services: {
        title: "Business Services",
        description: "Auth, Product, Order, and Inventory services each own a business responsibility. JWT-based authorization and Feign-based service integration support the overall flow.",
        tags: ["Spring Boot", "Spring Security", "OpenFeign", "JPA", "JWT RSA"]
    },
    messaging: {
        title: "Async Messaging",
        description: "Orders use RabbitMQ and an outbox-driven flow to request inventory processing asynchronously, which keeps stock updates safer and decouples service timing.",
        tags: ["RabbitMQ", "Spring AMQP", "Outbox Pattern", "Idempotent Consumer"]
    },
    data: {
        title: "Persistence",
        description: "Each service owns its own MySQL schema. Flyway manages schema evolution and Spring Data JPA handles the persistence layer in each service.",
        tags: ["MySQL", "Flyway", "Spring Data JPA", "Service-Owned Databases"]
    }
};

const elements = {
    sessionState: document.getElementById("sessionState"),
    sessionMeta: document.getElementById("sessionMeta"),
    metricRole: document.getElementById("metricRole"),
    metricEmail: document.getElementById("metricEmail"),
    metricProducts: document.getElementById("metricProducts"),
    metricOrders: document.getElementById("metricOrders"),
    pageTitle: document.getElementById("pageTitle"),
    productsList: document.getElementById("productsList"),
    ordersList: document.getElementById("ordersList"),
    overviewProducts: document.getElementById("overviewProducts"),
    overviewOrders: document.getElementById("overviewOrders"),
    productsState: document.getElementById("productsState"),
    ordersState: document.getElementById("ordersState"),
    productsPageLabel: document.getElementById("productsPageLabel"),
    ordersPageLabel: document.getElementById("ordersPageLabel"),
    productsPrevBtn: document.getElementById("productsPrevBtn"),
    productsNextBtn: document.getElementById("productsNextBtn"),
    ordersPrevBtn: document.getElementById("ordersPrevBtn"),
    ordersNextBtn: document.getElementById("ordersNextBtn"),
    diagramNodes: Array.from(document.querySelectorAll(".diagram-node")),
    diagramDetailTitle: document.getElementById("diagramDetailTitle"),
    diagramDetailDescription: document.getElementById("diagramDetailDescription"),
    diagramTags: document.getElementById("diagramTags"),
    views: {
        overview: document.getElementById("view-overview"),
        architecture: document.getElementById("view-architecture"),
        products: document.getElementById("view-products"),
        orders: document.getElementById("view-orders"),
        admin: document.getElementById("view-admin"),
        account: document.getElementById("view-account")
    },
    navLinks: Array.from(document.querySelectorAll(".nav-link")),
    jumpLinks: Array.from(document.querySelectorAll(".jump-link")),
    adminOnlyLinks: Array.from(document.querySelectorAll(".admin-only"))
};

const productCardTemplate = document.getElementById("productCardTemplate");
const orderCardTemplate = document.getElementById("orderCardTemplate");

document.getElementById("loginForm").addEventListener("submit", handleLogin);
document.getElementById("registerForm").addEventListener("submit", handleRegister);
document.getElementById("productForm").addEventListener("submit", handleCreateProduct);
document.getElementById("orderForm").addEventListener("submit", handleCreateOrder);
document.getElementById("logoutBtn").addEventListener("click", logout);
document.getElementById("accountLogoutBtn").addEventListener("click", logout);
document.getElementById("refreshDashboardBtn").addEventListener("click", refreshDashboard);

elements.productsPrevBtn.addEventListener("click", () => changeProductsPage(-1));
elements.productsNextBtn.addEventListener("click", () => changeProductsPage(1));
elements.ordersPrevBtn.addEventListener("click", () => changeOrdersPage(-1));
elements.ordersNextBtn.addEventListener("click", () => changeOrdersPage(1));

elements.navLinks.forEach((link) => {
    link.addEventListener("click", () => openView(link.dataset.view));
});

elements.jumpLinks.forEach((link) => {
    link.addEventListener("click", () => openView(link.dataset.view));
});

elements.diagramNodes.forEach((node) => {
    node.addEventListener("click", () => selectDiagramStep(node.dataset.diagramStep));
});

bootstrap().catch(handleError);

async function bootstrap() {
    syncSessionUI();
    renderOverview();
    renderDiagram();
    await restoreSession();
    openView(state.identity ? "overview" : "account");
    if (state.identity) {
        refreshDashboard();
    }
}

async function handleLogin(event) {
    event.preventDefault();
    const payload = {
        email: document.getElementById("loginEmail").value.trim(),
        password: document.getElementById("loginPassword").value
    };

    const response = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify(payload)
    });

    storeToken(response.token);
    openView("overview");
    await refreshDashboard();
}

async function handleRegister(event) {
    event.preventDefault();
    const payload = {
        fullName: document.getElementById("registerFullName").value.trim(),
        email: document.getElementById("registerEmail").value.trim(),
        password: document.getElementById("registerPassword").value
    };

    const response = await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify(payload)
    });

    alert(response.message || "Registration successful. Please login.");
}

async function handleCreateProduct(event) {
    event.preventDefault();
    if (!isAdmin()) {
        alert("Admin access is required to create a product.");
        return;
    }

    const payload = {
        name: document.getElementById("productName").value.trim(),
        price: Number(document.getElementById("productPrice").value),
        stock: Number(document.getElementById("productStock").value)
    };

    const response = await apiRequest("/products", {
        method: "POST",
        body: JSON.stringify(payload)
    });

    event.target.reset();
    state.productsPage = 0;
    await loadProducts();
    openView("products");
}

async function handleCreateOrder(event) {
    event.preventDefault();
    ensureAuthenticated();

    const payload = {
        productId: Number(document.getElementById("orderProductId").value),
        quantity: Number(document.getElementById("orderQuantity").value)
    };

    const response = await apiRequest("/orders", {
        method: "POST",
        body: JSON.stringify(payload)
    });

    state.ordersPage = 0;
    await Promise.all([loadOrders(), loadProducts()]);
}

function logout() {
    apiRequest("/auth/logout", { method: "POST" }).catch(() => null);
    state.token = "";
    state.identity = null;
    state.productsPage = 0;
    state.ordersPage = 0;
    state.latestProducts = [];
    state.latestOrders = [];
    syncSessionUI();
    clearDashboard();
    openView("account");
}

async function refreshDashboard() {
    if (!state.identity) {
        syncSessionUI();
        renderOverview();
        return;
    }

    try {
        await Promise.all([loadProducts(), loadOrders()]);
        syncSessionUI();
    } catch (error) {
        handleError(error);
    }
}

async function loadProducts() {
    ensureAuthenticated();
    const page = await apiRequest(`/products?page=${state.productsPage}&size=${state.productsSize}`);
    state.latestProducts = page.content;
    renderProducts(page);
    renderOverview();
    elements.metricProducts.textContent = String(page.totalElements);
}

async function loadOrders() {
    ensureAuthenticated();
    const page = await apiRequest(`/orders?page=${state.ordersPage}&size=${state.ordersSize}`);
    state.latestOrders = page.content;
    renderOrders(page);
    renderOverview();
    elements.metricOrders.textContent = String(page.totalElements);
}

function renderProducts(page) {
    elements.productsList.innerHTML = "";
    elements.productsPageLabel.textContent = `Page ${page.number + 1}`;
    elements.productsPrevBtn.disabled = page.first;
    elements.productsNextBtn.disabled = page.last;

    if (!page.content.length) {
        elements.productsState.textContent = "No products available yet.";
        return;
    }

    elements.productsState.textContent = `${page.totalElements} product(s) loaded.`;

    page.content.forEach((product) => {
        const node = productCardTemplate.content.firstElementChild.cloneNode(true);
        node.querySelector(".record-id").textContent = `Product #${product.id}`;
        node.querySelector(".record-title").textContent = product.name;
        node.querySelector(".record-price").textContent = `Price: Rs ${formatMoney(product.price)}`;
        node.querySelector(".chip-stock").textContent = `Stock ${product.stock}`;
        node.addEventListener("click", () => {
            document.getElementById("orderProductId").value = product.id;
            openView("orders");
        });
        elements.productsList.appendChild(node);
    });
}

function renderOrders(page) {
    elements.ordersList.innerHTML = "";
    elements.ordersPageLabel.textContent = `Page ${page.number + 1}`;
    elements.ordersPrevBtn.disabled = page.first;
    elements.ordersNextBtn.disabled = page.last;

    if (!page.content.length) {
        elements.ordersState.textContent = "No orders found for this account.";
        return;
    }

    elements.ordersState.textContent = `${page.totalElements} order(s) loaded.`;

    page.content.forEach((order) => {
        const node = orderCardTemplate.content.firstElementChild.cloneNode(true);
        node.querySelector(".record-id").textContent = `Order #${order.id}`;
        node.querySelector(".record-title").textContent = `Product ${order.productId} | Qty ${order.quantity}`;
        node.querySelector(".record-meta").textContent = `Placed by ${order.userEmail} on ${formatDate(order.createdAt)}`;
        node.querySelector(".record-price").textContent = `Total: Rs ${formatMoney(order.totalAmount)}`;
        node.querySelector(".chip-status").textContent = order.status;
        elements.ordersList.appendChild(node);
    });
}

function renderOverview() {
    elements.overviewProducts.innerHTML = "";
    elements.overviewOrders.innerHTML = "";

    if (!state.identity) {
        elements.overviewProducts.innerHTML = `<div class="mini-item">Sign in to load catalog details.</div>`;
        elements.overviewOrders.innerHTML = `<div class="mini-item">Sign in to view order activity.</div>`;
        return;
    }

    if (!state.latestProducts.length) {
        elements.overviewProducts.innerHTML = `<div class="mini-item">No products available yet.</div>`;
    } else {
        state.latestProducts.slice(0, 3).forEach((product) => {
            const item = document.createElement("div");
            item.className = "mini-item";
            item.innerHTML = `<strong>${escapeHtml(product.name)}</strong><br><span>Product #${product.id} | Stock ${product.stock}</span>`;
            elements.overviewProducts.appendChild(item);
        });
    }

    if (!state.latestOrders.length) {
        elements.overviewOrders.innerHTML = `<div class="mini-item">No orders recorded for this account.</div>`;
    } else {
        state.latestOrders.slice(0, 3).forEach((order) => {
            const item = document.createElement("div");
            item.className = "mini-item";
            item.innerHTML = `<strong>Order #${order.id}</strong><br><span>Status ${escapeHtml(order.status)} | Product ${order.productId}</span>`;
            elements.overviewOrders.appendChild(item);
        });
    }
}

function clearDashboard() {
    elements.productsList.innerHTML = "";
    elements.ordersList.innerHTML = "";
    elements.productsState.textContent = "Sign in to load products.";
    elements.ordersState.textContent = "Sign in to load orders.";
    elements.metricProducts.textContent = "0";
    elements.metricOrders.textContent = "0";
    renderOverview();
}

function syncSessionUI() {
    const identity = state.identity;
    const signedIn = Boolean(identity);

    elements.sessionState.textContent = signedIn ? "Signed in" : "Signed out";
    elements.sessionMeta.textContent = signedIn
        ? `${identity.email} | role ${identity.role}`
        : "Sign in to access products, orders, and admin tools.";
    elements.metricRole.textContent = identity?.role || "Guest";
    elements.metricEmail.textContent = identity?.email || "Not logged in";
    elements.adminOnlyLinks.forEach((link) => {
        link.classList.toggle("hidden", !isAdmin());
    });

    if (!isAdmin() && state.currentView === "admin") {
        openView(signedIn ? "overview" : "account");
    }
}

function openView(viewName) {
    const targetView = viewName === "admin" && !isAdmin() ? (state.identity ? "overview" : "account") : viewName;
    state.currentView = targetView;

    Object.entries(elements.views).forEach(([name, node]) => {
        node.classList.toggle("hidden", name !== targetView);
    });

    elements.navLinks.forEach((link) => {
        link.classList.toggle("active", link.dataset.view === targetView);
    });

    elements.pageTitle.textContent = formatViewTitle(targetView);
}

function changeProductsPage(delta) {
    const nextPage = state.productsPage + delta;
    if (nextPage < 0) {
        return;
    }
    state.productsPage = nextPage;
    loadProducts().catch(handleError);
}

function changeOrdersPage(delta) {
    const nextPage = state.ordersPage + delta;
    if (nextPage < 0) {
        return;
    }
    state.ordersPage = nextPage;
    loadOrders().catch(handleError);
}

function storeToken(token) {
    state.token = token;
    state.identity = decodeToken(token);
    syncSessionUI();
}

function isAdmin() {
    return state.identity?.role === "ADMIN";
}

function ensureAuthenticated() {
    if (!state.identity) {
        throw new Error("Please login first.");
    }
}

async function restoreSession() {
    try {
        const response = await apiRequest("/auth/validate");
        if (response?.valid) {
            state.identity = {
                email: response.email,
                role: response.role
            };
            syncSessionUI();
        }
    } catch (error) {
        state.token = "";
        state.identity = null;
        syncSessionUI();
    }
}

function decodeToken(token) {
    if (!token) {
        return null;
    }

    try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        return {
            email: payload.sub,
            role: payload.role
        };
    } catch (error) {
        return null;
    }
}

async function apiRequest(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, {
        ...options,
        headers,
        credentials: "same-origin"
    });

    let payload = null;
    const text = await response.text();
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch (error) {
            payload = { message: text };
        }
    }

    if (!response.ok) {
        const error = new Error(payload?.message || `Request failed with status ${response.status}`);
        error.payload = payload;
        throw error;
    }

    return payload;
}

function handleError(error) {
    const message = error?.payload?.message || error.message || "Unexpected error";
    alert(message);
}

function formatMoney(value) {
    return Number(value).toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatDate(value) {
    return new Date(value).toLocaleString();
}

function formatViewTitle(viewName) {
    switch (viewName) {
        case "architecture":
            return "Architecture";
        case "products":
            return "Products";
        case "orders":
            return "Orders";
        case "admin":
            return "Admin";
        case "account":
            return "Account";
        default:
            return "Overview";
    }
}

function selectDiagramStep(step) {
    if (!diagramContent[step]) {
        return;
    }
    state.activeDiagramStep = step;
    renderDiagram();
}

function renderDiagram() {
    const content = diagramContent[state.activeDiagramStep];
    if (!content) {
        return;
    }

    elements.diagramNodes.forEach((node) => {
        node.classList.toggle("active", node.dataset.diagramStep === state.activeDiagramStep);
    });

    elements.diagramDetailTitle.textContent = content.title;
    elements.diagramDetailDescription.textContent = content.description;
    elements.diagramTags.innerHTML = "";

    content.tags.forEach((tag) => {
        const tagNode = document.createElement("span");
        tagNode.className = "diagram-tag";
        tagNode.textContent = tag;
        elements.diagramTags.appendChild(tagNode);
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
