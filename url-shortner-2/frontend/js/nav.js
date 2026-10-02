const API_URL = "http://127.0.0.1:5000";

async function loadNavbar() {

    const navbar = document.getElementById("navbar");

    if (!navbar) {
        return;
    }

    try {

        const response = await fetch(
            `${API_URL}/api/me`,
            {
                method: "GET",
                credentials: "include"
            }
        );

        const data = await response.json();


        // ==========================================
        // NOT LOGGED IN
        // ==========================================

        if (!data.logged_in) {

            navbar.innerHTML = `
                <nav class="navbar">

                    <div class="nav-brand">
                        <a href="index.html">
                            URL Shortener
                        </a>
                    </div>

                    <div class="nav-links">

                        <a href="index.html">
                            Home
                        </a>

                        <a href="login.html">
                            Login
                        </a>

                        <a href="register.html">
                            Register
                        </a>

                    </div>

                </nav>
            `;

            return;
        }


        // ==========================================
        // LOGGED IN
        // ==========================================

        let adminLink = "";

        if (data.user.role === "admin") {

            adminLink = `
                <a href="admin.html">
                    Admin Dashboard
                </a>
            `;

        }


        navbar.innerHTML = `

            <nav class="navbar">

                <div class="nav-brand">

                    <a href="index.html">
                        URL Shortener
                    </a>

                </div>


                <div class="nav-links">

                    <a href="index.html">
                        Home
                    </a>

                    <a href="myurls.html">
                        My URLs
                    </a>

                    ${adminLink}


                    <span class="user-name">
                        👤 ${data.user.name}
                    </span>


                    <button
                        id="logoutBtn"
                        class="logout-button"
                    >
                        Logout
                    </button>

                </div>

            </nav>

        `;


        // ==========================================
        // LOGOUT
        // ==========================================

        const logoutBtn =
            document.getElementById("logoutBtn");


        logoutBtn.addEventListener(
            "click",
            async () => {

                try {

                    const response =
                        await fetch(
                            `${API_URL}/api/logout`,
                            {
                                method: "POST",
                                credentials: "include"
                            }
                        );


                    const result =
                        await response.json();


                    if (result.success) {

                        alert("Logged out successfully!");

                        window.location.href =
                            "index.html";

                    }

                } catch (error) {

                    console.error(
                        "Logout error:",
                        error
                    );

                    alert(
                        "Could not logout."
                    );

                }

            }
        );


    } catch (error) {

        console.error(
            "Navbar error:",
            error
        );

    }

}


document.addEventListener(
    "DOMContentLoaded",
    loadNavbar
);